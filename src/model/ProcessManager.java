package model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ProcessManager {
	
	public static int PORT_NUMBER = 7171;
	
	private ServerSocket serverSocket;
	private Socket clientSocket;
	private PrintWriter instructionOutput;
	private ObjectInputStream ois;
	private MigratableProcess mp;
	
	public void run() {
		System.out.println("****Process Manager is started.");
		// open server socket, listen from client, and build required streams
		try {
			serverSocket = new ServerSocket(PORT_NUMBER);
			System.out.println("****Waiting for client to connect.");
			clientSocket = serverSocket.accept();
			System.out.println("****Client has connected.");
			instructionOutput =
	                new PrintWriter(clientSocket.getOutputStream(), true);
			ois = new ObjectInputStream(clientSocket.getInputStream());
		} catch(IOException e) {
			System.err.println("Oops, error occurs with multiple choices.");
			e.printStackTrace();
			System.exit(1);
		}

		// send suspend instruction to migratable process
		instructionOutput.println("suspend");
		System.out.println("****Send suspend signal to client.");
		
		// read migratable process
		try{
			mp = (MigratableProcess) ois.readObject();
			System.out.println("****Receive migratable process from client.");
		} catch(ClassNotFoundException cnfe) {
			System.err.println("Couldn't find class during receiving of object.");
			System.exit(1);
		} catch(IOException e) {
			System.err.println("Couldn't receive object from stream.");
			System.exit(1);
		}
		
		System.out.println("****Resume migratable process.");
		// resume the migratable process
		Thread newThread = new Thread(mp);
		newThread.start();
	}
	
	/**
	 * local test for server
	 * @param args
	 */
	public static void main(String args[]) {
		// initialization
		ProcessManager pm = new ProcessManager();
		
		// start process
		pm.run();
	}
}
