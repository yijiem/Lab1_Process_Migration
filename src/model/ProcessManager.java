package model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;

/*
 * ProcessManager functions as the control center, it launches
 * a console for user-interaction and monitors for requests
 * to launch, remove, and migrate processes, as well as displaying
 * current processes status
 */

public class ProcessManager {
	
	// ProcessManager generates a unique processID each time it launches a new process
	private static int processID = 0;
	
	private HashMap<Integer, Socket> slavelist;
	private HashMap<Integer, String> processlist;
	private HashMap<Integer, Integer> processSlaveMap;
	private HashMap<Integer, String> processStatusMap;
	private HashMap<Integer, ObjectOutputStream> slaveOutputStreamMap;
	private HashMap<Integer, ObjectInputStream> slaveInputStreamMap;
	private BufferedReader reader;
	private String userInput;
	private String cmdLineArguments[];
	private ConnectionHandler handler;
	private ObjectOutputStream outputStream;
	private ObjectInputStream inputStream;
	
	public ProcessManager() {
		System.out.println("Starting ProcessManager...");
		processlist = new HashMap<Integer, String>();
		processSlaveMap = new HashMap<Integer, Integer>();
		processStatusMap = new HashMap<Integer, String>();
		slaveOutputStreamMap = new HashMap<Integer, ObjectOutputStream>();
		slaveInputStreamMap = new HashMap<Integer, ObjectInputStream>();
		handler = new ConnectionHandler(Config.MASTER_PORT);
		// spawn a new thread to handle connection
		Thread t =new Thread(handler);
		t.start();
	}
	
	/**
	 * launches a console to continuously accept and process user commands
	 */
	public void launchConsole() {
		System.out.println("Initializing console for user-interaction...");
		reader = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			System.out.print("==> ");
			try {
				userInput = reader.readLine();
				if (userInput.equals("")) continue;
				cmdLineArguments = userInput.split(" ");
				String commandType = cmdLineArguments[0];
				// parse commands and process				
				if (commandType.equals("quit")) {
					System.out.println("terminating the command prompt...");
					System.exit(0);
				} else if (commandType.equals("suspend")) {
					int suspendProcessID = Integer.parseInt(cmdLineArguments[1]);
					System.out.println("Sending suspend signal to process with processID: " + suspendProcessID);
					boolean success = sendSuspendMsg(suspendProcessID);
					if (success)
						System.out.println("process has been suspended successfully!");
					else
						System.out.println("suspend failed...");
				} else if (commandType.equals("resume")) {
					int resumeProcessID = Integer.parseInt(cmdLineArguments[1]);
					System.out.println("Sending resume signal to process with processID: " + resumeProcessID);
					boolean success = sendResumeMsg(resumeProcessID);
					if (success)
						System.out.println("process has been resumed successfully!");
					else
						System.out.println("resume failed...");
				} else if (commandType.equals("migrate")) {
					int migrateProcessID = Integer.parseInt(cmdLineArguments[1]);
					int destinationSlaveID = Integer.parseInt(cmdLineArguments[2]);
					System.out.println("migrating process: " + processID + " to slave: " + destinationSlaveID);
					boolean success = migrate(migrateProcessID, destinationSlaveID);
					if (success)
						System.out.println("migration successful!");
					else
						System.out.println("migration failed...");
					
				} else if (commandType.equals("remove")) {
					int removeProcessID = Integer.parseInt(cmdLineArguments[1]);
					System.out.println("Sending remove signal to process with processID: " + removeProcessID);
					boolean success = remove(removeProcessID);
					if (success)
						System.out.println("process has been successfully removed!");
					else
						System.out.println("remove failed...");
				} else if (commandType.equals("info")) {
					displayInfo();
				} else {
					// command to start a new process
					String processName = cmdLineArguments[0];
					processID++;
					int slaveID = Integer.parseInt(cmdLineArguments[cmdLineArguments.length - 1]);
					String[] processArgs = null;
					if (cmdLineArguments.length > 2) {
						// retrieve argument list for the process to run with
						processArgs = new String[cmdLineArguments.length - 2];
						for (int i = 1; i < cmdLineArguments.length - 1; i++) {
							processArgs[i-1] = cmdLineArguments[i];
						}
					}
					System.out.println("starting a new process: " + processName + " on slave: " + slaveID);
					
					// start the user-required process in the required slave
					boolean success = startProcess(processName, processArgs, slaveID, processID);
					if (success)
						System.out.println("A new process has been successfully launched!");
					else
						System.out.println("Failed to start a new process...");
					
				}
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	/**
	 * displays current process information, including PID, processName,
	 * process status(running/suspending), location in the system
	 */
	private void displayInfo() {
		Iterator<Integer> nameIterator = processlist.keySet().iterator();
		while (nameIterator.hasNext()) {
			Integer pid = nameIterator.next();
			String processName = processlist.get(pid);
			// find the process status
			String processStatus = processStatusMap.get(pid);
			// locate where the process is running on
			Integer slaveID = processSlaveMap.get(processID);
			String status = "[Process ID: " + pid.intValue();
			status += ", Process Name: " + processName;
			status += ", Process Status: " + processStatus;
			status += ", Location: Slave " + slaveID.intValue() + "]";
			System.out.println(status);
		}	
	}

	/**
	 * start a new process at a specified location
	 * 
	 * @param processName name of the process class
	 * @param processArgs arguments of the process
	 * @param slaveID the id of the slave node to run the process at
	 * @param processID the generated pid
	 * @return acknowledgement of success or failure
	 */
	public boolean startProcess(String processName, String[] processArgs, int slaveID, int processID) {
		// find the desired slave in the slavelist
		slavelist = handler.getSlaveList();
		Iterator<Integer> slaveIterator = slavelist.keySet().iterator();
		Socket slaveSocket = null;
		while (slaveIterator.hasNext()){
		  Integer id = slaveIterator.next();
		  if (id.intValue() == slaveID) {
			  slaveSocket = slavelist.get(id);
		  }		  
		}
		if (slaveSocket != null) {			
			try {
				if (slaveOutputStreamMap.containsKey(slaveID)) {
					outputStream = slaveOutputStreamMap.get(slaveID);
				} else {
					outputStream = new ObjectOutputStream(slaveSocket.getOutputStream());
					inputStream = new ObjectInputStream(slaveSocket.getInputStream());
					slaveOutputStreamMap.put(slaveID, outputStream);
					slaveInputStreamMap.put(slaveID, inputStream);
				}				
				outputStream.writeObject("start");
				outputStream.writeObject(processName);
				outputStream.writeObject(processArgs);
				outputStream.writeObject(new Integer(processID));
				String feedback = (String) inputStream.readObject();
				if (feedback.equals("fail")) {
					System.out.println("process failed to start...");
					return false;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			// record the process in the processSlaveMap and processlist
			processSlaveMap.put(processID, slaveID);
			processlist.put(processID, processName);
			processStatusMap.put(processID, "running");
		} else {
			System.out.println("slave with such slaveID does not exist!");
		}
		return true;
	}
	
	/**
	 * send suspend instruction to a migratable process
	 * 
	 * @param processID id of the process to be suspended
	 * @return acknowledgement of success or failure
	 */
	public boolean sendSuspendMsg(int processID) {
		
		// find what slave the process is running/suspending on
		Integer targetSlaveID = processSlaveMap.get(processID);
		if (targetSlaveID != null) {
			// locate the slave socket and use the socket to send suspend signal
			Socket slaveSocket = slavelist.get(targetSlaveID);
			if (slaveSocket != null) {
				try {
					// see if the process has already suspended
					if (processStatusMap.get(processID).equals("suspending")) {
						return true;
					}
					outputStream = slaveOutputStreamMap.get(targetSlaveID);
					outputStream.writeObject("suspend");
					outputStream.writeObject(processID);
					
					inputStream = slaveInputStreamMap.get(targetSlaveID);
					String suspendOK = null;
					suspendOK = (String) inputStream.readObject();
					if (suspendOK.equals("fail"))
						return false;
					// update process status
					processStatusMap.put(processID, "suspending");					
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("Error when locating slave socket...");
			}
		}
		return true;
	}
	
	/**
	 * send resume message to a migratable process
	 * 
	 * @param processID pid of the process to be resumed
	 * @return acknowledgement of success or failure
	 */
	public boolean sendResumeMsg(int processID) {
		// find what slave the process is running/suspending on
		Integer targetSlaveID = processSlaveMap.get(processID);
		if (targetSlaveID != null) {
			// locate the slave socket and use the socket to send resume signal
			Socket slaveSocket = slavelist.get(targetSlaveID);
			if (slaveSocket != null) {
				try {
					outputStream = slaveOutputStreamMap.get(targetSlaveID);
					outputStream.writeObject("resume");
					outputStream.writeObject(new Integer(processID));
					
					// wait for acknowledgement
					inputStream = slaveInputStreamMap.get(targetSlaveID);
					String resumeOK = null;
					resumeOK = (String) inputStream.readObject();				
					if (resumeOK.equals("fail"))
						return false;
					// update process status
					processStatusMap.put(processID, "running");			
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("Error when locating slave socket...");
			}
		}
		return true;
	}
	
	/**
	 * migrate a process to a specified location
	 * 
	 * @param processID pid of the process to be migrated
	 * @param targetSlaveID migration destination
	 * @return acknowledgement of success or failure
	 */
	public boolean migrate(int processID, int targetSlaveID) {
		// find the current location of the process
		Integer currentLocation = processSlaveMap.get(processID);
		if (currentLocation.intValue() == targetSlaveID) {
			System.out.println("Process already running/suspending at this location...");
		} else {
			// suspend the signal if its currently running
			if (processStatusMap.get(processID).equals("running")) {
				boolean suspendOK = sendSuspendMsg(processID);
				if (!suspendOK) return false;
			}
			try {
				// contact the slave currently holding the process
				Integer slaveID = processSlaveMap.get(processID);
				outputStream = slaveOutputStreamMap.get(slaveID);	
				outputStream.writeObject("migrate");
				outputStream.writeObject(processID);
				
				// wait for acknowledgement of successful serialization
				inputStream = slaveInputStreamMap.get(slaveID);
				String serializeOK = (String) inputStream.readObject();
				if (!serializeOK.equals("done")) {
					return false;
				}

				// contact the recieving slave
				outputStream = slaveOutputStreamMap.get(targetSlaveID);
				if (outputStream == null) {
					Socket slaveSocket = slavelist.get(targetSlaveID);
					outputStream = new ObjectOutputStream(slaveSocket.getOutputStream());
					inputStream = new ObjectInputStream(slaveSocket.getInputStream());
					slaveOutputStreamMap.put(targetSlaveID, outputStream);
					slaveInputStreamMap.put(targetSlaveID, inputStream);
				}
				outputStream.writeObject("receive");
				outputStream.writeObject(processID);
				inputStream = slaveInputStreamMap.get(targetSlaveID);
				String receiveOK = (String) inputStream.readObject();
				if (receiveOK.equals("fail"))
					return false;				
				// update process location
				processSlaveMap.put(processID, targetSlaveID);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	/**
	 * remove a currently alive process
	 * 
	 * @param processID: pid of the process to be removed
	 * @return acknowledgement of success or failure
	 */
	public boolean remove(int processID) {
		Integer targetSlaveID = processSlaveMap.get(processID);
		outputStream = slaveOutputStreamMap.get(targetSlaveID);
		if (outputStream != null) {
			try {
				if (sendSuspendMsg(processID) == false)
					return false;
				outputStream.writeObject("remove");
				outputStream.writeObject(processID);
				// wait for acknowledgement
				String removeOK = (String) inputStream.readObject();
				if (removeOK.equals("fail"))
					return false;
				// remove the process from data structure
				processlist.remove(processID);
				processStatusMap.remove(processID);
				processSlaveMap.remove(processID);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

}
