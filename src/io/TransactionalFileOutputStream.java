package io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;

/*
 * When a write is requested via the library, it opens the file, seek to the requisite location, 
 * perform the operation, and close the file again. It maintains all the information 
 * required in order to continue performing write operations on the file, 
 * even if the process is transferred to another node. It uses a migration flag for caching purpose.
 */
public class TransactionalFileOutputStream extends OutputStream implements Serializable {

	private static final long serialVersionUID = 659312719307594816L;
	private File file;
	private int pointer;
	private boolean migrated;
	private transient RandomAccessFile writer;
	
	public TransactionalFileOutputStream(String filename) {
		file = new File(filename);
		pointer = 0;
		migrated = false;
		try {
			writer = new RandomAccessFile(file, "rws");
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
	 * Writes the specified byte to this output stream.
	 * The general contract for write is that one byte is written to the output stream. 
	 * The byte to be written is the eight low-order bits of the argument b.
	 * The 24 high-order bits of b are ignored.
	 */
	@Override
	public void write(int arg0) throws IOException {
		if (migrated) {
			writer = new RandomAccessFile(file, "rws");
			migrated = false; // reset flag
		}
		writer.seek(pointer);
		writer.write(arg0);
		pointer++;
		writer.close();
	}
	
}

