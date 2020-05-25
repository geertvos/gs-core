package net.geertvos.gvm.streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class RandomAccessByteStream {

	private final int blocksize;
	private final List<byte[]> buffers = new ArrayList<byte[]>();
	private int size = 0;
	private int pointer = 0;

	/**
	 * Construct a new ByteStreamBuffer with blocksize 512 and 1 initial block
	 */
	public RandomAccessByteStream() {
		this(6000);
	}

	/**
	 * Construct a new ByteStreamBuffer with the specified blocksize and 1
	 * initial block
	 * 
	 * @param blocksize
	 *            The number of bytes to store in each block, choose a small
	 *            value for memory efficient processing of small files, the
	 *            larger the files the larger the blocksize can be.
	 */
	public RandomAccessByteStream(int blocksize) {
		this(blocksize, 1);
	}

	/**
	 * Construct a new ByteStreamBuffer with the specified blocksize and the
	 * specified number of initial blocks
	 * 
	 * @param blocksize
	 *            The number of bytes to store in each block, choose a small
	 *            value for memory efficient processing of small files, the
	 *            larger the files the larger the blocksize can be. Must be >1
	 * @param numblocks
	 *            Depending on the blocksize and the expected size of the file.
	 *            Must be >1
	 */
	public RandomAccessByteStream(int blocksize, int numblocks) {
		if (blocksize < 1)
			throw new IllegalArgumentException("Blocksize must be 1 or higher");
		if (numblocks < 1)
			throw new IllegalArgumentException("Numblocks must be 1 or higher");
		this.blocksize = blocksize;
		for (int i = 0; i < numblocks; i++)
			buffers.add(new byte[blocksize]);
	}

	public RandomAccessByteStream clone() {
		RandomAccessByteStream stream = new RandomAccessByteStream(this.blocksize, 1);
		stream.buffers.clear();
		for(byte[] buffer : buffers) {
			stream.buffers.add(buffer);
		}
		return stream;
	}
	
	/**
	 * Write a byte and move the pointer to the next position
	 * 
	 * @param b
	 *            the bye to write
	 */
	public void write(byte b) {
		int bufferNumber = pointer / blocksize;
		int localPointer = pointer - (bufferNumber * blocksize);
		while (buffers.size() <= bufferNumber) {
			buffers.add(new byte[blocksize]);
		}
		byte[] buffer = buffers.get(bufferNumber);
		buffer[localPointer] = b;
		pointer++;
		size = pointer > size ? pointer : size;
	}

	/**
	 * Write an array of bytes and move the pointer to the next position
	 * 
	 * @param b
	 *            the bye to write
	 */
	public void write(byte[] buf) {
		int bp = 0;
		while (bp < buf.length) {
			int bufferNumber = pointer / blocksize;
			while (buffers.size() <= bufferNumber) {
				buffers.add(new byte[blocksize]);
			}
			int localPointer = pointer - (bufferNumber * blocksize);
			int copy = (buf.length - bp > blocksize) ? blocksize - localPointer
					: (buf.length - bp);
			byte[] buffer = buffers.get(bufferNumber);
			for (int x = 0; x < copy; x++) {
				buffer[localPointer + x] = buf[bp + x];
			}
			pointer += copy;
			bp += copy;
		}
		size += buf.length;
	}

	public byte[] read(int len) {
		byte[] data = new byte[len];
		int read = 0;
		int bufferNumber = pointer / blocksize;
		int localPointer = pointer - (bufferNumber * blocksize);
		int toRead = len;
		while (toRead > 0) {
			if (toRead <= blocksize - localPointer) {
				byte[] buffer = buffers.get(bufferNumber);
				for (int i = 0; i < toRead; i++) {
					data[read] = buffer[localPointer + i];
					pointer++;
					read++;
				}
			} else {
				bufferNumber = pointer / blocksize;
				byte[] buffer = buffers.get(bufferNumber);
				for (int i = 0; i < blocksize; i++) {
					data[read] = buffer[localPointer + i];
					pointer++;
					read++;
				}
			}
			toRead = len - read;
		}
		return data;
	}

	/**
	 * Write an integer value and move the pointer 4 bytes.
	 * 
	 * @param val
	 *            The integer to write
	 */
	public void writeInt(int val) {
		write((byte) (val >>> 0));
		write((byte) (val >>> 8));
		write((byte) (val >>> 16));
		write((byte) (val >>> 24));
	}

	/**
	 * Write a double value and move the pointer 8 bytes.
	 * 
	 * @param val
	 *            The double to write
	 */
	public void writeDouble(double val) {
		long j = Double.doubleToLongBits(val);
		write((byte) (j >>> 0));
		write((byte) (j >>> 8));
		write((byte) (j >>> 16));
		write((byte) (j >>> 24));
		write((byte) (j >>> 32));
		write((byte) (j >>> 40));
		write((byte) (j >>> 48));
		write((byte) (j >>> 56));
	}

	/**
	 * Read a double from the current position and move the pointer 8 bytes
	 * 
	 * @return the double value
	 */
	public double readDouble() {
		byte d1 = read();
		byte d2 = read();
		byte d3 = read();
		byte d4 = read();
		byte d5 = read();
		byte d6 = read();
		byte d7 = read();
		byte d8 = read();
		long j = ((d1 & 0xFFL) << 0) + ((d2 & 0xFFL) << 8)
				+ ((d3 & 0xFFL) << 16) + ((d4 & 0xFFL) << 24)
				+ ((d5 & 0xFFL) << 32) + ((d6 & 0xFFL) << 40)
				+ ((d7 & 0xFFL) << 48) + (((long) d8) << 56);
		return Double.longBitsToDouble(j);
	}

	/**
	 * Write a boolean value. true is 1 false is 0, move the pointer 1 byte
	 * 
	 * @param val
	 *            the boolean
	 */
	public void writeBoolean(boolean val) {
		write((byte) (val ? 1 : 0));
	}

	/**
	 * Write a char value (UTF-16).Move the pointer 2 bytes.
	 * 
	 * @param val
	 *            the char
	 */
	public void writeChar(char val) {
		write((byte) (val >>> 0));
		write((byte) (val >>> 8));
	}

	/**
	 * Reads a UTF-16 char and moves the pointer 2 bytes.
	 * 
	 * @return the character
	 */
	public char readChar() {
		byte b1 = read();
		byte b2 = read();
		return (char) (((b1 & 0xFF) << 0) + ((b2) << 8));
	}

	/**
	 * Writes the size(bytes) and the content of the string to the buffer as
	 * UTF-16
	 * 
	 * @param val
	 */
	public void writeString(String val) {
		try {
			byte[] bytes = val.getBytes("UTF-16");
			writeInt(bytes.length);
			write(bytes);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public String readString() {
		int size = readInt();
		byte[] bytes = read(size);

		try {
			return new String(bytes, "UTF-16");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Read a byte and move the pointer 1 byte.
	 * 
	 * @return the byte
	 */
	public byte read() {
		int bufferNumber = pointer / blocksize;
		int localPointer = pointer - (bufferNumber * blocksize);
		byte[] buffer = buffers.get(bufferNumber);
		byte data = buffer[localPointer];
		pointer++;
		return data;

	}

	/**
	 * Relocate the current pointer.
	 * 
	 * @param pos
	 *            The new position. Pos must be < getSize()
	 */
	public void seek(int pos) {

		int bufferNumber = pos / blocksize;
		if (buffers.size() <= bufferNumber)
			throw new RuntimeException("Index out of bounds");
		this.pointer = pos;
	}

	/**
	 * Returns the current position of the pointer.
	 * 
	 * @return
	 */
	public int getPointerPosition() {
		return pointer;
	}

	/**
	 * Read a boolean value from the current position. Move pointer 1 byte.
	 * 
	 * @return the boolean value
	 */
	public boolean readBoolean() {
		byte b = read();
		return b != 0;
	}

	/**
	 * Read an integer value from the current position and move the pointer 4
	 * bytes.
	 * 
	 * @return the integer value
	 */
	public int readInt() {
		byte i1 = read();
		byte i2 = read();
		byte i3 = read();
		byte i4 = read();
		return ((i1 & 0xFF) << 0) + ((i2 & 0xFF) << 8) + ((i3 & 0xFF) << 16)
				+ ((i4) << 24);
	}

	/**
	 * Returns the current used size of the buffer. Allocated memory may be
	 * more.
	 * 
	 * @return the size
	 */
	public int size() {
		return size;
	}

	/**
	 * Write entire buffer to a outputstream
	 * 
	 * @param stream
	 * @throws IOException
	 */
	public void writeTo(OutputStream stream) throws IOException {
		int toWrite = size;
		int currentBlock = 0;
		while (toWrite > 0) {
			if (toWrite > blocksize) {
				byte[] buf = buffers.get(currentBlock);
				stream.write(buf, 0, buf.length);
				toWrite -= buf.length;
				printArray("Written: ", buf);
				currentBlock++;
			} else {
				byte[] buf = buffers.get(currentBlock);
				stream.write(buf, 0, toWrite);
				printArray("Written: ", buf, toWrite);
				toWrite -= buf.length;
			}
		}
		stream.flush();
	}
	
	/**
	 * Write entire buffer to a outputstream
	 * 
	 * @param stream
	 * @throws IOException
	 */
	public byte[] getBytes() {
		byte[] data = new byte[size];
		int pos = getPointerPosition();
		seek(0);
		for( int i=0;i<size;i++)
		{
			data[i] = read();
		}
		seek(pos);
		return data;
	}	

	private void printArray(String prefix, byte[] array) {
		printArray(prefix, array, array.length);
	}

	public static void printArray(String prefix, byte[] array, int size) {
		size = size > array.length ? array.length : size;
		System.out.print(prefix);
		for (int i = 0; i < size; i++) {
			String hex = array[i]+"";
			if (hex.length() == 1)
				hex = "0" + hex;
			System.out.print(hex + " ");
		}
		System.out.println();
	}

	/**
	 * Reads from the stream and resets size and pointer. Pointer will be 0,
	 * size the number of bytes read.
	 * 
	 * @param stream
	 *            The stream to read from.
	 * @throws IOException
	 */
	public void readFrom(InputStream stream) throws IOException {
		buffers.clear();
		int length = 0;
		pointer = 0;
		size = 0;
		byte[] buf = new byte[blocksize];
		while ((length = stream.read(buf)) > 0) {
			buffers.add(buf);
			printArray("Read: ", buf, length);
			buf = new byte[blocksize];
			size += length;
		}
	}

	public void add(byte value) {
		write( value );
	}

	public void set(int pos, int value) {
		int oldpos = getPointerPosition();
		seek(pos);
		writeInt(value);
		seek(oldpos);
	}

}
