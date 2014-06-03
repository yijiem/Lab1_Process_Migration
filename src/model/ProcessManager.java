package model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;

public class ProcessManager {
	
	private static int PORT_NUMBER = 7777;
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
	
	// constructor
	public ProcessManager() {
		System.out.println("Starting ProcessManager...");
		processlist = new HashMap<Integer, String>();
		processSlaveMap = new HashMap<Integer, Integer>();
		processStatusMap = new HashMap<Integer, String>();
		slaveOutputStreamMap = new HashMap<Integer, ObjectOutputStream>();
		slaveInputStreamMap = new HashMap<Integer, ObjectInputStream>();
		handler = new ConnectionHandler(PORT_NUMBER);
		// spawn a new thread for handling connection
		Thread t =new Thread(handler);
		t.start();
	}
	
	// launch a console for user input command
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
								
				if (commandType.equals("quit")) {
					System.out.println("terminating the command prompt...");
					System.exit(0);
				} else if (commandType.equals("suspend")) {
					// read the second argument
					int suspendProcessID = Integer.parseInt(cmdLineArguments[1]);
					sendSuspendMsg(suspendProcessID);
				} else if (commandType.equals("resume")) {
					// read the second argument
					int resumeProcessID = Integer.parseInt(cmdLineArguments[1]);
					sendResumeMsg(resumeProcessID);					
				} else if (commandType.equals("migrate")) {
					// read the processID and migration destination slave
					int migrateProcessID = Integer.parseInt(cmdLineArguments[1]);
					int destinationSlaveID = Integer.parseInt(cmdLineArguments[2]);
					migrate(migrateProcessID, destinationSlaveID);
				} else if (commandType.equals("remove")) {
					// read the second argument
					int removeProcessID = Integer.parseInt(cmdLineArguments[1]);
					remove(removeProcessID);
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
					System.out.println("starting a new process: " + processName);
					System.out.println("target slaveID: " + slaveID);
					
					// start the user-required process in the required slave
					startProcess(processName, processArgs, slaveID, processID);
					
				}
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	// start a new process in a specified slave node
	public void startProcess(String processName, String[] processArgs, int slaveID, int processID) {
		// find the desired slave in the slavelist
		slavelist = handler.getSlaveList();
		Iterator<Integer> slaveIterator = slavelist.keySet().iterator();
		Socket slaveSocket = null;
		while(slaveIterator.hasNext()){
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
			} catch (IOException e) {
				e.printStackTrace();
			}			
			// record the process in the processSlaveMap and processlist
			processSlaveMap.put(processID, slaveID);
			processlist.put(processID, processName);
			processStatusMap.put(processID, "running");
		} else {
			System.out.println("slave with such slaveID does not exist!");
		}
	}
	
	public boolean sendSuspendMsg(int processID) {
		// send suspend instruction to a migratable process
		System.out.println("Sending suspend signal to process with processID: " + processID);
		// find what slave the process is running/suspending on
		Integer targetSlaveID = getProcessLocation(processID);
		if (targetSlaveID != null) {
			// locate the slave socket and use the socket to send suspend signal
			Socket slaveSocket = slavelist.get(targetSlaveID);
			if (slaveSocket != null) {
				try {
					outputStream = slaveOutputStreamMap.get(targetSlaveID);
					outputStream.writeObject("suspend");
					outputStream.writeObject(processID);
					
					inputStream = slaveInputStreamMap.get(targetSlaveID);
					String suspendOK = null;
					try {
						suspendOK = (String) inputStream.readObject();
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (suspendOK.equals("false"))
						return false;					
					// update process status
					processStatusMap.put(processID, "suspending");
										
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				System.out.println("Error when locating slave socket...");
			}
		}
		return true;
	}
	
	// send resume message to a specific process
	public void sendResumeMsg(int processID) {
		// send resume instruction to a migratable process
		System.out.println("Sending resume signal to process with processID: " + processID);
		// find what slave the process is running/suspending on
		Integer targetSlaveID = getProcessLocation(processID);
		if (targetSlaveID != null) {
			// locate the slave socket and use the socket to send resume signal
			Socket slaveSocket = slavelist.get(targetSlaveID);
			if (slaveSocket != null) {
				try {
					outputStream = slaveOutputStreamMap.get(targetSlaveID);
					outputStream.writeObject("resume");
					outputStream.writeObject(new Integer(processID));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// update process status
				processStatusMap.put(processID, "running");
			} else {
				System.out.println("Error when locating slave socket...");
			}
		}	
	}
	
	// migrate a process from location A to location B
	public void migrate(int processID, int targetSlaveID) {
		// find the current location of the process
		Integer currentLocation = getProcessLocation(processID);
		if (currentLocation.intValue() == targetSlaveID) {
			System.out.println("Process already running/suspending at this location...");
		} else {
			// suspend the signal if its currently running
			if (processStatusMap.get(processID).equals("running")) {
				boolean suspendOK = sendSuspendMsg(processID);
				if (!suspendOK) return;
			}
			try {
				// contact the slave currently holding the process
				Integer slaveID = getProcessLocation(processID);
				outputStream = slaveOutputStreamMap.get(slaveID);	
				outputStream.writeObject("migrate");
				outputStream.writeObject(processID);
				Thread.sleep(1000);
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
				// update process location
				processSlaveMap.put(processID, targetSlaveID);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
	}
	
	// remove a process
	public void remove(int processID) {
		Integer targetSlaveID = getProcessLocation(processID);
		outputStream = slaveOutputStreamMap.get(targetSlaveID);
		if (outputStream != null) {
			try {
				outputStream.writeObject("suspend");
				outputStream.writeObject(processID);
				outputStream.writeObject("remove");
				outputStream.writeObject(processID);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// remove the process from data structure
			processlist.remove(processID);
			processStatusMap.remove(processID);
			processSlaveMap.remove(processID);
		}
	}
	
	// get process location based on the processID
	public Integer getProcessLocation(int processID) {
		Integer targetSlaveID = null;
		Iterator<Integer> mappingIterator = processSlaveMap.keySet().iterator();
		while (mappingIterator.hasNext()) {
			Integer id = mappingIterator.next();
			if (id.intValue() == processID) {
				targetSlaveID = processSlaveMap.get(id);
			}
		}
		return targetSlaveID;
	}

}
