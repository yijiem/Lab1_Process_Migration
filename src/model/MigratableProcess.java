package model;

import java.io.Serializable;

/*
 * Any process which extends this abstract base class is considered migratable in our system 
 */
public abstract class MigratableProcess implements Runnable, Serializable {

	private static final long serialVersionUID = 6418384768265078145L;
	
	/**
	 * run method of Thread
	 */
	public abstract void run();
	
	/**
	 * produce a simple string representation of the object,
	 * containing class name and its arguments
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
	
	/**
	 * check if the process has already completed
	 * @return true if the process has already completed
	 */
	public abstract boolean isComplete();
}
