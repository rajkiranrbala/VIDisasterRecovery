package com.sjsu.vmservices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfEntityMetric;
import com.vmware.vim25.PerfEntityMetricBase;
import com.vmware.vim25.PerfMetricIntSeries;
import com.vmware.vim25.PerfMetricSeries;
import com.vmware.vim25.PerfMetricSeriesCSV;
import com.vmware.vim25.PerfProviderSummary;
import com.vmware.vim25.PerfQuerySpec;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.PerformanceManager;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

public class VirtualMachinePerformanceMonitor {

	private static HashMap<Integer, PerfCounterInfo> perfCounterInfoMap = new HashMap<Integer, PerfCounterInfo>();
	private static boolean initialized = false;

	static void initialize(ServiceInstanceFactory factory) {
		ServiceInstance si = factory.getServiceInstance();
		PerformanceManager performanceManager = si.getPerformanceManager();
		PerfCounterInfo[] infos = performanceManager.getPerfCounter();
		for (PerfCounterInfo info : infos) {
			perfCounterInfoMap.put(new Integer(info.getKey()), info);
		}

	}

	public interface IVirtualMachinePerformanceMetricsEventHandler {
		public void onPerformanceParametersReceived(
				VirtualMachinePerformanceMonitor task,
				HashMap<String, HashMap<String, String>> parameters,
				long timeStamp);
	}

	private String virtualMachineName;
	private ServiceInstanceFactory factory;
	private ArrayList<IVirtualMachinePerformanceMetricsEventHandler> vmPerformanceMonitorEventHandlers;
	private Timer monitorTimer;
	private int refreshRate;
	private int maxSamples;

	public VirtualMachinePerformanceMonitor(ServiceInstanceFactory factory,
			String virtualMachineName, int maxSamples) {
		this.factory = factory;
		this.virtualMachineName = virtualMachineName;
		this.vmPerformanceMonitorEventHandlers = new ArrayList<IVirtualMachinePerformanceMetricsEventHandler>();
		this.setMaxSamples(maxSamples);
		if (!initialized) {
			initialize(factory);
		}
	}

	public void addVirtualMachinePerformanceMonitorEventHandler(
			IVirtualMachinePerformanceMetricsEventHandler handler) {
		if (!vmPerformanceMonitorEventHandlers.contains(handler)) {
			vmPerformanceMonitorEventHandlers.add(handler);
		}
	}

	public void removeVirtualMachinePerformanceMonitorEventHandler(
			IVirtualMachinePerformanceMetricsEventHandler handler) {
		if (vmPerformanceMonitorEventHandlers.contains(handler)) {
			vmPerformanceMonitorEventHandlers.remove(handler);
		}

	}

	public String getVirtualMachineName() {
		return virtualMachineName;
	}

	public void resetTask(boolean stop) {
		try {
			if (this.monitorTimer != null) {
				monitorTimer.cancel();
			}
			if (!stop) {
				ServiceInstance serviceInstance = factory.getServiceInstance();
				InventoryNavigator inventoryNavigator = new InventoryNavigator(
						serviceInstance.getRootFolder());
				VirtualMachine virtualMachine = (VirtualMachine) inventoryNavigator
						.searchManagedEntity("VirtualMachine",
								virtualMachineName);
				if (virtualMachine == null) {
					throw new Exception("Virtual Machine '"
							+ virtualMachineName + "' not found.");
				}

				PerformanceManager performanceManager = serviceInstance
						.getPerformanceManager();
				PerfProviderSummary pps = performanceManager
						.queryPerfProviderSummary(virtualMachine);
				this.refreshRate = pps.getRefreshRate().intValue();

				monitorTimer = new Timer();
				monitorTimer.schedule(new TimerTask() {

					@Override
					public void run() {
						getPerformanceMetrics();
					}
				}, 0, refreshRate * 1000);
			} else {
				monitorTimer = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println(ex.getMessage());

		}
	}

	protected void getPerformanceMetrics() {
		try {
			ServiceInstance serviceInstance = factory.getServiceInstance();
			InventoryNavigator inventoryNavigator = new InventoryNavigator(
					serviceInstance.getRootFolder());
			VirtualMachine virtualMachine = (VirtualMachine) inventoryNavigator
					.searchManagedEntity("VirtualMachine", virtualMachineName);
			if (virtualMachine == null) {
				throw new Exception("Virtual Machine '" + virtualMachineName
						+ "' not found.");
			}

			PerformanceManager performanceManager = serviceInstance
					.getPerformanceManager();

			PerfQuerySpec perfQuerySpec = new PerfQuerySpec();
			perfQuerySpec.setEntity(virtualMachine.getMOR());
			perfQuerySpec.setMaxSample(new Integer(maxSamples));
			perfQuerySpec.setFormat("normal");
			perfQuerySpec.setIntervalId(new Integer(refreshRate));

			PerfEntityMetricBase[] pValues = performanceManager
					.queryPerf(new PerfQuerySpec[] { perfQuerySpec });

			if (pValues != null) {
				generatePerformanceResult(pValues);
			}

		} catch (Exception ex) {

		}
	}

	public void generatePerformanceResult(PerfEntityMetricBase[] pValues) {
		HashMap<String, HashMap<String, String>> propertyGroups = new HashMap<String, HashMap<String, String>>();
		for (PerfEntityMetricBase p : pValues) {
			PerfEntityMetric pem = (PerfEntityMetric) p;
			PerfMetricSeries[] pms = pem.getValue();
			for (PerfMetricSeries pm : pms) {
				int counterId = pm.getId().getCounterId();
				PerfCounterInfo info = perfCounterInfoMap.get(new Integer(
						counterId));

				String value = "";

				if (pm instanceof PerfMetricIntSeries) {
					PerfMetricIntSeries series = (PerfMetricIntSeries) pm;
					long[] values = series.getValue();
					long result = 0;
					for (long v : values) {
						result += v;
					}
					result = (long) (result / values.length);
					value = String.valueOf(result) + " "
							+ info.getUnitInfo().getLabel();
				} else if (pm instanceof PerfMetricSeriesCSV) {
					PerfMetricSeriesCSV seriesCsv = (PerfMetricSeriesCSV) pm;
					value = seriesCsv.getValue() + " in "
							+ info.getUnitInfo().getLabel();
				}

				HashMap<String, String> properties;
				if (propertyGroups.containsKey(info.getGroupInfo().getKey())) {
					properties = propertyGroups.get(info.getGroupInfo()
							.getKey());
				} else {
					properties = new HashMap<String, String>();
					propertyGroups
							.put(info.getGroupInfo().getKey(), properties);
				}

				String propName = String.format("[%s.%s]", info.getNameInfo()
						.getKey(), info.getRollupType());
				properties.put(propName, value);
			}
		}
		long timeStamp = System.currentTimeMillis();
		for (IVirtualMachinePerformanceMetricsEventHandler handler : vmPerformanceMonitorEventHandlers) {
			handler.onPerformanceParametersReceived(this, propertyGroups,
					timeStamp);
		}

	}

	public int getMaxSamples() {
		return maxSamples;
	}

	public void setMaxSamples(int maxSamples) {
		this.maxSamples = maxSamples;
		if (this.maxSamples < 1) {
			this.maxSamples = 1;
		}
	}

}
