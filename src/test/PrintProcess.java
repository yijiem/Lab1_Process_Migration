package test;

import model.MigratableProcess;
import io.*;

/*
 * PrintProcess is a thread that periodically add and print
 * a core value variable on the console.
 */
public class PrintProcess extends MigratableProcess {

	private static final long serialVersionUID = 1L;
	
	private TransactionalFileInputStream  inFile;
	private TransactionalFileOutputStream outFile;
	private String query;
	private String[] arguments;
	private int coreValue; // core value of the thread, use to examine process migration
	private volatile boolean suspending;
	private volatile boolean complete;
	
	/**
	 * Create a PrintProcess object
	 * @param args
	 */
	public PrintProcess(String args[]) {
		if (args.length != 3) {
			System.err.println("usage: PrintProcess <queryString> <inputFile> <outputFile>");
			System.exit(1);
		}
		complete = false;
		suspending = false;
		arguments = args;
		query = args[0];
		inFile = new TransactionalFileInputStream(args[1]);
		outFile = new TransactionalFileOutputStream(args[2]);
	}

	/**
	 * Run function of thread
	 */
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
		if (!suspending) {
			complete = true;
		}
		suspending = false;		
	}

	/**
	 * produce a simple string representation of PrintProcess object
	 */
	@Override
	public String toString() {
		String info = "Arguments: ";
		for (int i = 0; i < arguments.length; i++) {
			info += arguments[i] + " ";
		}
		return "[PrintProcess," + info + "]";
	}
	
	/**
	 * suspend running
	 */
	@Override
	public void suspend() {
		suspending = true;
		while(suspending);
	}

	/**
	 * resume running
	 */
	@Override
	public void resume() {
		suspending = false;
	}
	
	/**
	 * check whether thread is completed or not
	 */
	public boolean isComplete() {
		return complete;
	}
}
