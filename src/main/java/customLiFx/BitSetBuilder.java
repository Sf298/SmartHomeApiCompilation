/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package customLiFx;

/**
 *
 * @author saud
 */
public class BitSetBuilder {
	
	private byte[] bytes;
	private int sizeAdded = 0;

	public BitSetBuilder(int sizeInBytes) {
		this.bytes = new byte[sizeInBytes];
	}

	public BitSetBuilder(byte[] arr) {
		this.bytes = arr;
	}
	
	/**
	 * 
	 * @param size the number of bits from the end of the value
	 * @param val the value
	 */
	public void append(int size, long val) {
		val = (val << (Long.SIZE-size));
		for(int i=0; i<size; i++) {
			append(val < 0);
			val = val << 1;
		}
	}
	public void append(boolean val) {
		set(sizeAdded, val);
	}
	public void set(int bit, boolean val) {
		sizeAdded = Math.max(sizeAdded, bit+1);
		byte wB = bytes[bit/8];
		bytes[bit/8] = setBitInByte(wB, bit%8, val);
	}
	public boolean get(int bit) {
		byte wB = bytes[bit/8];
		return (wB & (1<<(7-bit%8))) > 0;
	}
	private static byte setBitInByte(byte b, int pos, boolean val) {
		if(val) {
			b = (byte) (b | (1<<(7-pos)));
		} else {
			b = (byte) (b & ~(1<<(7-pos)));
		}
		return b;
	}
	
	public byte[] getBytes() {
		return bytes;
	}
	
	public long getBits(int startI, int size) {
		long out = 0;
		for(int i=startI; i<startI+size; i++) {
			boolean currBit = get(i);
			out = out << 1;
			if(currBit) out = out | 1;
		}
		return out;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<bytes.length; i++) {
			sb.append(byte2str(bytes[i])).append(" ");
		}
		return sb.toString();
	}
	
	public static String byte2str(byte b) {
		String out = Integer.toBinaryString(b & 0xFF);
		while(out.length() < 8) {
			out = "0"+out;
		}
		return out;
	}
	public static String byte2hex(byte b) {
		String out = Integer.toHexString(b & 0xFF);
		while(out.length() < 2) {
			out = "0"+out;
		}
		return out;
	}
	public static String byte2str(int b) {
		return byte2str((byte)b);
	}
	public static String long2str(long l) {
		String out = Long.toBinaryString(l);
		while(out.length() < Long.SIZE) {
			out = "0"+out;
		}
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<out.length(); i++) {
			sb.append(out.charAt(i));
			if(i % 8 == 7) sb.append(" ");
		}
		return sb.toString();
	}
	
	public static String bytearr2hex(byte[] arr) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<arr.length; i++) {
			sb.append(byte2hex(arr[i])).append(" ");
		}
		return sb.toString();
	}
	public static String bytearr2str(byte[] arr) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<arr.length; i++) {
			sb.append(byte2str(arr[i])).append(" ");
		}
		return sb.toString();
	}
	
}
