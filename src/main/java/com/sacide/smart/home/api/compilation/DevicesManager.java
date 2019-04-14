/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sacide.smart.home.api.compilation;

import com.sacide.smart.home.api.compilation.backend.Device;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 *
 * @author saud
 */
public class DevicesManager {
	
	private int nextID = 0;
	private HashMap<Integer, Device> devices = new HashMap<>();
	
	public DevicesManager() {
		
	}
	
	public Map<Integer, Device> getDeviceMap() {
		return new HashMap<>(devices);
	}
	
	public Collection<Device> getDevices() {
		return devices.values();
	}
	
	/**
	 * Scans network for all available devices. Old devices keep their ID's
	 */
	public void scanDevices() {
		HashSet<Device> newDevices = new HashSet<>();
		
		LifxCommanderW lifx = new LifxCommanderW();
		newDevices.addAll(lifx.discoverDevices());
		
		BlindsAPI blinds = new BlindsAPI();
		newDevices.addAll(blinds.discoverDevices());
		
		PhilipsAPIV hue = new PhilipsAPIV(new File("./philips.prop"));
		newDevices.addAll(hue.discoverDevices());
		
		TPLinkAPI tplink = new TPLinkAPI();
		newDevices.addAll(tplink.discoverDevices());
		
		HashMap<Integer, Device> oldDevicesMap = new HashMap<>(devices);
		HashSet<Device> oldDevicesSet = new HashSet<>(devices.values());
		for(Device d : newDevices) {
			if(!oldDevicesSet.contains(d)) {
				devices.put(nextID++, d);
			}
		}
		for(Map.Entry<Integer, Device> entry : oldDevicesMap.entrySet()) {
			Integer key = entry.getKey();
			Device value = entry.getValue();
			if(!newDevices.contains(value)) {
				devices.remove(key);
			}
		}
		
	}
	
	/**
	 * Gets a device object from its ID.
	 * @param id the ID.
	 * @return the requested device object or null if not found.
	 */
	public Device getDevice(int id) {
		return devices.get(id);
	}
	
	/**
	 * Gets a device's ID from its object.
	 * @param d the device.
	 * @return the requested device ID or -1 if not found.
	 */
	public int getID(Device d) {
		for(Map.Entry<Integer, Device> entry : devices.entrySet()) {
			Device value = entry.getValue();
			if(value.equals(d)) {
				return entry.getKey();
			}
		}
		return -1;
	}
	
}
