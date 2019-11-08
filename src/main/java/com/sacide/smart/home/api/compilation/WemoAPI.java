/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sacide.smart.home.api.compilation;

import com.sacide.smart.home.api.compilation.backend.Device;
import com.sacide.smart.home.api.compilation.backend.IActionAPI;
import com.sacide.smart.home.api.compilation.backend.OnOffDevice;
import com.sacide.smart.home.api.compilation.backend.utils.NetUtils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * source: https://objectpartners.com/2014/03/25/a-groovy-time-with-upnp-and-wemo/
 * @author saud
 */
public class WemoAPI implements IActionAPI {

    @Override
    public Collection<Device> discoverDevices() {
		MulticastSocket socket = null;
		try {
			InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName("239.255.255.250"), 1900);
			socket = new MulticastSocket(null);
			socket.bind(new InetSocketAddress("192.168.0.155", 1901));
			StringBuilder packet = new StringBuilder();
			packet.append("M-SEARCH * HTTP/1.1\r\n" );
			packet.append("HOST: 239.255.255.250:1900\r\n" );
			packet.append("MAN: \"ssdp:discover\"\r\n" );
			packet.append("MX: ").append( "5" ).append( "\r\n" );
			packet.append("ST: " ).append( "ssdp:all" ).append( "\r\n" ).append( "\r\n" );
			packet.append( "ST: " ).append( "urn:Belkin:device:controllee:1" ).append( "\r\n" ).append( "\r\n" );
			byte[] data = packet.toString().getBytes();
			socket.send(new DatagramPacket(data, data.length, socketAddress));
		} catch (IOException ex) {
			Logger.getLogger(WemoAPI.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			if(socket != null) {
			socket.disconnect();
			socket.close();
			}
		}
		
		// GET ALL RESPONSES
		ArrayList<String> responses = new ArrayList<>();
		try {
			MulticastSocket recSocket = new MulticastSocket(null);
			recSocket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 1901));
			recSocket.setTimeToLive(10);
			recSocket.setSoTimeout(1000);
			recSocket.joinGroup(InetAddress.getByName("239.255.255.250"));
			long endTime = System.currentTimeMillis() + recSocket.getSoTimeout()+1;
			while (System.currentTimeMillis() < endTime) {
				byte[] buf = new byte[2048];
				DatagramPacket input = new DatagramPacket(buf, buf.length);
				try {
					recSocket.receive(input);
					String originaldata = new String(input.getData());
					responses.add(originaldata);
				} catch (SocketTimeoutException e) {
					Logger.getLogger(WemoAPI.class.getName()).log(Level.SEVERE, null, e);
				}
			}
			recSocket.disconnect();
			recSocket.close();
		} catch (IOException ex) {
			Logger.getLogger(WemoAPI.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		// GET LOCATIONS FROM RESPONSES
		HashSet<String> responseLocations = new HashSet<>();
		for(String resp : responses) {
			ArrayList<String> matches = getMatching("LOCATION: http:\\/\\/[0-9:.]+\\/.+", resp);
			for(String match : matches) {
				responseLocations.add(match.substring(10));
			}
		}
		
		// FILTER RESPONSES BY LOCATION CONTENT
		HashMap<String, String> respContent = new HashMap<>();
		for(String loc : responseLocations) {
			try {
				String page = NetUtils.readURL(new URL(loc));
				if(page.contains("Belkin Plugin Socket 1.0")) {
					respContent.put(loc, page);
				}
			} catch (IOException ex) {
				System.out.println(loc);
				Logger.getLogger(WemoAPI.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		
		// TO DEVICES
		ArrayList<Device> devices = new ArrayList<>();
		for (Map.Entry<String, String> entry : respContent.entrySet()) {
			String loc = entry.getKey(), page = entry.getValue();
			
			String friendlyName = getFirstMatching("<friendlyName>.+<\\/friendlyName>", page);
			if(friendlyName == null) continue;
			friendlyName = friendlyName.substring(14, friendlyName.length()-15);

			String ip = getFirstMatching("\\/[0-9.]+", loc);
			if(ip == null) continue;
			ip = ip.substring(1);
			
			String portStr = getFirstMatching(":[0-9]+", loc);
			int port = Integer.parseInt(portStr.substring(1));
			
			System.out.println(friendlyName+"("+ip+":"+port+")");
			devices.add(toOnOffDevice(new Device(ip, port, friendlyName)));
		}
		return devices;
    }
	
	public OnOffDevice toOnOffDevice(Device d) {
		return new OnOffDevice(d) {
			@Override
			public void setPowerState(boolean bool) throws IOException {
				
			}
			
			@Override
			public boolean getPowerState() throws IOException {
				URL url = new URL("http://"+ip_id+":"+port+"/upnp/control/basicevent1");
				String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
							"<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
							"  <s:Body>\n" +
							"    <u:GetBinaryState xmlns:u=\"urn:Belkin:service:basicevent:1\"></u:GetBinaryState>\n" +
							"  </s:Body>\n" +
							"</s:Envelope>";	
				String response = NetUtils.makeAPIRequest(url, "GET", body, 
								"SOAPACTION", "\"urn:Belkin:service:basicevent:1#GetBinaryState\"",
								"Content-Type", "text/xml; charset=\"utf-8\""
						);
				System.out.println(response);
				return false;
			}
		};
	}
	
	HashMap<String, Pattern> patterns = new HashMap<>();
	private String getFirstMatching(String pattern, String str) {
		ArrayList<String> out = getMatching(pattern, str);
		if(out.isEmpty())
			return null;
		return out.get(0);
	}
	private ArrayList<String> getMatching(String pattern, String str) {
		Pattern p = patterns.get(pattern);
		if(p == null) {
			p = Pattern.compile(pattern);
			patterns.put(str, p);
		}
		
		Matcher matcher = p.matcher(str);
		ArrayList<String> out = new ArrayList<>();
		while(matcher.find()) {
			out.add(matcher.group());
		}
		return out;
	}
	
}
