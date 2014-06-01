package model;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;

public class TransactionalFileOutputStream extends OutputStream implements Serializable {

	private static final long serialVersionUID = 3L;
	private File file;
	private int pointer;
	
	public TransactionalFileOutputStream(String filename) {
		file = new File(filename);
		pointer = 0;
	}

	/**
	 * Writes the specified byte to this output stream.
	 * The general contract for write is that one byte is written to the output stream. 
	 * The byte to be written is the eight low-order bits of the argument b.
	 * The 24 high-order bits of b are ignored.
	 */
	@Override
	public void write(int arg0) throws IOException {
		RandomAccessFile writer = new RandomAccessFile(file, "rws");
		writer.seek(pointer);
		writer.write(arg0);
		pointer++;
		writer.close();
	}

	/**
	 * local test
	 * @param args
	 */
	public static void main(String args[]) {
		TransactionalFileOutputStream outputStream = new TransactionalFileOutputStream("yijiem.txt");
		try{
			for(int i = 40; i < 45; i++) {
				outputStream.write(i);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}

