package com.sjsu.vmmanager;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Configuration {

	private static final String CONFIGURATION_FILE = "HealthMonitor.config";
	private static Configuration configuration;

	private Configuration() {
		hosts = new ArrayList<String>();
		virtualMachines = new HashMap<String, String>();
	}

	public static Configuration GetConfiguration() {
		if (configuration == null) {
			configuration = load();
		}
		return configuration;
	}

	private long heartBeat;
	private int pingTimeout;
	private long backupInterval;
	private long pingInterval;
	private String vcenterUrl;
	private String vcenterUserName;
	private String vcenterPassword;

	public String getVcenterUrl() {
		return vcenterUrl;
	}

	public void setVcenterUrl(String vcenterUrl) {
		this.vcenterUrl = vcenterUrl;
	}

	public String getVcenterUserName() {
		return vcenterUserName;
	}

	public void setVcenterUserName(String vcenterUserName) {
		this.vcenterUserName = vcenterUserName;
	}

	public String getVcenterPassword() {
		return vcenterPassword;
	}

	public void setVcenterPassword(String vcenterPassword) {
		this.vcenterPassword = vcenterPassword;
	}

	public ArrayList<String> getHosts() {
		return hosts;
	}

	public void setHosts(ArrayList<String> hosts) {
		this.hosts = hosts;
	}

	public HashMap<String, String> getVirtualMachines() {
		return virtualMachines;
	}

	public void setVirtualMachines(HashMap<String, String> virtualMachines) {
		this.virtualMachines = virtualMachines;
	}

	public void setHeartBeat(long heartBeat) {
		this.heartBeat = heartBeat;
	}

	public void setPingTimeout(int pingTimeout) {
		this.pingTimeout = pingTimeout;
	}

	public void setBackupInterval(long backupInterval) {
		this.backupInterval = backupInterval;
	}

	public void setPingInterval(long pingInterval) {
		this.pingInterval = pingInterval;
	}

	public void setMaxSamples(int maxSamples) {
		this.maxSamples = maxSamples;
	}

	private int maxSamples;
	private ArrayList<String> hosts;
	private HashMap<String, String> virtualMachines;

	public long getHeartBeat() {
		return heartBeat;
	}

	public int getPingTimeout() {
		return pingTimeout;
	}

	public long getBackupInterval() {
		return backupInterval;
	}

	public long getPingInterval() {
		return pingInterval;
	}

	public HashMap<String, String> getMonitoredVirtualmachines() {
		return virtualMachines;
	}

	public int getMaxSamples() {
		return this.maxSamples;
	}

	public static void save() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(configuration);
		FileWriter writer;
		try {
			writer = new FileWriter(CONFIGURATION_FILE);
			writer.write(json);
			writer.close();
		} catch (Exception e) {
		}
	}

	public static Configuration load() {
		try {
			Gson gson = new Gson();
			return (Configuration) gson.fromJson(new FileReader(
					CONFIGURATION_FILE), Configuration.class);
		} catch (Exception ex) {
			Configuration configuration = new Configuration();
			configuration.setBackupInterval(10 * 60 * 1000);
			configuration.setHeartBeat(5 * 60 * 1000);
			configuration.setMaxSamples(3);
			configuration.setPingInterval(5 * 1000);
			configuration.setPingTimeout(2 * 1000);
			configuration.setVcenterPassword("12!@qwQW");
			configuration.setVcenterUrl("https://130.65.132.170/sdk");
			configuration.setVcenterUserName("Administrator");
			return configuration;
		}
	}
}
