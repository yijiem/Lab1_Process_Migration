package test;

import model.ProcessManager;

/*
 * Driver for starting a ProcessManager, accepting connections
 * and launching console for user interaction 
 */
public class Driver {
	public static void main(String args[]) {
		
		// initialization
		ProcessManager manager = new ProcessManager();
		
		// take some initial setup time
		try {
		    Thread.sleep(1000);
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
		
		// launch console for taking user input command
		manager.launchConsole();	
		
	}
}
