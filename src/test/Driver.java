package test;

import model.ProcessManager;

public class Driver {
	public static void main(String args[]) {
		
		// initialization
		ProcessManager manager = new ProcessManager();
		
		try {
		    Thread.sleep(1000);
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
		
		// launch console for taking user input command
		manager.launchConsole();	
		
	}
}
