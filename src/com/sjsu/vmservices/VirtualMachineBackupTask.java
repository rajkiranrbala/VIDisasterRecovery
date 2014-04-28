package com.sjsu.vmservices;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class VirtualMachineBackupTask {

	public interface IVirtualMachineBackupEventHandler {

		public void onBackupSucceded(VirtualMachineBackupTask task,
				long timeStamp, String cloneName);

		public void onBackupRequested(VirtualMachineBackupTask task,
				long timeStamp);

		public void onBackupFailed(VirtualMachineBackupTask task,
				long timeStamp, Exception ex);
	}

	private ServiceInstanceFactory factory;
	private String virtualMachineName;
	private Timer backupTimer;
	private ArrayList<IVirtualMachineBackupEventHandler> vmBackupEventHandlers;
	private String cloneName;
	private long backupInterval;
	private boolean backupInProgress;

	public String getCloneName() {
		return cloneName;
	}

	public long getBackupInterval() {
		return backupInterval;
	}

	public VirtualMachineBackupTask(ServiceInstanceFactory factory,
			String virtualMachineName, long backupInterval) {
		this.factory = factory;
		this.virtualMachineName = virtualMachineName;
		this.backupInterval = backupInterval;
		this.vmBackupEventHandlers = new ArrayList<IVirtualMachineBackupEventHandler>();
		this.cloneName = "Clone-" + virtualMachineName;
	}

	public void addVirtualMachineBackupEventHandler(
			IVirtualMachineBackupEventHandler handler) {
		if (!vmBackupEventHandlers.contains(handler)) {
			vmBackupEventHandlers.add(handler);
		}
	}

	public void removeVirtualMachineBackupEventHandler(
			IVirtualMachineBackupEventHandler handler) {
		if (vmBackupEventHandlers.contains(handler)) {
			vmBackupEventHandlers.remove(handler);
		}
	}

	public String getVirtualMachineName() {
		return this.virtualMachineName;
	}

	public void resetBackupTask(boolean stop) {
		final VirtualMachineBackupTask task = this;
		if (this.backupTimer != null) {
			backupTimer.cancel();
		}
		if (!stop) {
			backupTimer = new Timer();
			backupTimer.schedule(new TimerTask() {

				@Override
				public void run() {
					long timeStamp = System.currentTimeMillis();
					for (IVirtualMachineBackupEventHandler h : vmBackupEventHandlers) {
						h.onBackupRequested(task, timeStamp);
					}
					if (backupInProgress) {
						for (IVirtualMachineBackupEventHandler h : vmBackupEventHandlers) {
							Exception ex = new Exception(
									"Backup already in progress.");
							h.onBackupFailed(task, timeStamp, ex);
						}
						return;
					}
					try {
						backupInProgress = true;
						backupVirtualMachine();
						timeStamp = System.currentTimeMillis();
						for (IVirtualMachineBackupEventHandler h : vmBackupEventHandlers) {
							h.onBackupSucceded(task, timeStamp, cloneName);
						}
					} catch (Exception ex) {
						timeStamp = System.currentTimeMillis();
						for (IVirtualMachineBackupEventHandler h : vmBackupEventHandlers) {
							h.onBackupFailed(task, timeStamp, ex);
						}
					}
					backupInProgress = false;
				}
			}, 0, backupInterval);
		} else {
			backupTimer = null;
		}
	}

	private void backupVirtualMachine() throws Exception {
		ServiceInstance serviceInstance = factory.getServiceInstance();
		InventoryNavigator inventoryNavigator = new InventoryNavigator(
				serviceInstance.getRootFolder());
		VirtualMachine virtualMachine = (VirtualMachine) inventoryNavigator
				.searchManagedEntity("VirtualMachine", virtualMachineName);
		if (virtualMachine == null) {
			throw new Exception("Backup failed. Virtual Machine '"
					+ virtualMachineName + "' not found.");
		}
		String name = "Clone-" + virtualMachine.getName() + "-"
				+ System.currentTimeMillis();

		VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
		cloneSpec.setLocation(new VirtualMachineRelocateSpec());
		cloneSpec.setPowerOn(false);
		cloneSpec.setTemplate(false);

		Task task = virtualMachine.cloneVM_Task(
				(Folder) virtualMachine.getParent(), name, cloneSpec);
		@SuppressWarnings("deprecation")
		String status = task.waitForMe();
		if (status == Task.SUCCESS) {
			deleteVirtualMachineBackup(serviceInstance);
			virtualMachine.rename_Task(cloneName);
		} else {
			throw new Exception(task.getTaskInfo().getError().getFault()
					.toString());
		}

	}

	private void deleteVirtualMachineBackup(ServiceInstance serviceInstance) {
		try {
			InventoryNavigator inventoryNavigator = new InventoryNavigator(
					serviceInstance.getRootFolder());
			VirtualMachine virtualMachine = (VirtualMachine) inventoryNavigator
					.searchManagedEntity("VirtualMachine", cloneName);
			if (virtualMachine != null) {
				virtualMachine.destroy_Task();
			}
		} catch (Exception ex) {
		}
	}

}
