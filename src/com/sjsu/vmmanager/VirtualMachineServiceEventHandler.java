package com.sjsu.vmmanager;

import java.util.HashMap;

import com.sjsu.vmmanager.VirtualMachineMonitor.IVirtualMachineMonitorEventHandler;
import com.sjsu.vmservices.VirtualMachineBackupTask;
import com.sjsu.vmservices.VirtualMachineBackupTask.IVirtualMachineBackupEventHandler;
import com.sjsu.vmservices.VirtualMachineHeartBeatMonitor;
import com.sjsu.vmservices.VirtualMachineHeartBeatMonitor.IVirtualMachineHeartBeatEventHandlers;
import com.sjsu.vmservices.VirtualMachinePerformanceMonitor;
import com.sjsu.vmservices.VirtualMachinePerformanceMonitor.IVirtualMachinePerformanceMetricsEventHandler;
import com.sjsu.vmservices.VirtualMachineRestoreTask;
import com.sjsu.vmservices.VirtualMachineRestoreTask.IVirtualMachineRestorEventHandler;

public class VirtualMachineServiceEventHandler implements
		IVirtualMachinePerformanceMetricsEventHandler,
		IVirtualMachineHeartBeatEventHandlers,
		IVirtualMachineBackupEventHandler, IVirtualMachineRestorEventHandler,
		IVirtualMachineMonitorEventHandler {

	@Override
	public void onVirtualMachinePoweredOn(VirtualMachineMonitor monitor,
			long timeStamp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onVirtualMachinePoweredOff(VirtualMachineMonitor monitor,
			long timeStamp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onVirtualMachineFailed(VirtualMachineMonitor monitor,
			long timeStamp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRestoreRequested(
			VirtualMachineRestoreTask task, long timeStamp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRestoreCompleted(
			VirtualMachineRestoreTask task, long timeStamp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRestoreFailed(VirtualMachineRestoreTask task,
			long timeStamp, Exception ex) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBackupSucceded(VirtualMachineBackupTask task, long timeStamp,
			String cloneName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBackupRequested(VirtualMachineBackupTask task, long timeStamp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBackupFailed(VirtualMachineBackupTask task, long timeStamp,
			Exception ex) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onHeartBeatStopped(VirtualMachineHeartBeatMonitor checker,
			long timeStamp, long duration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onHeartBeatSucceded(VirtualMachineHeartBeatMonitor checker,
			long timeStamp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onHeartBeatMissing(VirtualMachineHeartBeatMonitor checker,
			long timeStamp, long duration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPerformanceParametersReceived(
			VirtualMachinePerformanceMonitor task,
			HashMap<String, HashMap<String, String>> parameters, long timeStamp) {
		// TODO Auto-generated method stub

	}

}