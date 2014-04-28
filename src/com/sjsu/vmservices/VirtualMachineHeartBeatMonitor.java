package com.sjsu.vmservices;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class VirtualMachineHeartBeatMonitor {

	public interface IVirtualMachineHeartBeatEventHandlers {
		public void onHeartBeatStopped(VirtualMachineHeartBeatMonitor checker,
				long timeStamp, long duration);

		public void onHeartBeatSucceded(VirtualMachineHeartBeatMonitor checker,
				long timeStamp);

		public void onHeartBeatMissing(VirtualMachineHeartBeatMonitor checker,
				long timeStamp, long duration);
	}

	ArrayList<IVirtualMachineHeartBeatEventHandlers> heartBeatEventHandlers;

	private InetAddress ipAddress;
	private long duration;
	private int timeout;
	private long interval;

	private long pingStartTime;
	private Timer pingTimer;
	private long pingFailTime;

	private String virtualMachineName;

	public String getVirtualMachineName() {
		return virtualMachineName;
	}

	public VirtualMachineHeartBeatMonitor(String virtualmachineName,
			InetAddress ipAddress, long duration, long interval, int timeout) {
		this.virtualMachineName = virtualmachineName;
		this.ipAddress = ipAddress;
		this.interval = interval;
		this.duration = duration;
		this.timeout = timeout;
		this.heartBeatEventHandlers = new ArrayList<IVirtualMachineHeartBeatEventHandlers>();
		this.pingStartTime = System.currentTimeMillis();
	}

	public InetAddress getIpAddress() {
		return ipAddress;
	}

	public long getDuration() {
		return duration;
	}

	public int getTimeout() {
		return timeout;
	}

	public long getInterval() {
		return interval;
	}

	public void addHeartBeatEventHandler(
			IVirtualMachineHeartBeatEventHandlers handler) {
		if (!this.heartBeatEventHandlers.contains(handler)) {
			this.heartBeatEventHandlers.add(handler);
		}
	}

	public void removeHeartBeatEventHandler(
			IVirtualMachineHeartBeatEventHandlers handler) {
		if (this.heartBeatEventHandlers.contains(handler)) {
			this.heartBeatEventHandlers.remove(handler);
		}
	}

	public void resetTask(boolean stop) {
		if (this.pingTimer != null) {
			pingTimer.cancel();
		}
		if (!stop) {
			pingTimer = new Timer();
			pingTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						if (ping(ipAddress.getHostAddress())) {
							pingSucceded();
						} else {
							pingFailed();
						}
					} catch (Exception ex) {
						pingFailed();
					}
				}
			}, 0, interval);
		} else {
			pingTimer = null;
		}
	}

	private static boolean ping(String host) {
		try {
			boolean isWindows = System.getProperty("os.name").toLowerCase()
					.contains("win");

			ProcessBuilder processBuilder = new ProcessBuilder("ping",
					isWindows ? "-n" : "-c", "1", host);
			Process proc = processBuilder.start();

			int returnVal = proc.waitFor();
			return returnVal == 0;
		} catch (Exception ex) {
			return false;
		}
	}

	protected void pingSucceded() {
		pingStartTime = System.currentTimeMillis();
		for (IVirtualMachineHeartBeatEventHandlers h : this.heartBeatEventHandlers) {
			h.onHeartBeatSucceded(this, pingStartTime);
		}
	}

	protected void pingFailed() {
		pingFailTime = System.currentTimeMillis();
		if ((pingFailTime - pingStartTime) >= duration) {
			for (IVirtualMachineHeartBeatEventHandlers h : this.heartBeatEventHandlers) {
				h.onHeartBeatStopped(this, pingFailTime, pingFailTime
						- pingStartTime);
			}
		} else {
			for (IVirtualMachineHeartBeatEventHandlers h : this.heartBeatEventHandlers) {
				h.onHeartBeatMissing(this, pingFailTime, pingFailTime
						- pingStartTime);
			}
		}
	}
}
