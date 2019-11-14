/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package customLiFx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

/**
 *
 * @author saud
 */
public class PacketBuilder {
	
	public static final int LITTLE = 0;
	public static final int BIG = 1;
	
	protected final ArrayList<String> fieldNames;
	protected final ArrayList<Integer> fieldSize;
	protected final ArrayList<BitList> fieldVals;
	protected final ArrayList<Integer> fieldRevGroups;
	protected final int endianness;

	public PacketBuilder(int endianness) {
		this.fieldNames = new ArrayList<>();
		this.fieldSize = new ArrayList<>();
		this.fieldVals = new ArrayList<>();
		this.fieldRevGroups = new ArrayList<>();
		this.endianness = endianness;
	}

	public PacketBuilder(PacketBuilder pb) {
		this.fieldNames = new ArrayList<>(pb.fieldNames);
		this.fieldSize = new ArrayList<>(pb.fieldSize);
		this.fieldVals = new ArrayList<>(pb.fieldVals);
		this.fieldRevGroups = new ArrayList<>(pb.fieldRevGroups);
		this.endianness = pb.endianness;
	}
	
	public void addField(String name, int size, int reverseByteGroup) {
		addField(name, size, 0, reverseByteGroup);
	}
	public void addField(String name, int size, long value, int reverseByteGroup) {
		fieldNames.add(name);
		fieldSize.add(size);
		fieldVals.add(new BitList(value).sublistTail(size));
		fieldRevGroups.add(reverseByteGroup);
	}
	
	public void setValue(String fieldName, long value) {
		int idx = fieldNames.indexOf(fieldName);
		if(idx == -1) throw new RuntimeException("Field not found");
		setValue(idx, value);
	}
	public void setValue(int fieldIndex, long value) {
		fieldVals.set(fieldIndex, new BitList(value).sublistTail(fieldSize.get(fieldIndex)));
	}
	
	public long getValueLong(String fieldName) {
		int idx = fieldNames.indexOf(fieldName);
		if(idx == -1) throw new RuntimeException("Field not found");
		return getValueLong(idx);
	}
	public long getValueLong(int fieldIndex) {
		return fieldVals.get(fieldIndex).asLong();
	}
	public String getValueStr(String fieldName) {
		int idx = fieldNames.indexOf(fieldName);
		if(idx == -1) throw new RuntimeException("Field not found");
		return getValueStr(idx);
	}
	public String getValueStr(int fieldIndex) {
		return fieldVals.get(fieldIndex).asString();
	}
	public String getValueStrRev(String fieldName) {
		int idx = fieldNames.indexOf(fieldName);
		if(idx == -1) throw new RuntimeException("Field not found");
		return getValueStrRev(idx);
	}
	public String getValueStrRev(int fieldIndex) {
		return fieldVals.get(fieldIndex).reverseBytes().asString();
	}
	
	public int bitSize() {
		int sum = 0;
		for(Integer s : fieldSize) {
			sum += s;
		}
		return sum;
	}
	public int byteSize() {
		return bitSize()/8;
	}
	
	public int bitIdxToField(int bit) {
		int sum = 0;
		for(int i = 0; i < fieldSize.size(); i++) {
			sum += fieldSize.get(i);
			if(bit < sum) return i;
		}
		return -1;
	}
	
	public byte[] compile() {
		BitList out = new BitList();
		
		if(endianness == LITTLE) {
			ArrayList<Integer> groupIDs = getUniqueGroups();
			for(Integer id : groupIDs) {
				ArrayList<Integer> idxs = getIdxsOfGroup(id);
				BitList groupBl = new BitList();

				// add bytes in reverse group order
				for(int i=idxs.size()-1; i>=0; i--) {
					groupBl.addAll(fieldVals.get(idxs.get(i)));
				}

				// reverse bytes
				groupBl = groupBl.reverseBytes();
				out.addAll(groupBl);
			}
		} else {
			for(int i=0; i<fieldVals.size(); i++) {
				out.addAll(fieldVals.get(i));
			}
		}
		
		return out.asByteArr();
	}
	public void parse(byte[] msg) {
		if(msg.length != byteSize())
			throw new RuntimeException("Packet sizes do not match");
		
		BitList msgL = new BitList(msg);
		if(endianness==LITTLE) {
			ArrayList<Integer> groupIDs = getUniqueGroups();
			for(Integer id : groupIDs) {
				BitList groupL = msgL.removeHead(getGroupSize(id));
				groupL = groupL.reverseBytes();
				
				ArrayList<Integer> idxs = getIdxsOfGroup(id);
				for(int idx : idxs) {
					//System.out.println(groupL);
					fieldVals.set(idx, groupL.removeTail(fieldSize.get(idx)));
				}
			}
			
		} else {
			for(int i=fieldVals.size()-1; i>=0; i--) {
				fieldVals.set(i, msgL.removeTail(fieldSize.get(i)));
			}
		}
		
		/*if(endianness==LITTLE) msg = reverseByGroup(msg);
		
		BitList bl = new BitList(msg);
		int sum = 0;
		for(int i=0; i<fieldSize.size(); i++) {
			int size = fieldSize.get(i);
			fieldVals.set(i, bl.subList(sum, sum+size));
			sum += size;
		}*/
	}
	protected ArrayList<Integer> getUniqueGroups() {
		ArrayList<Integer> out = new ArrayList<>();
		for(int i=0; i<fieldRevGroups.size(); i++) {
			int groupID = fieldRevGroups.get(i);
			if(!out.contains(groupID)) out.add(groupID);
		}
		return out;
	}
	protected int getGroupSize(int id) {
		ArrayList<Integer> idxs = getIdxsOfGroup(id);
		int sum = 0;
		for(Integer idx : idxs) {
			sum += fieldSize.get(idx);
		}
		return sum;
	}
	protected ArrayList<Integer> getIdxsOfGroup(int groupID) {
		ArrayList<Integer> out = new ArrayList<>();
		for(int i=0; i<fieldRevGroups.size(); i++) {
			if(fieldRevGroups.get(i) == groupID)
				out.add(i);
		}
		return out;
	}
	
	protected byte[] reverseByGroup(byte[] arr) {
		byte[] out = new byte[arr.length];
		ArrayList<Byte> groupBytes = new ArrayList<>();
		int currGroup = -1;
		for(int i=0; i<arr.length; i++) {
			int tempGroup = fieldRevGroups.get(bitIdxToField(i*8));
			if(tempGroup != currGroup) {
				insertBytes(out, i-groupBytes.size(), groupBytes);
				groupBytes.clear();
				currGroup = tempGroup;
			}
			groupBytes.add(0, arr[i]);
		}
		insertBytes(out, arr.length-groupBytes.size(), groupBytes);
		return out;
	}
	protected void insertBytes(byte[] arr, int startPos, ArrayList<Byte> toInsert) {
		if(toInsert.isEmpty()) return;
		for(int i=0; i<toInsert.size(); i++) {
			arr[i+startPos] = toInsert.get(i);
		}
	}
	
	public void append(PacketBuilder pb) {
		fieldNames.addAll(pb.fieldNames);
		fieldSize.addAll(pb.fieldSize);
		fieldVals.addAll(pb.fieldVals);
		fieldRevGroups.addAll(pb.fieldRevGroups);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for(int i=0; i<fieldNames.size(); i++) {
			BitList bl = fieldVals.get(i);
			String value = (bl.size()>64) ? bl.asString() : bl.asLong()+"";
			sb.append(fieldNames.get(i)).append("=").append(value).append(", ");
		}
		return sb.substring(0, sb.length()-2) + "}";
	}
	
}
