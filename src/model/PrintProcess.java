package model;

import java.io.*;
import java.net.*;

public class PrintProcess extends MigratableProcess {

	private static final long serialVersionUID = 1L;
	private static String LOCAL_HOSTNAME;
	
	private transient Socket clientSocket;
	private transient OutputStream os;
	private transient ObjectOutputStream oos; // output object to server through socket
	private transient BufferedReader instructionReader; // read instruction from server
	private TransactionalFileInputStream  inFile;
	private TransactionalFileOutputStream outFile;
	private String query;
	private int coreValue; // core value of the thread, use to examine process migration
	private volatile boolean suspending;

	public PrintProcess(String args[]) {
		if (args.length != 3) {
			System.err.println("usage: PrintProcess <queryString> <inputFile> <outputFile>");
			// throw new Exception("Invalid Arguments");
			System.exit(1);
		}
		
		suspending = false;
		query = args[0];
		inFile = new TransactionalFileInputStream(args[1]);
		outFile = new TransactionalFileOutputStream(args[2]);
	}

	@Override
	public void run() {
		while (!suspending) {
			// periodically increase the coreValue
			coreValue++;
			System.out.println("    coreValue = " + coreValue);
			try {
				Thread.sleep(1000);
			} catch(InterruptedException e) {
				System.err.println("Thread is interrupted when sleep for one second.");
		        System.exit(1);
			}
		}
		suspending = false;		
	}

	@Override
	public void register(int port) {
		try {
			System.out.println("    Client(migratable process) starts.");
			// connect and register to server socket
			InetAddress addr = InetAddress.getLocalHost();
			LOCAL_HOSTNAME = addr.getHostAddress();
			clientSocket = new Socket(LOCAL_HOSTNAME, port);
			System.out.println("    Client has connected to the server.");
			// open object output stream and instruction reader stream
			os = clientSocket.getOutputStream();
			oos = new ObjectOutputStream(os);
			instructionReader =
	                new BufferedReader(
	                    new InputStreamReader(clientSocket.getInputStream()));
		} catch (UnknownHostException e) {
            System.err.println("Don't know about host " + LOCAL_HOSTNAME);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                LOCAL_HOSTNAME);
            System.exit(1);
        } 
		
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void suspend() {
		suspending = true;	
	}

	@Override
	public void resume() {
		suspending = false;
		run();
	}
	
	/**
	 * local test for client
	 */
	public static void main(String args[]) {
		// initialization
		String stringArgs[] = new String[3];
		stringArgs[0] = "test";
		stringArgs[1] = stringArgs[2] = "yijiem.txt";
		PrintProcess pp = new PrintProcess(stringArgs);
		
		// start process
		pp.register(ProcessManager.PORT_NUMBER);
		new Thread(pp).start();
	}
}
