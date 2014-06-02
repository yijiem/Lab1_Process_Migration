package test;

import java.io.*;
import java.net.*;

import model.MigratableProcess;
import io.*;

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
}
