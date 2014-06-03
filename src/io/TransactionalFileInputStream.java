package io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;

/*
 * When a read is requested via the library, it opens the file, seek to the requisite location, 
 * perform the operation, and close the file again. It maintains all the information 
 * required in order to continue performing read operations on the file, 
 * even if the process is transferred to another node. It uses a migration flag for caching purpose.
 */
public class TransactionalFileInputStream extends InputStream implements Serializable {
	
	private static final long serialVersionUID = -5455698830722521273L;
	private File file;
	private int pointer; // file position
	private boolean migrated; // migrated flag
	private transient RandomAccessFile reader;
	
	public TransactionalFileInputStream(String filename) {
		file = new File(filename);
		pointer = 0;
		migrated = false;
		try {
			reader = new RandomAccessFile(file, "r");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * set the migrated flag to true
	 */
	public void setMigrated() {
		this.migrated = true;
	}
	
	/**
	 * read the next byte from file
	 */
	@Override
	public int read() throws IOException {
		int returnValue;
		// open the file
		if (migrated) {
			reader = new RandomAccessFile(file, "r");
			migrated = false; // reset flag
		}
		// seek to requisite location
		reader.seek(pointer);
		// perform operation
		returnValue = reader.read();
		pointer++;
		// close file
		reader.close();
		return returnValue;
	}	
}

