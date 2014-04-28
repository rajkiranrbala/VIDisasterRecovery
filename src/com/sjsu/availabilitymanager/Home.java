package com.sjsu.availabilitymanager;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.DefaultListModel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sjsu.vmmanager.Configuration;
import com.sjsu.vmmanager.VirtualMachineMonitor;
import com.sjsu.vmmanager.VirtualMachineServiceEventHandler;
import com.sjsu.vmservices.ServiceInstanceFactory;
import com.sjsu.vmservices.VirtualMachineBackupTask;
import com.sjsu.vmservices.VirtualMachineHeartBeatMonitor;
import com.sjsu.vmservices.VirtualMachinePerformanceMonitor;
import com.sjsu.vmservices.VirtualMachineRestoreTask;
import com.vmware.vim25.mo.ServiceInstance;

/**
 * 
 * @author Rajkiran Ramachandran Balasubramanian
 */
public class Home extends javax.swing.JFrame {

	private static final long serialVersionUID = 3567389064665310783L;
	private ServiceEventHandler handler;
	private HashMap<String, JTabLog> logWindows;
	private VMLoader manager;
	ReentrantLock lock;

	public Home() {
		initComponents();
		lock = new ReentrantLock();
		setTitle("Virtual Machine Availability Manager");
		handler = new ServiceEventHandler();
		logWindows = new HashMap<String, JTabLog>();
	}

	private JTabLog getLogWindow(String name) {
		lock.lock();
		if (!logWindows.containsKey(name)) {
			JTabLog l = new JTabLog(name);
			logWindows.put(name, l);
			this.tabLogs.addTab(name, l);
		}
		lock.unlock();
		return logWindows.get(name);
	}

	private void initComponents() {
		setMinimumSize(new Dimension(800, 600));
		jSplitPane1 = new javax.swing.JSplitPane();
		jSplitPane2 = new javax.swing.JSplitPane();
		jPanel1 = new javax.swing.JPanel();
		btnMonitor = new javax.swing.JButton();
		jSplitPane4 = new javax.swing.JSplitPane();
		jTabbedPane3 = new javax.swing.JTabbedPane();
		jScrollPane4 = new javax.swing.JScrollPane();
		lstHosts = new javax.swing.JList<String>();
		jTabbedPane4 = new javax.swing.JTabbedPane();
		jScrollPane3 = new javax.swing.JScrollPane();
		lstVirtualMachines = new javax.swing.JList<String>();
		tabLogs = new javax.swing.JTabbedPane();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		getContentPane().setLayout(
				new javax.swing.BoxLayout(getContentPane(),
						javax.swing.BoxLayout.LINE_AXIS));

		jSplitPane2.setDividerLocation(35);
		jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

		btnMonitor.setText("Start Monitor");
		btnMonitor.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				performButtonAction();

			}
		});
		jPanel1.add(btnMonitor);

		jSplitPane2.setTopComponent(jPanel1);

		jSplitPane4.setDividerLocation(200);
		jSplitPane4.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

		lstHosts.setModel(new DefaultListModel<String>());
		jScrollPane4.setViewportView(lstHosts);

		jTabbedPane3.addTab("Hosts", jScrollPane4);

		jSplitPane4.setTopComponent(jTabbedPane3);

		lstVirtualMachines.setModel(new DefaultListModel<String>());
		jScrollPane3.setViewportView(lstVirtualMachines);

		jTabbedPane4.addTab("Virtual Machines", jScrollPane3);

		jSplitPane4.setRightComponent(jTabbedPane4);

		jSplitPane2.setRightComponent(jSplitPane4);

		jSplitPane1.setLeftComponent(jSplitPane2);

		jSplitPane1.setRightComponent(tabLogs);

		getContentPane().add(jSplitPane1);

		pack();
	}

	protected void performButtonAction() {
		if (btnMonitor.getText().equals("Start Monitor")) {
			final Configuration config = Configuration.GetConfiguration();
			HashMap<String, String> vms = config.getMonitoredVirtualmachines();
			if (vms.size() == 0) {
				getLogWindow("System").appendlog(
						"No virtual machines to monitor");
				return;
			}
			ArrayList<String> hosts = Configuration.GetConfiguration()
					.getHosts();
			DefaultListModel<String> model = (DefaultListModel<String>) lstHosts
					.getModel();
			for (String s : hosts) {
				model.addElement(s);
			}

			manager = new VMLoader(new ServiceInstanceFactory() {

				@Override
				public ServiceInstance getServiceInstance() {
					try {
						return new ServiceInstance(new URL(
								config.getVcenterUrl()),
								config.getVcenterUserName(),
								config.getVcenterPassword(), true);
					} catch (Exception ex) {
						return null;
					}
				}
			});
			manager.addVirtualMachineServiceEventHandler(handler);
			for (Entry<String, String> entry : vms.entrySet()) {
				try {
					String ipAddress = vms.get(entry.getKey());
					manager.addVirtualMachineToMonitor(entry.getKey(),
							ipAddress);
					((DefaultListModel<String>) lstVirtualMachines.getModel())
							.addElement(entry.getKey());
				} catch (Exception ex) {
					getLogWindow("System").appendlog(ex.getMessage());
				}
			}
			btnMonitor.setText("Stop Monitor");
		} else {
			manager.removeAllMonitors();
			btnMonitor.setText("Start Monitor");
		}

	}

	public static void main(String args[]) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(Home.class.getName()).log(
					java.util.logging.Level.SEVERE, null, ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(Home.class.getName()).log(
					java.util.logging.Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(Home.class.getName()).log(
					java.util.logging.Level.SEVERE, null, ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(Home.class.getName()).log(
					java.util.logging.Level.SEVERE, null, ex);
		}

		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new Home().setVisible(true);
			}
		});
	}

	private javax.swing.JButton btnMonitor;
	private javax.swing.JPanel jPanel1;

	private javax.swing.JScrollPane jScrollPane3;
	private javax.swing.JScrollPane jScrollPane4;
	private javax.swing.JSplitPane jSplitPane1;
	private javax.swing.JSplitPane jSplitPane2;
	private javax.swing.JSplitPane jSplitPane4;
	private javax.swing.JTabbedPane jTabbedPane3;
	private javax.swing.JTabbedPane jTabbedPane4;
	private javax.swing.JList<String> lstHosts;
	private javax.swing.JList<String> lstVirtualMachines;
	private javax.swing.JTabbedPane tabLogs;

	public class ServiceEventHandler extends VirtualMachineServiceEventHandler {
		private String convertTime(long time) {
			Date date = new Date(time);
			Format format = new SimpleDateFormat("yyyy MM dd HH:mm:ss");
			return format.format(date).toString();
		}

		@Override
		public void onBackupFailed(VirtualMachineBackupTask task,
				long timeStamp, Exception ex) {
			String log = String.format("[%s] [Backup] Backup Failed. %s.",
					convertTime(timeStamp), ex.getMessage());
			getLogWindow(task.getVirtualMachineName()).appendlog(log);
		}

		@Override
		public void onBackupSucceded(VirtualMachineBackupTask task,
				long timeStamp, String cloneName) {
			String log = String.format(
					"[%s] [Backup] Backup Successful. Clone Name %s.",
					convertTime(timeStamp), cloneName);
			getLogWindow(task.getVirtualMachineName()).appendlog(log);
		}

		@Override
		public void onBackupRequested(VirtualMachineBackupTask task,
				long timeStamp) {
			String log = String.format("[%s] [Backup] Performing Backup.",
					convertTime(timeStamp));
			getLogWindow(task.getVirtualMachineName()).appendlog(log);
		}

		@Override
		public void onHeartBeatMissing(VirtualMachineHeartBeatMonitor checker,
				long timeStamp, long duration) {
			String log = String.format(
					"[%s] [HeartBeat] Heart beat missing for %d ms.",
					convertTime(timeStamp), duration);
			getLogWindow(checker.getVirtualMachineName()).appendlog(log);
		}

		@Override
		public void onHeartBeatStopped(VirtualMachineHeartBeatMonitor checker,
				long timeStamp, long duration) {
			String log = String.format("[%s] [HeartBeat] Heart beat stopped.",
					convertTime(timeStamp), duration);
			getLogWindow(checker.getVirtualMachineName()).appendlog(log);
		}

		@Override
		public void onHeartBeatSucceded(VirtualMachineHeartBeatMonitor checker,
				long timeStamp) {
			String log = String.format(
					"[%s] [HeartBeat] Heart beat successful.",
					convertTime(timeStamp));
			getLogWindow(checker.getVirtualMachineName()).appendlog(log);
		}

		@Override
		public void onPerformanceParametersReceived(
				VirtualMachinePerformanceMonitor task,
				HashMap<String, HashMap<String, String>> parameters,
				long timeStamp) {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String json = gson.toJson(parameters);
			getLogWindow(task.getVirtualMachineName()).appendPerformanceLog(
					String.format("[%s]\n%s", convertTime(timeStamp), json));
		}

		@Override
		public void onVirtualMachineFailed(VirtualMachineMonitor monitor,
				long timeStamp) {
			String log = String.format("[%s] [Monitor] Virtual machine failed",
					convertTime(timeStamp));
			getLogWindow(monitor.getVirtualMachineName()).appendlog(log);
		}

		@Override
		public void onVirtualMachinePoweredOff(VirtualMachineMonitor monitor,
				long timeStamp) {
			String log = String.format(
					"[%s] [Monitor] Virtual machine switched off",
					convertTime(timeStamp));
			getLogWindow(monitor.getVirtualMachineName()).appendlog(log);
		}

		@Override
		public void onVirtualMachinePoweredOn(VirtualMachineMonitor monitor,
				long timeStamp) {
			String log = String.format(
					"[%s] [Monitor] Virtual machine switched on",
					convertTime(timeStamp));
			getLogWindow(monitor.getVirtualMachineName()).appendlog(log);
		}

		@Override
		public void onRestoreCompleted(VirtualMachineRestoreTask task,
				long timeStamp) {
			String log = String
					.format("[%s] [Restore] Virtual machine restored to clone %s on %s.",
							convertTime(timeStamp), task.getCloneName(),
							task.getTargetHost());
			getLogWindow(task.getVirtualMachineName()).appendlog(log);
		}

		@Override
		public void onRestoreFailed(VirtualMachineRestoreTask task,
				long timeStamp, Exception ex) {
			String log = String.format(
					"[%s] [Restore] Virtual machine restore failed. %s.",
					convertTime(timeStamp), ex.getMessage());
			getLogWindow(task.getVirtualMachineName()).appendlog(log);
		}

		@Override
		public void onRestoreRequested(VirtualMachineRestoreTask task,
				long timeStamp) {
			String log = String.format(
					"[%s] [Restore] Attempting virtual machine restore.",
					convertTime(timeStamp));
			getLogWindow(task.getVirtualMachineName()).appendlog(log);
		}

	}

	public class JTabLog extends JTabbedPane {
		private static final long serialVersionUID = 7056107110120765858L;
		private javax.swing.JScrollPane jScrollPane1;
		private javax.swing.JScrollPane jScrollPane2;
		private javax.swing.JTextArea txtLogs;
		private javax.swing.JTextArea txtPerformance;
		private String vmName;
		ReentrantLock logLock;
		ReentrantLock statisticsLock;

		public String getVmName() {
			return vmName;
		}

		public JTabLog(String vmName) {
			this.vmName = vmName;
			logLock = new ReentrantLock();
			statisticsLock = new ReentrantLock();
			init();
		}

		public void appendlog(String s) {
			logLock.lock();
			txtLogs.append(s);
			txtLogs.append("\n");
			logLock.unlock();
		}

		public void appendPerformanceLog(String s) {
			statisticsLock.lock();
			txtPerformance.setText("");
			txtPerformance.append(s);
			txtPerformance.append("\n");
			statisticsLock.unlock();
		}

		private void init() {
			jScrollPane1 = new javax.swing.JScrollPane();
			txtLogs = new javax.swing.JTextArea();
			jScrollPane2 = new javax.swing.JScrollPane();
			txtPerformance = new javax.swing.JTextArea();

			txtLogs.setEditable(false);
			txtLogs.setColumns(20);
			txtLogs.setRows(5);
			jScrollPane1.setViewportView(txtLogs);

			this.addTab("Logs", jScrollPane1);

			txtPerformance.setEditable(false);
			txtPerformance.setColumns(20);
			txtPerformance.setRows(5);
			jScrollPane2.setViewportView(txtPerformance);

			this.addTab("Performance", jScrollPane2);
		}
	}
}
