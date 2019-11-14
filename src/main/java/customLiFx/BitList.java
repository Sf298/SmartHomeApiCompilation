/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package customLiFx;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author saud
 */
public class BitList implements List<Boolean> {
	
	private int size;
	private ArrayList<Integer> arr = new ArrayList<>();
	
	public BitList() {
		
	}
	public BitList(byte... arr) {
		for(int i=0; i<arr.length; i++) {
			byte b = arr[i];
			for(int j=0; j<Byte.SIZE; j++) {
				add(getBitInByte(b, j));
			}
		}
	}
	public BitList(Collection<? extends Boolean> c) {
		this();
		addAll(c);
	}
	public BitList(long l) {
		this(ByteBuffer.allocate(Long.BYTES).putLong(l).array());
	}
	public BitList(String s) {
		this(s.getBytes());
	}
	
	@Override
	public int size() {
		return size;
	}
	
	@Override
	public boolean isEmpty() {
		return size() == 0;
	}
	
	@Override
	public boolean contains(Object o) {
		return indexOf(o) > -1;
	}
	
	@Override
	public Iterator<Boolean> iterator() {
		return new Iterator<Boolean>() {
			private int pos = 0;
			
			@Override
			public boolean hasNext() {
				return pos < size;
			}

			@Override
			public Boolean next() {
				Boolean out = get(pos);
				pos++;
				return out;
			}
		};
	}
	
	
	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
	@Override
	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
	
	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean addAll(Collection<? extends Boolean> c) {
		for(Boolean b : c) {
			add(b);
		}
		return true;
	}
	
	@Override
	public boolean addAll(int index, Collection<? extends Boolean> c) {
		for(Boolean b : c) {
			add(index, b);
			index++;
		}
		return true;
	}
	
	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
	@Override
	public void clear() {
		size = 0;
		arr.clear();
	}
	
	@Override
	public Boolean get(int index) {
		int chunkIdx = toChunkIndex(index);
		int chunk = arr.get(chunkIdx);
		return getBitInInt(chunk, toIndexInChunk(index));
	}
	private static boolean getBitInInt(int i, int pos) {
		return (i & (1<<(Integer.SIZE-1-pos))) != 0;
	}
	private static boolean getBitInByte(byte b, int pos) {
		return (b & (1<<(Byte.SIZE-1-pos))) != 0;
	}
	
	@Override
	public Boolean set(int index, Boolean value) {
		Boolean out = get(index);
		
		int chunkIdx = toChunkIndex(index);
		int chunk = arr.get(chunkIdx);
		chunk = setBitInInt(chunk, toIndexInChunk(index), value);
		arr.set(chunkIdx, chunk);
		
		return out;
	}
	private static int setBitInInt(int i, int pos, boolean val) {
		int mask = 1<<(Integer.SIZE-1-pos);
		if(val) {
			i = i | mask;
		} else {
			i = i & ~mask;
		}
		return i;
	}
	private static int setBitInByte(byte b, int pos, boolean val) {
		int mask = 1<<(Byte.SIZE-1-pos);
		if(val) {
			b = (byte) (b | mask);
		} else {
			b = (byte) (b & ~mask);
		}
		return b;
	}
	
	
	@Override
	public boolean add(Boolean e) {
		add(size, e);
		return true;
	}
	
	@Override
	public void add(int index, Boolean element) {
		if(index < 0 || index > size)
			throw new IndexOutOfBoundsException();
		
		size++;
		if(size > arr.size()*8) arr.add(0);
		
		shiftTailRight(index, 1);
		set(index, element);
	}
	
	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
	@Override
	public Boolean remove(int index) {
		Boolean out = get(index);
		shiftTailLeft(index, 1);
		size--;
		return out;
	}
	public BitList remove(int fromIndex, int toIndex) {
		BitList out = subList(fromIndex, toIndex);
		shiftTailLeft(fromIndex, toIndex-fromIndex);
		size -= toIndex-fromIndex;
		return out;
	}
	public BitList removeTail(int bitCount) {
		if(bitCount <= size)
			return remove(size-bitCount, size);
		
		BitList out = new BitList(this);
		int steps = arr.size()*8 - bitCount;
		for(int i=0; i<steps; i++)
			out.add(false);
		out.shiftRight(steps);
		clear();
		return out;
	}
	public BitList removeHead(int bitCount) {
		if(bitCount <= size)
			return remove(0, bitCount);
		
		BitList out = new BitList(this);
		while(out.size < bitCount)
			out.add(false);
		clear();
		return out;
	}
	
	
	public byte[] asByteArr() {
		BitList temp = padLeft();
		byte[] out = new byte[temp.size()/8];
		for(int i = 0; i < out.length; i++) {
			out[i] = temp.subList(i*8, (i+1)*8).asByte();
		}
		return out;
	}
	public byte asByte() {
		byte out = 0;
		for(int i = 0; i < Byte.SIZE; i++) {
			out = (byte) (out << 1);
			if(get(i)) out = (byte) (out | 1);
		}
		return out;
	}
	public long asLong() {
		byte[] arr = asByteArr();
		long out = 0;
		for(int i=0; i<arr.length; i++) {
			out = out << 8;
			out = out | (0xFFl & arr[i]);
		}
		return out;
	}
	public String asString() {
		return new String(asByteArr());
	}
	
	
	@Override
	public int indexOf(Object o) {
		if(!(o instanceof Boolean)) return -1;
		Boolean b = (Boolean) o;
		for(int i=0; i<size; i++) {
			if(get(i).equals(b)) return i;
		}
		return -1;
	}
	
	@Override
	public int lastIndexOf(Object o) {
		if(!(o instanceof Boolean)) return -1;
		Boolean b = (Boolean) o;
		for(int i=size-1; i>=0; i--) {
			if(get(i).equals(b)) return i;
		}
		return -1;
	}
	
	@Override
	public ListIterator<Boolean> listIterator() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
	@Override
	public ListIterator<Boolean> listIterator(int index) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
	@Override
	public BitList subList(int fromIndex, int toIndex) {
		BitList out = new BitList();
		for(int i=fromIndex; i<toIndex; i++) {
			out.add(get(i));
		}
		return out;
	}
	public BitList sublistTail(int bitCount) {
		if(bitCount <= size)
			return subList(size-bitCount, size);
		
		BitList out = new BitList(this);
		int steps = arr.size()*8 - bitCount;
		for(int i=0; i<steps; i++)
			out.add(false);
		out.shiftRight(steps);
		return out;
	}
	
	public void shiftRight(int steps) {
		shiftTailRight(0, steps);
	}
	public void shiftLeft(int steps) {
		shiftTailLeft(0, steps);
	}
	public void shiftTailRight(int startPos, int steps) {
		for(int i=size-1; i>=startPos; i--) {
			if(i-steps < startPos)
				set(i, false);
			else
				set(i, get(i-steps));
		}
	}
	public void shiftTailLeft(int startPos, int steps) {
		for(int i=startPos; i<size; i++) {
			if(i+steps > size)
				set(i, false);
			else
				set(i, get(i+steps));
		}
	}
	public void shiftHeadLeft(int endPos, int steps) {
		for(int i=0; i<=endPos; i++) {
			if(i+steps > endPos)
				set(i, false);
			else
				set(i, get(i+steps));
		}
	}
	public void shiftHeadRight(int endPos, int steps) {
		for(int i=endPos; i>=0; i--) {
			if(i-steps < 0)
				set(i, false);
			else
				set(i, get(i-steps));
		}
	}
	
	public BitList reverseBytes() {
		byte[] arr = asByteArr();
		byte[] out = new byte[arr.length];
		for(int i=0; i<arr.length; i++) {
			out[arr.length-i-1] = arr[i];
		}
		return new BitList(out);
	}
	public BitList reverseBits() {
		BitList out = new BitList();
		for(int i=size-1; i>=0; i--) {
			out.add(get(i));
		}
		return out;
	}
	
	public BitList padLeft() {
		BitList out = new BitList(this);
		int steps = arr.size()*8 - size;
		for(int i=0; i<steps; i++)
			out.add(false);
		out.shiftRight(steps);
		return out;
	}
	
	private static int toChunkIndex(int idx) {
		return idx/Integer.SIZE;
	}
	private static int toIndexInChunk(int idx) {
		return idx%Integer.SIZE;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<size; i++) {
			if(i!=0 && i%8==0) sb.append(" ");
			sb.append(get(i) ? 1 : 0);
		}
		return sb.toString();
	}
	
}
