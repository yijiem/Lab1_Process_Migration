package model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

/*
 * Slave which communicates with master(Process Manager) to
 * run Threads(Migratable Process) under its context and receive
 * user command to manipulate(start, suspend, resume, migrate, 
 * receive) Threads.
 */
public class Slave {
	
	private HashMap<Integer, MigratableProcess> threadsHashMap; // class type is unknown
	// Connect With master
	private Socket socketWithMaster;
	private String instruction;
	private ObjectInputStream oisWithMaster;
	private ObjectOutputStream oosWithMaster;
	
	/**
	 * Create a Slave instance and connect to master 
	 * by designating hostname and port number of master
	 * @param hostname
	 * @param port
	 */
	public Slave(String hostname, int port){
		System.out.println("    A Slave starts...");
		try {
			// connect to master
			socketWithMaster = new Socket(hostname, port);
			oisWithMaster = new ObjectInputStream(socketWithMaster.getInputStream());
			oosWithMaster = new ObjectOutputStream(socketWithMaster.getOutputStream());
			System.out.println("    Slave has connected to the master...");
		} catch (UnknownHostException e) {
	        System.err.println("Slave: Don't know about host " + hostname);
	        System.exit(1);
	    } catch (IOException e) {
	        System.err.println("Slave: Couldn't get I/O for the connection to " +
	            hostname);
	        System.exit(1);
	    } 
		
		threadsHashMap = new HashMap<Integer, MigratableProcess>();
	}
	
	/**
	 * Run slave process
	 * receive master command and process correspondingly
	 * Master command categories:  1. start   ---- start a new thread
	 * 							   2. suspend ---- suspend an exiting thread
	 * 							   3. resume  ---- resume a suspended thread
	 * 							   4. migrate ---- migrate a suspended thread
	 * 							   5. receive ---- receive a migrated thread
	 * 							   6. remove  ---- remove a suspended thread
	 */
	public void run() {
		while(true) {
			
			try {
				instruction = (String) oisWithMaster.readObject();
			} catch(IOException e) {
				e.printStackTrace();
				System.err.println("Slave: Couldn't read instruction from master.");
			    continue;
			} catch(ClassNotFoundException cnfe) {
				System.err.println("Slave: String class not found.");
			    continue;
			}
			
			// create and start migratable thread
			if(instruction.equals("start")) {
				System.out.println("Slave has received start signal from master, start a thread immediately...");
				try {
					// get process name
					String processName = (String) oisWithMaster.readObject();
					System.out.println("processName = " + processName);
					// get arguments
					String[] arguments = (String[]) oisWithMaster.readObject();
					System.out.println("length of arguments = " + arguments.length);
					// get Thread id
					int threadId = (Integer) oisWithMaster.readObject();
					System.out.println("threadId = " + threadId);
					// create migratable process using generic class expression
					Class<?> className = Class.forName("test." + processName);
					Constructor<?> constructor = className.getConstructor(String[].class);				
					MigratableProcess mp = (MigratableProcess) constructor.newInstance(new Object[] {arguments});
					
					// put this thread into thread hash map
					threadsHashMap.put(threadId, mp);
					// start thread
					new Thread(mp).start();
					// send success signal to master
					oosWithMaster.writeObject("success");
					// System.out.println("Thread has been started...");
				} catch(IOException e) {
					try {
						// send fail signal to master
						oosWithMaster.writeObject("fail");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					System.err.println("Slave: Couldn't read (process name/arguments/threadId) from master, "
							+ "maybe master do not follow protocol.");
				    continue;
				} catch(ClassNotFoundException cnfe) {
					try {
						oosWithMaster.writeObject("fail");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					System.err.println("Slave: Arguments/ThreadId class not found.");
				    continue;
				} catch(InstantiationException ie) {
					try {
						oosWithMaster.writeObject("fail");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					ie.printStackTrace();
					continue;
				} catch(IllegalAccessException iae) {
					try {
						oosWithMaster.writeObject("fail");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					iae.printStackTrace();
					continue;
				} catch(NoSuchMethodException nsme) {
					try {
						oosWithMaster.writeObject("fail");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					nsme.printStackTrace();
					continue;
				} catch(InvocationTargetException ite) {
					try {
						oosWithMaster.writeObject("fail");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					ite.printStackTrace();
					continue;
				}
				
			// suspend a thread determined by its Thread id
			} else if(instruction.equals("suspend")) {		
				System.out.println("Slave has received suspend signal from master, suspend a thread immediately...");
				try {
					// get Thread id
					int threadId = (Integer) oisWithMaster.readObject();
					System.out.println("threadId = " + threadId);
					// get thread from hashmap
					MigratableProcess mp = threadsHashMap.get(threadId);
					if(mp.isComplete()) {
						// if thread is completed, then send false signal to process manager
						System.out.println("The thread is already completed, so just ignore...");
						oosWithMaster.writeObject("fail");
						continue;
					} else {
						// if thread is incompleted, then suspend thread and send true signal to process manager
						mp.suspend();
						oosWithMaster.writeObject("success");
						oosWithMaster.flush();
						System.out.println("The thread is incompleted, so suspend...");
					}
				} catch(IOException e) {
					System.err.println("Slave: Couldn't read threadId from master.");
				    continue;
				} catch(ClassNotFoundException cnfe) {
					System.err.println("Slave: ThreadId class not found.");
				    continue;
				}
			
			// resume a thread determined by its Thread id
			} else if(instruction.equals("resume")) {
				System.out.println("Slave has received resume signal from master, resume a thread immediately...");
				try {
					// get Thread id
					int threadId = (Integer) oisWithMaster.readObject();
					// get thread from hashmap
					MigratableProcess mp = threadsHashMap.get(threadId);
					if(mp.isComplete()) {
						System.out.println("The thread is already completed, so just ignore...");
						oosWithMaster.writeObject("fail");
						continue;
					} else {
						// resume thread
						mp.resume();
						new Thread(mp).start();
						oosWithMaster.writeObject("success");
					}
				} catch(IOException e) {
					System.err.println("Slave: Couldn't read threadId from master.");
				    continue;
				} catch(ClassNotFoundException cnfe) {
					System.err.println("Slave: ThreadId class not found.");
				    continue;
				}
				
			// migrate(serialize) thread in a .ser file	
			} else if(instruction.equals("migrate")) { 
				System.out.println("Slave has received migrate signal from master, migrate a thread immediately...");
				try {
					// get Thread id
					int threadId = (Integer) oisWithMaster.readObject();
					// get thread from hashmap
					MigratableProcess mp = threadsHashMap.get(threadId);
					if(mp.isComplete()) {
						System.out.println("The thread is already completed, so just ignore...");
						oosWithMaster.writeObject("fail");
						continue;
					} else {
						new File(Config.DIRECTORY + "migrate" + threadId + ".ser");
						FileOutputStream file = new FileOutputStream(Config.DIRECTORY + "migrate" + threadId + ".ser");
						ObjectOutputStream objectOutStrm = new ObjectOutputStream(file);
			            objectOutStrm.writeObject(mp);
			            objectOutStrm.flush();
			            objectOutStrm.close();
			            // remove thread from node's hashmap
			            threadsHashMap.remove(threadId);
			            // send done signal to master
			            oosWithMaster.writeObject("done");
			            oosWithMaster.flush();
			            System.out.println("Slave has stored migrating thread in the shared file system.");
					}
				} catch(IOException e) {
					System.err.println("Slave: Couldn't read threadId from master.");
				    continue;
				} catch(ClassNotFoundException cnfe) {
					System.err.println("Slave: ThreadId class not found.");
				    continue;
				}
			
			// receive thread from a .ser file
			} else if(instruction.equals("receive")) {
				System.out.println("Slave has received receive signal from master, "
						+ "receive a thread from .ser file immediately...");
				try {
					// get Thread id
					int threadId = (Integer) oisWithMaster.readObject();
					FileInputStream file = new FileInputStream(Config.DIRECTORY + "migrate" + threadId + ".ser");
					ObjectInputStream objectInStrm = new ObjectInputStream(file);
					MigratableProcess mp = (MigratableProcess) objectInStrm.readObject();
					objectInStrm.close();
		            // add thread into node's hashmap
		            threadsHashMap.put(threadId, mp);
		            System.out.println("Slave has received migrating thread from the shared file system.");
		            oosWithMaster.writeObject("success");
				} catch(IOException e) {
					try {
						oosWithMaster.writeObject("fail");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					System.err.println("Slave: Couldn't read threadId from master.");
				    continue;
				} catch(ClassNotFoundException cnfe) {
					try {
						oosWithMaster.writeObject("fail");
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.err.println("Slave: ThreadId class not found.");
				    continue;
				}
				
			} else if(instruction.equals("remove")) {
				// remove thread
				System.out.println("Slave has received remove signal from master, remove a thread immediately...");
				try {
					// get Thread id
					int threadId = (Integer) oisWithMaster.readObject();		
		            // remove thread from node's hashmap
		            threadsHashMap.remove(threadId);
		            System.out.println("Slave has removed thread from the its hash map.");
		            oosWithMaster.writeObject("success");
				} catch(IOException e) {
					try {
						oosWithMaster.writeObject("fail");
					} catch (IOException e1) {
						e.printStackTrace();
					}
					System.err.println("Slave: Couldn't read threadId from master.");
				    continue;
				} catch(ClassNotFoundException cnfe) {
					try {
						oosWithMaster.writeObject("fail");
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.err.println("Slave: ThreadId class not found.");
				    continue;
				}				
			}
		}
	}
	
	/**
	 * Initialize a slave and start it.
	 */
	public static void main(String args[]) {
		Slave newSlave = new Slave(Config.MASTER_IP, Config.MASTER_PORT);
		newSlave.run();
	}
}
