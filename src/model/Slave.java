package model;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public class Slave {
	
	private HashMap<Integer, MigratableProcess> threadsHashMap; // class type is unknown
	// private String HOSTNAME;

	// Connect With master
	private Socket socketWithMaster;
	// private BufferedReader instructionReader;
	private String instruction;
	private InputStream isWithMaster;
	private ObjectInputStream oisWithMaster;
	
	// Connect With another node(slave)
	private OutputStream osWithSlave;
	private ObjectOutputStream oosWithSlave;
	
	public Slave(String hostname, int port){
		System.out.println("    A Slave starts...");
		try {
			// connect to master
			// InetAddress addr = InetAddress.getLocalHost();
			// LOCAL_HOSTNAME = addr.getHostAddress();
			socketWithMaster = new Socket(hostname, port);
//			instructionReader =
//	                new BufferedReader(
//	                    new InputStreamReader(socketWithMaster.getInputStream()));
			isWithMaster = socketWithMaster.getInputStream();
			oisWithMaster = new ObjectInputStream(isWithMaster);
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
	
	public void run() {
		while(true) {
			
			try {
				instruction = (String) oisWithMaster.readObject();
			} catch(IOException e) {
				e.printStackTrace();
				System.err.println("Slave: Couldn't read instruction from master.");
			    System.exit(1);
			} catch(ClassNotFoundException cnfe) {
				System.err.println("Slave: String class not found.");
			    System.exit(1);
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
					Class<?> className = Class.forName("model." + processName);
					Constructor<?> constructor = className.getConstructor(String[].class);				
					MigratableProcess mp = (MigratableProcess) constructor.newInstance(new Object[] {arguments});
					
					// put this thread into thread hash map
					threadsHashMap.put(threadId, mp);
					// start thread
					new Thread(mp).start();
					// System.out.println("Thread has been started...");
				} catch(IOException e) {
					System.err.println("Slave: Couldn't read (process name/arguments/threadId) from master, "
							+ "maybe master do not follow protocol.");
				    System.exit(1);
				} catch(ClassNotFoundException cnfe) {
					System.err.println("Slave: Arguments/ThreadId class not found.");
				    System.exit(1);
				} catch(InstantiationException ie) {
					ie.printStackTrace();
					System.exit(1);
				} catch(IllegalAccessException iae) {
					iae.printStackTrace();
					System.exit(1);
				} catch(NoSuchMethodException nsme) {
					nsme.printStackTrace();
					System.exit(1);
				} catch(InvocationTargetException ite) {
					ite.printStackTrace();
					System.exit(1);
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
					// suspend thread
					mp.suspend();
				} catch(IOException e) {
					System.err.println("Slave: Couldn't read threadId from master.");
				    System.exit(1);
				} catch(ClassNotFoundException cnfe) {
					System.err.println("Slave: ThreadId class not found.");
				    System.exit(1);
				}
			
			// resume a thread determined by its Thread id
			} else if(instruction.equals("resume")) {
				System.out.println("Slave has received resume signal from master, resume a thread immediately...");
				try {
					// get Thread id
					int threadId = (Integer) oisWithMaster.readObject();
					// get thread from hashmap
					MigratableProcess mp = threadsHashMap.get(threadId);
					// resume thread
					mp.resume();
				} catch(IOException e) {
					System.err.println("Slave: Couldn't read threadId from master.");
				    System.exit(1);
				} catch(ClassNotFoundException cnfe) {
					System.err.println("Slave: ThreadId class not found.");
				    System.exit(1);
				}
				
			} else if(instruction.equals("kill")) {
				// kill slave
				System.out.println("Slave is killed by master...Bye Bye");
				System.exit(0);
			}
		}
	}
	
	/**
	 * Start a slave process
	 */
	public static void main(String args[]) {
		Slave newSlave = new Slave("128.237.220.212", 7777);
		newSlave.run();
	}
}
