package com.sacide.smart.home.api.compilation;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONObject;

import com.sacide.smart.home.api.compilation.backend.BlindsDevice;
import com.sacide.smart.home.api.compilation.backend.Device;
import com.sacide.smart.home.api.compilation.backend.IActionAPI;
import com.sacide.smart.home.api.compilation.backend.utils.NetUtils;
import com.sacide.smart.home.api.compilation.backend.utils.NetUtils.ResponseChecker;

public class BlindsAPI implements IActionAPI {


	@Override
	public Collection<Device> discoverDevices() {
		try {
			ArrayList<Device> devices = NetUtils.getAvaiableDevicesByIP(new ResponseChecker() {
			
				
				@Override
				public Device checkResponse(String host) throws IOException {
					URL url = new URL("http://" + host + "/devices");
					String output = NetUtils.makeAPIRequest(url,"GET");
					if(!output.contains("id"))
						return null;		
					return new Device(host, 80, new JSONObject(output).getString("id"));
				}
			});
			
			return devices;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	 public BlindsDevice toBlindDevice(Device d) {
	        return new BlindsDevice(d) {

				@Override
				public void setAngle(int degrees) throws IOException {
					URL url = new URL(ip_id);
					String body = NetUtils.properties2body("\"id\"", "2", "\"angle\"", String.valueOf(degrees));
					NetUtils.makeAPIRequest(url,"PUT", body);
				}

				@Override
				public int getAngle() throws IOException {
					URL url = new URL(ip_id);
					JSONObject json = new JSONObject(NetUtils.makeAPIRequest(url,"GET"));
					return json.getInt("angle");
				}
	            
	        };
	 }
}
