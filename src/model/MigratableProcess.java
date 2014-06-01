package model;

import java.io.Serializable;

public abstract class MigratableProcess implements Runnable, Serializable {

	// private static final long serialVersionUID = 1L;
	
	/**
	 * run method of Thread
	 */
	public abstract void run();
	
	/**
	 * register MigratableProcess to ProcessManager
	 * @param port
	 */
	public abstract void register(int port);
	
	/**
	 * produce a simple string representation of the object
	 */
	public abstract String toString();
	
	/**
	 * suspend thread
	 * call before object is serialized
	 */
	public abstract void suspend();
	
	/**
	 * resume the process which has been suspended and then migrated
	 */
	public abstract void resume();
}
