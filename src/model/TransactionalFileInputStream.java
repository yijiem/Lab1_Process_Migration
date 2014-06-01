package model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;

public class TransactionalFileInputStream extends InputStream implements Serializable {
	
	private static final long serialVersionUID = 2L;
	private File file;
	private int pointer;
	
	public TransactionalFileInputStream(String filename) {
		file = new File(filename);
		pointer = 0;
	}
	
	/**
	 * read the next byte from file
	 */
	@Override
	public int read() throws IOException {
		int returnValue;
		// open the file
		RandomAccessFile reader = new RandomAccessFile(file, "r");
		// seek to requisite location
		reader.seek(pointer);
		// perform operation
		returnValue = reader.read();
		pointer++;
		// close file
		reader.close();
		return returnValue;
	}
	
	
	/**
	 * local test method
	 * @param args
	 */
	public static void main(String args[]) {
		TransactionalFileInputStream inputStream = new TransactionalFileInputStream("yijiem.txt");
		int returnValue;
		try{
			while((returnValue = inputStream.read()) != -1) {
				System.out.print((char) returnValue);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
}

