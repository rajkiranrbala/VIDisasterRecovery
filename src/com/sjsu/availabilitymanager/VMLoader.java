package com.sjsu.availabilitymanager;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.HashMap;

import com.sjsu.vmmanager.VirtualMachineMonitor;
import com.sjsu.vmmanager.VirtualMachineServiceEventHandler;
import com.sjsu.vmservices.ServiceInstanceFactory;

public class VMLoader {

	private ServiceInstanceFactory factory;
	private HashMap<String, VirtualMachineMonitor> virtualMachines;
	private ArrayList<VirtualMachineServiceEventHandler> vmServiceEventHandlers;

	public VMLoader(ServiceInstanceFactory factory) {
		this.factory = factory;
		virtualMachines = new HashMap<String, VirtualMachineMonitor>();
		vmServiceEventHandlers = new ArrayList<VirtualMachineServiceEventHandler>();
	}

	public void addVirtualMachineServiceEventHandler(
			VirtualMachineServiceEventHandler h) {
		if (!vmServiceEventHandlers.contains(h)) {
			vmServiceEventHandlers.add(h);
			for (VirtualMachineMonitor m : virtualMachines.values()) {
				m.addVirtualMachineServiceEventHandler(h);
			}
		}

	}

	public void removeVirtualMachineServiceEventHandler(
			VirtualMachineServiceEventHandler h) {
		if (vmServiceEventHandlers.contains(h)) {
			vmServiceEventHandlers.remove(h);
			for (VirtualMachineMonitor m : virtualMachines.values()) {
				m.removeVirtualMachineServiceEventHandler(h);
			}
		}
	}

	public void addVirtualMachineToMonitor(String name, String ipAddress)
			throws Exception {
		if (!virtualMachines.containsKey(name)) {
			VirtualMachineMonitor monitor = new VirtualMachineMonitor(factory,
					name, Inet4Address.getByName(ipAddress));
			for (VirtualMachineServiceEventHandler h : vmServiceEventHandlers) {
				monitor.addVirtualMachineServiceEventHandler(h);
			}
			monitor.startMonitor();
			virtualMachines.put(name, monitor);
		} else {
			throw new Exception("Virtual machine '" + name
					+ "' already being monitored.");
		}
	}

	public void removeVirtualMachineFromMonitor(String name) throws Exception {
		if (!virtualMachines.containsKey(name)) {
			VirtualMachineMonitor monitor = virtualMachines.get(name);
			for (VirtualMachineServiceEventHandler h : vmServiceEventHandlers) {
				monitor.removeVirtualMachineServiceEventHandler(h);
			}
			monitor.stopMonitor();
			virtualMachines.remove(name);
		} else {
			throw new Exception("Virtual machine '" + name
					+ "' not being monitored.");
		}
	}

	public void removeAllMonitors() {
		for (VirtualMachineMonitor m : virtualMachines.values()) {
			m.stopMonitor();
		}
		virtualMachines.clear();
	}
}
