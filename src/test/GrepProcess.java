package test;

import io.TransactionalFileInputStream;
import io.TransactionalFileOutputStream;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;

import model.MigratableProcess;

public class GrepProcess extends MigratableProcess
{
	private TransactionalFileInputStream  inFile;
	private TransactionalFileOutputStream outFile;
	private String query;
	private String[] arguments;

	private volatile boolean suspending;
	private volatile boolean complete;
	private volatile int count;

	public GrepProcess(String args[]) throws Exception
	{
		if (args.length != 3) {
			System.out.println("usage: GrepProcess <queryString> <inputFile> <outputFile>");
			throw new Exception("Invalid Arguments");
		}
		
		arguments = args;
		suspending = false;
		complete = false;
		count = 0;
		query = args[0];
		inFile = new TransactionalFileInputStream(args[1]);
		outFile = new TransactionalFileOutputStream(args[2]);
	}

	public void run()
	{
		PrintStream out = new PrintStream(outFile);
		DataInputStream in = new DataInputStream(inFile);

		try {
			while (!suspending) {
				String line = in.readLine();
				if (line == null) break;
				if (line.contains(query)) {
					out.println(line);
					System.out.println(++count + ": " + line);
				}
				// Make grep take longer so that we don't require extremely large files for interesting results
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// ignore it
				}
			}
			if (!suspending) {
				complete = true;
			}
		} catch (EOFException e) {
			//End of File
		} catch (IOException e) {
			System.out.println ("GrepProcess: Error: " + e);
		}

		suspending = false;
	}

	public void suspend()
	{
		suspending = true;
	}

	@Override
	public String toString() {
		String info = "Arguments: ";
		for (int i = 0; i < arguments.length; i++) {
			info += arguments[i] + " ";
		}
		return "[GrepProcess," + info + "]";
	}

	@Override
	public void resume() {
		suspending = false;
	}
	
	public boolean isComplete() {
		return complete;
	}
}