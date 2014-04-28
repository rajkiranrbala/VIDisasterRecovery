package com.sjsu.vmservices;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.ComputeResource;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class VirtualMachineRestoreTask {

	public interface IVirtualMachineRestorEventHandler {

		public void onRestoreRequested(
				VirtualMachineRestoreTask task, long timeStamp);

		public void onRestoreCompleted(
				VirtualMachineRestoreTask task, long timeStamp);

		public void onRestoreFailed(
				VirtualMachineRestoreTask task, long timeStamp, Exception ex);

	}

	private ArrayList<IVirtualMachineRestorEventHandler> vmRestoreEventHandlers;
	private ServiceInstanceFactory factory;
	private String cloneName;
	private String targetHost;
	private List<String> targetHosts;
	private String virtualMachineName;
	private static Random random = new Random();

	public String getVirtualMachineName() {
		return virtualMachineName;
	}

	public String getCloneName() {
		return cloneName;
	}

	public String getTargetHost() {
		return targetHost;
	}

	public VirtualMachineRestoreTask(ServiceInstanceFactory factory,
			String virtualMachineName, String cloneName,
			List<String> targetHosts) {
		this.virtualMachineName = virtualMachineName;
		this.factory = factory;
		this.cloneName = cloneName;
		this.targetHosts = targetHosts;
		vmRestoreEventHandlers = new ArrayList<IVirtualMachineRestorEventHandler>();
	}

	public void addVirtualMachineResotreEventHandler(
			IVirtualMachineRestorEventHandler handler) {

		if (!vmRestoreEventHandlers.contains(handler)) {
			vmRestoreEventHandlers.add(handler);
		}
	}

	public void removeVirtualMachineResotreEventHandler(
			IVirtualMachineRestorEventHandler handler) {
		if (vmRestoreEventHandlers.contains(handler)) {
			vmRestoreEventHandlers.remove(handler);
		}
	}

	@SuppressWarnings("deprecation")
	public void restore() {

		long timeStamp = System.currentTimeMillis();
		for (IVirtualMachineRestorEventHandler h : vmRestoreEventHandlers) {
			h.onRestoreRequested(this, timeStamp);
		}

		ServiceInstance si = factory.getServiceInstance();
		Folder rootFolder = si.getRootFolder();
		VirtualMachine vm = null;
		try {
			vm = (VirtualMachine) new InventoryNavigator(rootFolder)
					.searchManagedEntity("VirtualMachine", cloneName);
			if (vm == null) {
				throw new Exception("Clone Virtual Machine '" + cloneName
						+ "' not found.");
			}
		} catch (Exception ex) {
			timeStamp = System.currentTimeMillis();
			for (IVirtualMachineRestorEventHandler h : vmRestoreEventHandlers) {
				h.onRestoreFailed(this, timeStamp, ex);
			}
			return;
		}

		for (int i = 0; i < targetHosts.size(); i++) {
			targetHost = targetHosts.get(random.nextInt(targetHosts.size()));
			try {

				HostSystem host = (HostSystem) new InventoryNavigator(
						rootFolder).searchManagedEntity("HostSystem",
						targetHost);
				if (host == null) {
					throw new Exception("Target Host '" + targetHost
							+ "' not found.");
				}
				ComputeResource cr = (ComputeResource) host.getParent();
				Task task = vm.migrateVM_Task(cr.getResourcePool(), host,
						VirtualMachineMovePriority.highPriority,
						VirtualMachinePowerState.poweredOff);

				if (task.waitForMe() == Task.SUCCESS) {
					vm = (VirtualMachine) new InventoryNavigator(rootFolder)
							.searchManagedEntity("VirtualMachine", cloneName);
					task = vm.powerOnVM_Task(host);
					if (task.waitForMe() == Task.SUCCESS) {
						timeStamp = System.currentTimeMillis();
						for (IVirtualMachineRestorEventHandler h : vmRestoreEventHandlers) {
							h.onRestoreCompleted(this, timeStamp);
						}
						return;
					} else {
						throw new Exception("Failed to start virtual machine '"
								+ cloneName
								+ "' on host '"
								+ targetHost
								+ "'. "
								+ task.getTaskInfo().getError().getFault()
										.toString());
					}
				} else {
					throw new Exception("Failed to migrate virtual machine '"
							+ cloneName
							+ "' to host '"
							+ targetHost
							+ "'. "
							+ task.getTaskInfo().getError().getFault()
									.toString());
				}

			} catch (Exception ex) {
				timeStamp = System.currentTimeMillis();
				for (IVirtualMachineRestorEventHandler h : vmRestoreEventHandlers) {
					h.onRestoreFailed(this, timeStamp, ex);
				}
			}
		}
	}
}
