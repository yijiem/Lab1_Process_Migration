package test;

import model.MigratableProcess;
import io.*;

public class PrintProcess extends MigratableProcess {

	private static final long serialVersionUID = 1L;
	
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
		return "PrintProcess";
	}

	@Override
	public void suspend() {
		suspending = true;	
	}

	@Override
	public void resume() {
		suspending = false;
		// run();
	}
}
