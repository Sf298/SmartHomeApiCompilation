/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sacide.smart.home.api.compilation;

import com.sacide.smart.home.api.compilation.backend.Device;
import com.sacide.smart.home.api.compilation.backend.HSVK;
import com.sacide.smart.home.api.compilation.backend.IActionAPI;
import com.sacide.smart.home.api.compilation.backend.LightDevice;
import com.sacide.smart.home.api.compilation.backend.RGBLightDevice;
import com.sacide.smart.home.api.compilation.backend.utils.NetUtils;
import customLiFx.BitSetBuilder;
import customLiFx.PacketBuilder;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author saud
 */
public class LifxAPI implements IActionAPI {
	
	public DatagramSocket socket = null;
	
	@Override
	public Collection<Device> discoverDevices() {
		ArrayList<Device> out = new ArrayList<>();
		try {
			if(socket == null) socket = new DatagramSocket(56700);
			sendUDP(socket, getService(), NetUtils.getSubnet()+"255", 56700);
			
			
			PacketBuilder stateService = stateService();
			ArrayList<DatagramPacket> packets = waitAllUDP(socket, 1000, stateService.byteSize());
			for(DatagramPacket dp : packets) {
				stateService.parse(dp.getData());
				if(stateService.getValueLong("service") != 1)
					continue;
				
				String ip = dp.getAddress().getHostAddress();
				int port = dp.getPort();
				
				sendUDP(socket, getLabel(), ip, port);
				PacketBuilder stateLabel = stateLabel();
				DatagramPacket labelResp = waitUDP(socket, 56700, stateLabel.byteSize());
				stateLabel.parse(labelResp.getData());
				String label = stateLabel.getValueStrRev("label");
				
				
				Device d = new Device(ip, port, label);
				out.add(d);
			}
		} catch(Exception ex) {
			Logger.getLogger(LifxAPI.class.getName()).log(Level.SEVERE, null, ex);
		}
		return out;
	}
	
	public RGBLightDevice toRGBLightDevice(Device d) {
		return new RGBLightDevice(d) {
			@Override
			public void setLightPowerState(boolean on, int duration) throws IOException {
				sendUDP(socket, setPower(on, duration), getIp_id(), getPort());
			}

			@Override
			public boolean getLightPowerState() throws IOException {
				sendUDP(socket, getPower(), getIp_id(), getPort());
				PacketBuilder statePower = waitUDP(socket, 1000, statePower());
				return statePower.getValueLong("level") != 0;
			}

			@Override
			public void setLightBrightness(double brightness, int duration) throws IOException {
				throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
			}

			@Override
			public double getLightBrightness() throws IOException {
				throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
			}

			@Override
			public void setPowerState(boolean bool) throws IOException {
				throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
			}

			@Override
			public boolean getPowerState() throws IOException {
				throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
			}

			@Override
			public void setLightColor(HSVK hsvk, int duration) throws IOException {
				if(hsvk.hasNullValue())
					hsvk.fillNullWith(getLightColor());
				sendUDP(socket, setColor(hsvk, duration), getIp_id(), getPort());
			}

			@Override
			public HSVK getLightColor() throws IOException {
				sendUDP(socket, get(), getIp_id(), getPort());
				PacketBuilder pb = waitUDP(socket, 1000, state());
				long val = pb.getValueLong("color");
				return HSVK.parseKVSH16(val);
			}
			
		};
	}
	
	
	public static PacketBuilder getService() {
		PacketBuilder pb = new PacketBuilder(PacketBuilder.LITTLE);
		pb.append(packetFrame(0, true, 0));
		pb.append(packetFrameAddress(0, false, false, (byte)3));
		pb.append(packetProtocolHeader((short)2));
		pb.setValue("size", pb.byteSize());
		return pb;
	}
	public static PacketBuilder stateService() {
		PacketBuilder pb = new PacketBuilder(PacketBuilder.LITTLE);
		pb.append(packetFrame(0, true, 0));
		pb.append(packetFrameAddress(0, false, false, (byte)0));
		pb.append(packetProtocolHeader((short)3));
		
		pb.addField("service", 8, 10);
		pb.addField("port", 32, 11);
		
		pb.setValue("size", pb.byteSize());
		return pb;
	}
	
	public static PacketBuilder getLabel() {
		PacketBuilder pb = new PacketBuilder(PacketBuilder.LITTLE);
		pb.append(packetFrame(0, false, 0));
		pb.append(packetFrameAddress(0, false, false, (byte)0));
		pb.append(packetProtocolHeader((short)23));
		
		pb.setValue("size", pb.byteSize());
		return pb;
	}
	public static PacketBuilder stateLabel() {
		PacketBuilder pb = new PacketBuilder(PacketBuilder.LITTLE);
		pb.append(packetFrame(0, false, 0));
		pb.append(packetFrameAddress(0, true, false, (byte)0));
		pb.append(packetProtocolHeader((short)24));
		
		pb.addField("label", 32*8, 10);
		
		pb.setValue("size", pb.byteSize());
		return pb;
	}
	
	public static PacketBuilder getPower() {
		PacketBuilder pb = new PacketBuilder(PacketBuilder.LITTLE);
		pb.append(packetFrame(0, false, 0));
		pb.append(packetFrameAddress(0, false, false, (byte)0));
		pb.append(packetProtocolHeader((short)116));
		
		pb.setValue("size", pb.byteSize());
		return pb;
	}
	public static PacketBuilder setPower(boolean onOff, int durationMS) {
		PacketBuilder pb = new PacketBuilder(PacketBuilder.LITTLE);
		pb.append(packetFrame(0, false, 0));
		pb.append(packetFrameAddress(0, false, false, (byte)0));
		pb.append(packetProtocolHeader((short)117));
		
		pb.addField("level", 16, onOff?65535:0, 10);
		pb.addField("duration", 32, durationMS, 11);
		
		pb.setValue("size", pb.byteSize());
		return pb;
	}
	public static PacketBuilder statePower() {
		PacketBuilder pb = new PacketBuilder(PacketBuilder.LITTLE);
		pb.append(packetFrame(0, false, 0));
		pb.append(packetFrameAddress(0, true, false, (byte)0));
		pb.append(packetProtocolHeader((short)118));
		
		pb.addField("level", 16, 0);
		
		pb.setValue("size", pb.byteSize());
		return pb;
	}
	
	public static PacketBuilder get() {
		PacketBuilder pb = new PacketBuilder(PacketBuilder.LITTLE);
		pb.append(packetFrame(0, false, 0));
		pb.append(packetFrameAddress(0, false, false, (byte)0));
		pb.append(packetProtocolHeader((short)101));
		
		pb.setValue("size", pb.byteSize());
		return pb;
	}
	public static PacketBuilder setColor(HSVK hsvk, int durationMS) {
		PacketBuilder pb = new PacketBuilder(PacketBuilder.LITTLE);
		pb.append(packetFrame(0, false, 0));
		pb.append(packetFrameAddress(0, false, false, (byte)0));
		pb.append(packetProtocolHeader((short)102));
		
		pb.addField("reserved",  8, 0,               10);
		pb.addField("color",    64, hsvk.asKVSH16(), 11);
		pb.addField("duration", 32, durationMS,      12);
		
		pb.setValue("size", pb.byteSize());
		return pb;
	}
	public static PacketBuilder state() {
		PacketBuilder pb = new PacketBuilder(PacketBuilder.LITTLE);
		pb.append(packetFrame(0, false, 0));
		pb.append(packetFrameAddress(0, true, false, (byte)0));
		pb.append(packetProtocolHeader((short)107));
		
		pb.addField("color",    64, 10);
		pb.addField("reserved", 16, 11);
		pb.addField("power",    16, 12);
		pb.addField("label",  32*8, 13);
		pb.addField("reserved", 64, 14);
		
		pb.setValue("size", pb.byteSize());
		return pb;
	}
	
	
	public static PacketBuilder packetFrame(int size, boolean tagged, int source) {
		PacketBuilder out = new PacketBuilder(PacketBuilder.LITTLE);
		out.addField("size",       16,       size, 1);
		out.addField("protocol",   12,       1024, 2);
		out.addField("addressable", 1,          1, 2);
		out.addField("tagged",      1, tagged?1:0, 2);
		out.addField("origin",      2,          0, 2);
		out.addField("source",     32,     source, 3);
		return out;
	}
	public static PacketBuilder packetFrameAddress(long target, boolean ackReq, boolean respReq, byte seq) {
		PacketBuilder out = new PacketBuilder(PacketBuilder.LITTLE);
		out.addField("target",      64,  target,     4);
		out.addField("reserved",    48,       0,     5);
		out.addField("res_required", 1, respReq?1:0, 5);
		out.addField("ack_required", 1,  ackReq?1:0, 5);
		out.addField("reserved",     8,       0,     5);
		out.addField("sequence",     6,     seq,     5);
		return out;
	}
	public static PacketBuilder packetProtocolHeader(short type) {
		PacketBuilder out = new PacketBuilder(PacketBuilder.LITTLE);
		out.addField("reserved",     64,    0,  6);
		out.addField("type", 16, type,  7);
		out.addField("reserved",     16,    0,  8);
		return out;
	}
	
	public static void sendUDP(DatagramSocket socket, byte[] data, String address, int port) throws IOException {
		socket.setBroadcast(true);
		DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(address), port);
		socket.send(packet);
		socket.setBroadcast(false);
	}
	public static void sendUDP(DatagramSocket socket, PacketBuilder data, String address, int port) throws IOException {
		sendUDP(socket, data.compile(), address, port);
	}
	public static DatagramPacket waitUDP(DatagramSocket socket, int timeoutMS, int packetSize) throws SocketException, IOException {
		socket.setSoTimeout(timeoutMS);
		DatagramPacket packet = new DatagramPacket(new byte[packetSize], packetSize);
		try {
			do {
				socket.receive(packet);
			} while(packet.getAddress().equals(InetAddress.getLocalHost()));
		} catch(SocketTimeoutException ex) {
			packet = null;
		}
		return packet;
	}
	public static PacketBuilder waitUDP(DatagramSocket socket, int timeoutMS, PacketBuilder pb) throws SocketException, IOException {
		DatagramPacket dp = waitUDP(socket, timeoutMS, pb.byteSize());
		PacketBuilder out = new PacketBuilder(pb);
		out.parse(dp.getData());
		return out;
	}
	public static ArrayList<DatagramPacket> waitAllUDP(DatagramSocket socket, int timeoutMS, int packetSize) throws SocketException, IOException {
		socket.setSoTimeout(timeoutMS);
		ArrayList<DatagramPacket> out = new ArrayList<>();
		while(true) {
			try {
				DatagramPacket packet = new DatagramPacket(new byte[packetSize], packetSize);
				socket.receive(packet);
				if(!packet.getAddress().equals(InetAddress.getLocalHost()))
					out.add(packet);
			} catch(SocketTimeoutException ex) {
				return out;
			}
		}
	}
	
	
	public static void test1() {
		System.out.println("expected: 31 00 00 34 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 66 00 00 00 00 55 55 ff ff ff ff ac 0d 00 04 00 00");
		
		PacketBuilder pb = new PacketBuilder(PacketBuilder.LITTLE);
		pb.append(packetFrame(0, true, 0));
		pb.append(packetFrameAddress(0, false, false, (byte)0));
		pb.append(packetProtocolHeader((short)102));
		
		HSVK green = new HSVK(1/3.0, 1.0, 1.0, (3500-2500)/6500.0);
		pb.addField("reserved",  8, 0,                10);
		pb.addField("color",    64, green.asKVSH16(), 11);
		pb.addField("duration", 32, 1024,             12);
		
		pb.setValue("size", pb.byteSize());
		System.out.println("actual 1: "+BitSetBuilder.bytearr2hex(pb.compile()));
		pb.parse(new byte[pb.compile().length]);
		System.out.println("actual 2: "+BitSetBuilder.bytearr2hex(pb.compile()));
	}
	public static void test2() throws Exception {
		System.out.println(getService().byteSize());
		System.out.println(stateService().byteSize());
		System.out.println(getService());
	}
	public static void test3() throws Exception {
		DatagramSocket socket = new DatagramSocket(56700);
		sendUDP(socket, getService(), NetUtils.getSubnet()+"255", 56700);
		
		
		PacketBuilder stateService = LifxAPI.stateService();
		DatagramPacket dp = waitUDP(socket, 10000, stateService.byteSize());
		System.out.println(dp.getAddress().getHostAddress()+"\n"+dp.getData().length+" - "+Arrays.toString(dp.getData()));
		stateService.parse(dp.getData());
		System.out.println(stateService);
		
		dp = waitUDP(socket, 10000, stateService.byteSize());
		System.out.println(dp.getAddress().getHostAddress()+"\n"+dp.getData().length+" - "+Arrays.toString(dp.getData()));
		stateService.parse(dp.getData());
		System.out.println(stateService);
	}
	public static void test4() throws Exception {
		DatagramSocket socket = new DatagramSocket(56700);
		sendUDP(socket, getService(), NetUtils.getSubnet()+"255", 56700);
		System.out.println(BitSetBuilder.bytearr2hex(getService().compile()));
		
		PacketBuilder stateService = LifxAPI.stateService();
		ArrayList<DatagramPacket> packets = waitAllUDP(socket, 1000, stateService.byteSize());
		for(DatagramPacket dp : packets) {
			System.out.println(dp.getAddress().getHostAddress()+":"+dp.getPort()+" - "+dp.getData().length);
			stateService.parse(dp.getData());
			System.out.println(stateService);
		}
	}
	
}
