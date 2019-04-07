package com.sacide.smart.home.api.compilation;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sacide.smart.home.api.compilation.backend.Device;
import com.sacide.smart.home.api.compilation.backend.IActionAPI;
import com.sacide.smart.home.api.compilation.backend.OnOffDevice;
import com.sacide.smart.home.api.compilation.backend.utils.NetUtils;
import com.sacide.smart.home.api.compilation.backend.utils.NetUtils.ResponseChecker;
 
/**
 * TP-Link HS100
 *
 * @author Insxnity
 * @copyright Copyright (c) 2016, Insxnity Development
 */
public class TPLinkAPI implements IActionAPI {
 
    public static final String COMMAND_SWITCH_ON = "{\"system\":{\"set_relay_state\":{\"state\":1}}}}";
    public static final String COMMAND_SWITCH_OFF = "{\"system\":{\"set_relay_state\":{\"state\":0}}}}";
    public static final String COMMAND_INFO = "{\"system\":{\"get_sysinfo\":null}}";
 
    
    
    @Override
	public Collection<Device> discoverDevices() {
    	try {
			ArrayList<Device> devices = NetUtils.getAvaiableDevicesByIP(new ResponseChecker() {
			
				
				@Override
				public Device checkResponse(String host) throws IOException {
					URL url = ipToURL(host);
					String output = NetUtils.makeAPIRequest(url,"GET");
					if(!output.contains("..."))
						return null;		
					return toOnOffDevice(new Device(host, 9999));
				}
			});
			for (Device device: devices) {
				device.label = getName(device);
			}
			return devices;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
 
    public OnOffDevice toOnOffDevice(Device d) {
        return new OnOffDevice(d) {

			@Override
			public void setPowerState(boolean bool) throws IOException{
				if(bool) {
					try {
						if(switchOn(this) == false) {
							throw new Exception("Unable to turn on");
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					try {
						if(switchOff(this) == false) {
							throw new Exception("Unable to turn off");
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
			}

			@Override
			public boolean getPowerState() throws IOException{
				return isOn(this);
			}
 
        };
    }
    
    /**
     * send "On" signal to plug
     *
     * @return true if successful 
     */
    private boolean switchOn(Device dev) throws IOException {
 
        String jsonData = sendCommand(dev, COMMAND_SWITCH_ON);
        if(jsonData.length() > 0) {
 
            JsonObject jo = new JsonParser().parse(jsonData).getAsJsonObject();
            int errorCode = jo.get("system").getAsJsonObject().get("set_relay_state").getAsJsonObject().get("err_code").getAsInt();
            return errorCode == 0;
        }
        return false;
    }
 
    /**
     * send "Off" signal to plug
     *
     * @return true if successful
     */
    private boolean switchOff(Device dev) throws IOException {
 
        String jsonData = sendCommand(dev, COMMAND_SWITCH_OFF);
        if(jsonData.length() > 0) {
 
            JsonObject jo = new JsonParser().parse(jsonData).getAsJsonObject();
            int errorCode = jo.get("system").getAsJsonObject().get("set_relay_state").getAsJsonObject().get("err_code").getAsInt();
            return errorCode == 0;
        }
        return false;
    }
 
    /**
     * check if the plug is on
     *
     * @return STATE_ON oder STATE_OFF
     */
    private boolean isOn(Device dev) throws IOException {
 
        String jsonData = sendCommand(dev, COMMAND_INFO);
        if(jsonData.length() > 0) {
            JsonObject jo = new JsonParser().parse(jsonData).getAsJsonObject();
            int state = jo.get("system").getAsJsonObject().get("get_sysinfo").getAsJsonObject().get("relay_state").getAsInt();
            return state == 1 ? true : false;
        }
        return false;
    }
 
    /**
     * check name of plug
     *
     * @return name of plug
     */
    private String getName(Device dev) throws IOException {
    	String name = null;
        String jsonData = sendCommand(dev, COMMAND_INFO);
        if(jsonData.length() > 0) {
            JsonObject jo = new JsonParser().parse(jsonData).getAsJsonObject();
            name = jo.get("system").getAsJsonObject().get("get_sysinfo").getAsJsonObject().get("alias").getAsString();
        }
        return name;
    }
    
    /**
     * Return a map containing plug system information
     *
     * @return Map of Information
     */
    private Map<String, String> getInfo(Device dev) throws IOException {
 
        Map<String, String> result = new HashMap<>();
        String jsonData = sendCommand(dev, COMMAND_INFO);
        if(jsonData.length() > 0) {
 
            JsonObject jo = new JsonParser().parse(jsonData).getAsJsonObject();
            JsonObject systemInfo = jo.get("system").getAsJsonObject().get("get_sysinfo").getAsJsonObject();
            for(Map.Entry<String, JsonElement> entry : systemInfo.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return result;
    }
 
    /**
     * send <code>command</code> to plug
     *
     * @param command Command
     * @return Json String of the returned data
     * @throws IOException
     */
    protected String sendCommand(Device dev, String command) throws IOException {
 
        Socket socket = new Socket(dev.ip_id, dev.port);
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(encryptWithHeader(command));
 
        InputStream inputStream = socket.getInputStream();
        String data = decrypt(inputStream);
 
        outputStream.close();
        inputStream.close();
        socket.close();
 
        return data;
    }
    
    
    /**
     * Decrypt given data from InputStream
     *  
     * @param inputStream
     * @return
     * @throws IOException
     */
    private String decrypt(InputStream inputStream) throws IOException {
 
        int in;
        int key = 0x2B;
        int nextKey;
        StringBuilder sb = new StringBuilder();
        while((in = inputStream.read()) != -1) {
 
            nextKey = in;
            in = in ^ key;
            key = nextKey;
            sb.append((char) in);
        }
        return "{" + sb.toString().substring(5);
    }
    /**
     * Encrypt a command into plug-readable bytecode
     * 
     * @param command
     * @return
     */
    private int[] encrypt(String command) {
 
        int[] buffer = new int[command.length()];
        int key = 0xAB;
        for(int i = 0; i < command.length(); i++) {
 
            buffer[i] = command.charAt(i) ^ key;
            key = buffer[i];
        }
        return buffer;
    }
    /**
     * Encrypt a command into plug-readable bytecode with header
     * 
     * @param command
     * @return
     */
    private byte[] encryptWithHeader(String command) {
 
        int[] data = encrypt(command);
        byte[] bufferHeader = ByteBuffer.allocate(4).putInt(command.length()).array();
        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferHeader.length + data.length).put(bufferHeader);
        for(int in : data) {
 
            byteBuffer.put((byte) in);
        }
        return byteBuffer.array();
    }

    
    private URL ipToURL(String ip) throws MalformedURLException {
		 return new URL("http://" + ip);
	 }
	
}
