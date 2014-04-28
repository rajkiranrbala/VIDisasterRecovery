package com.sjsu.vmmanager;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.sjsu.vmservices.ServiceInstanceFactory;
import com.sjsu.vmservices.VirtualMachineBackupTask;
import com.sjsu.vmservices.VirtualMachineHeartBeatMonitor;
import com.sjsu.vmservices.VirtualMachinePerformanceMonitor;
import com.sjsu.vmservices.VirtualMachineRestoreTask;
import com.vmware.vim25.AlarmSpec;
import com.vmware.vim25.AlarmState;
import com.vmware.vim25.DuplicateName;
import com.vmware.vim25.EventAlarmExpression;
import com.vmware.vim25.InvalidName;
import com.vmware.vim25.ManagedEntityStatus;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.Alarm;
import com.vmware.vim25.mo.AlarmManager;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

public final class VirtualMachineMonitor extends
		VirtualMachineServiceEventHandler {

	public static final int STATUS_POWER_OFF = 0;
	public static final int STATUS_POWER_ON = 1;
	public static final int STATUS_FAILURE = -1;

	public String alarmNamePowerOn;
	public String alarmnamePowerOff;

	private ServiceInstanceFactory factory;

	private String virtualMachineName;
	private InetAddress ipAddress;

	private String cloneName;
	private String restoreHost;

	private int status;
	private boolean monitoring;
	private boolean restored;
	private long monitorStartTime;
	private long monitorEndTime;

	ArrayList<VirtualMachineServiceEventHandler> vmServiceEventHandlers;

	public void addVirtualMachineServiceEventHandler(
			VirtualMachineServiceEventHandler handler) {
		if (!this.vmServiceEventHandlers.contains(handler)) {
			vmServiceEventHandlers.add(handler);
		}
		backupTask.addVirtualMachineBackupEventHandler(handler);
		performanceTask
				.addVirtualMachinePerformanceMonitorEventHandler(handler);
		heartBeatMonitorTask.addHeartBeatEventHandler(handler);
		this.addVirtualMachineMonitorEventHandler(handler);
	}

	public void removeVirtualMachineServiceEventHandler(
			VirtualMachineServiceEventHandler handler) {
		if (this.vmServiceEventHandlers.contains(handler)) {
			vmServiceEventHandlers.remove(handler);
		}
		backupTask.removeVirtualMachineBackupEventHandler(handler);
		performanceTask
				.removeVirtualMachinePerformanceMonitorEventHandler(handler);
		heartBeatMonitorTask.removeHeartBeatEventHandler(handler);
		this.removeVirtualMachineMonitorEventHandler(handler);
	}

	public long getMonitorStartTime() {
		return monitorStartTime;
	}

	public long getMonitorEndTime() {
		return monitorEndTime;
	}

	public long getLastHeartBeatMissedTime() {
		return lastHeartBeatMissedTime;
	}

	public long getLastHeartBeatTime() {
		return lastHeartBeatTime;
	}

	public long getHeartBeatFailedTime() {
		return heartBeatFailedTime;
	}

	private long lastHeartBeatMissedTime;
	private long lastHeartBeatTime;
	private long heartBeatFailedTime;

	private VirtualMachineBackupTask backupTask;
	private VirtualMachineHeartBeatMonitor heartBeatMonitorTask;
	private VirtualMachinePerformanceMonitor performanceTask;
	private Timer powerOnTimer;

	public VirtualMachineMonitor(ServiceInstanceFactory factory,
			String virtualMachineName, InetAddress ipAddress) throws Exception {
		this.factory = factory;
		this.virtualMachineName = virtualMachineName;
		this.ipAddress = ipAddress;
		this.alarmnamePowerOff = virtualMachineName + "poff";
		this.alarmNamePowerOn = virtualMachineName + "pon";
		this.cloneName = "Clone-" + virtualMachineName;

		vmMonitorEventHandlers = new ArrayList<IVirtualMachineMonitorEventHandler>();
		vmServiceEventHandlers = new ArrayList<VirtualMachineServiceEventHandler>();

		backupTask = new VirtualMachineBackupTask(factory, virtualMachineName,
				Configuration.GetConfiguration().getBackupInterval());
		backupTask.addVirtualMachineBackupEventHandler(this);

		heartBeatMonitorTask = new VirtualMachineHeartBeatMonitor(
				virtualMachineName, ipAddress, Configuration.GetConfiguration()
						.getHeartBeat(), Configuration.GetConfiguration()
						.getPingInterval(), Configuration.GetConfiguration()
						.getPingTimeout());
		heartBeatMonitorTask.addHeartBeatEventHandler(this);

		performanceTask = new VirtualMachinePerformanceMonitor(factory,
				virtualMachineName, Configuration.GetConfiguration()
						.getMaxSamples());
		performanceTask.addVirtualMachinePerformanceMonitorEventHandler(this);
	}

	public String getCloneName() {
		return this.cloneName;
	}

	public int getStatus() {
		return this.status;
	}

	public boolean isRestored() {
		return this.restored;
	}

	public boolean isMonitoring() {
		return monitoring;
	}

	public InetAddress getIpAddress() {
		return ipAddress;
	}

	public String getVirtualMachineName() {
		return virtualMachineName;
	}

	public void startMonitor() throws Exception {
		checkVirtualMachinePowerState();
		if (STATUS_POWER_ON == this.status) {
			createAlarm(alarmnamePowerOff, true);
			backupTask.resetBackupTask(false);
			heartBeatMonitorTask.resetTask(false);
			performanceTask.resetTask(false);
		} else {
			long timeStamp = System.currentTimeMillis();
			for (IVirtualMachineMonitorEventHandler h : vmMonitorEventHandlers) {
				h.onVirtualMachinePoweredOff(this, timeStamp);
			}
			notifyOnPowerOn();
		}
	}

	public void stopMonitor() {
		backupTask.resetBackupTask(true);
		heartBeatMonitorTask.resetTask(true);
		performanceTask.resetTask(true);
		if (powerOnTimer != null) {
			powerOnTimer.cancel();
		}
	}

	private void createAlarm(String alarmName, boolean clear) throws Exception {
		ServiceInstance serviceInstance = factory.getServiceInstance();
		InventoryNavigator inventoryNavigator = new InventoryNavigator(
				serviceInstance.getRootFolder());
		VirtualMachine virtualMachine = (VirtualMachine) inventoryNavigator
				.searchManagedEntity("VirtualMachine", virtualMachineName);
		AlarmManager alarmManager = serviceInstance.getAlarmManager();
		Alarm[] alarms = alarmManager.getAlarm(virtualMachine);
		boolean found = false;
		for (Alarm a : alarms) {
			if (a.getAlarmInfo().getName().equals(alarmName)) {
				if (clear) {
					a.removeAlarm();
					found = false;
					break;
				}
				found = true;
				break;
			}
		}
		if (!found) {
			createAlarm(alarmName, virtualMachine, alarmManager);
		}
	}

	private void createAlarm(String alarmName, VirtualMachine virtualMachine,
			AlarmManager alarmManager) throws InvalidName, DuplicateName,
			RuntimeFault, RemoteException {
		AlarmSpec spec = new AlarmSpec();
		spec.setName(alarmName);
		EventAlarmExpression expression = new EventAlarmExpression();
		expression.setObjectType("VirtualMachine");
		expression.setStatus(ManagedEntityStatus.red);
		if (alarmName.equals(alarmNamePowerOn)) {
			expression.setEventType("VmPoweredOnEvent");
		} else {
			expression.setEventType("VmPoweredOffEvent");
		}
		spec.setExpression(expression);
		spec.setEnabled(true);
		spec.setDescription("");
		alarmManager.createAlarm(virtualMachine, spec);
	}

	private boolean checkEvents(String alarmName) throws Exception {
		ServiceInstance serviceInstance = factory.getServiceInstance();
		InventoryNavigator inventoryNavigator = new InventoryNavigator(
				serviceInstance.getRootFolder());
		VirtualMachine virtualMachine = (VirtualMachine) inventoryNavigator
				.searchManagedEntity("VirtualMachine", virtualMachineName);
		AlarmManager alarmManager = serviceInstance.getAlarmManager();

		Alarm[] alarms = alarmManager.getAlarm(virtualMachine);
		Alarm alarm = null;
		for (Alarm a : alarms) {
			if (a.getAlarmInfo().getName().equals(alarmName)) {
				alarm = a;
				break;
			}
		}
		if (alarm == null) {
			return false;
		}
		ManagedObjectReference alarmMor = alarm.getMOR();
		AlarmState[] states = alarmManager.getAlarmState(virtualMachine);
		for (AlarmState state : states) {
			if (state.getAlarm().getVal().equals(alarmMor.getVal())) {
				if ((state.getOverallStatus() == ManagedEntityStatus.red)
						&& !state.getAcknowledged()) {
					alarmManager.acknowledgeAlarm(alarm, virtualMachine);
					return true;
				}
			}
		}
		return false;
	}

	private void notifyOnPowerOn() throws Exception {
		final VirtualMachineMonitor monitor = this;
		if (powerOnTimer != null) {
			powerOnTimer.cancel();
		}
		createAlarm(alarmNamePowerOn, true);
		powerOnTimer = new Timer();
		powerOnTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					if (checkEvents(alarmNamePowerOn)) {
						this.cancel();
						long timeStamp = System.currentTimeMillis();
						for (IVirtualMachineMonitorEventHandler h : vmMonitorEventHandlers) {
							h.onVirtualMachinePoweredOn(monitor, timeStamp);
						}
						startMonitor();
					}
				} catch (Exception ex) {
				}

			}
		}, 0, 10000);

	}

	private void checkVirtualMachinePowerState() throws Exception {
		ServiceInstance serviceInstance = factory.getServiceInstance();
		InventoryNavigator inventoryNavigator = new InventoryNavigator(
				serviceInstance.getRootFolder());
		VirtualMachine virtualMachine = (VirtualMachine) inventoryNavigator
				.searchManagedEntity("VirtualMachine", virtualMachineName);
		if (virtualMachine == null) {
			throw new Exception("Virtual Machine '" + virtualMachineName
					+ "' does not exist.");
		}
		if (virtualMachine.getSummary().getRuntime().getPowerState() == VirtualMachinePowerState.poweredOn) {
			this.status = STATUS_POWER_ON;
		} else {
			this.status = STATUS_POWER_OFF;
		}
	}

	@Override
	public void onBackupSucceded(VirtualMachineBackupTask task, long timeStamp,
			String cloneName) {
		// this.cloneName = cloneName;
	}

	@Override
	public void onHeartBeatStopped(VirtualMachineHeartBeatMonitor checker,
			long timeStamp, long duration) {
		backupTask.resetBackupTask(true);
		performanceTask.resetTask(true);
		heartBeatMonitorTask.resetTask(true);

		try {
			if (checkEvents(alarmnamePowerOff)) {
				startMonitor();
				return;
			}
		} catch (Exception ex) {

		}

		this.status = STATUS_FAILURE;
		this.restored = false;
		this.heartBeatFailedTime = timeStamp;

		long tS = System.currentTimeMillis();
		for (IVirtualMachineMonitorEventHandler h : vmMonitorEventHandlers) {
			h.onVirtualMachineFailed(this, tS);
		}

		this.monitorEndTime = System.currentTimeMillis();
		this.monitoring = false;

		VirtualMachineRestoreTask restoreTask = new VirtualMachineRestoreTask(
				factory, virtualMachineName, cloneName, Configuration
						.GetConfiguration().getHosts());
		restoreTask.addVirtualMachineResotreEventHandler(this);
		for (VirtualMachineServiceEventHandler h : vmServiceEventHandlers) {
			restoreTask.addVirtualMachineResotreEventHandler(h);
		}
		restoreTask.restore();
	}

	@Override
	public void onRestoreCompleted(VirtualMachineRestoreTask task,
			long timeStamp) {
		this.restored = true;
		this.restoreHost = task.getTargetHost();
	}

	@Override
	public void onHeartBeatMissing(VirtualMachineHeartBeatMonitor checker,
			long timeStamp, long duration) {
		this.lastHeartBeatMissedTime = timeStamp;
	};

	@Override
	public void onHeartBeatSucceded(VirtualMachineHeartBeatMonitor checker,
			long timeStamp) {
		this.lastHeartBeatTime = timeStamp;
	}

	public String getRestoreHost() {
		return restoreHost;
	}

	ArrayList<IVirtualMachineMonitorEventHandler> vmMonitorEventHandlers;

	public void addVirtualMachineMonitorEventHandler(
			IVirtualMachineMonitorEventHandler handler) {
		if (!vmMonitorEventHandlers.contains(handler)) {
			vmMonitorEventHandlers.add(handler);
		}
	}

	public void removeVirtualMachineMonitorEventHandler(
			IVirtualMachineMonitorEventHandler handler) {
		if (vmMonitorEventHandlers.contains(handler)) {
			vmMonitorEventHandlers.remove(handler);
		}
	}

	public interface IVirtualMachineMonitorEventHandler {
		public void onVirtualMachinePoweredOn(VirtualMachineMonitor monitor,
				long timeStamp);

		public void onVirtualMachinePoweredOff(VirtualMachineMonitor monitor,
				long timeStamp);

		public void onVirtualMachineFailed(VirtualMachineMonitor monitor,
				long timeStamp);
	}

}
