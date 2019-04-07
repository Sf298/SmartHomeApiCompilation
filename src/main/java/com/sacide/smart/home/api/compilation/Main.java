/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sacide.smart.home.api.compilation;

import LifxCommander.Messages.DataTypes.HSBK;

import com.sacide.smart.home.api.compilation.backend.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author saud
 */
public class Main {

    public static void main(String[] args) throws IOException {
        ArrayList<Device> devices = new ArrayList<>();
        
        LifxCommanderW lifx = new LifxCommanderW();
        devices.addAll(lifx.discoverDevices());
        
        PhilipsAPIV phil = new PhilipsAPIV();
        devices.addAll(phil.discoverDevices());
        
        System.out.println(devices.size());
        for (Device d : devices) {
            /*if(d instanceof RGBLightDevice) {
                RGBLightDevice rgb = (RGBLightDevice) d;
                rgb.setLightColor(HSBK.INCANDESCENT, 0);
                continue;
            }*/
            if(d instanceof LightDevice) {
                LightDevice ld = (LightDevice) d;
                if(!ld.label.contains("bed")) {
                    System.out.println("activating... "+ld);
                    ld.setLightPowerState(false, 0);
                    ld.setLightBrightness(0.47, 0);
                    System.out.println("bri="+ld.getLightBrightness());
                }
            }
        }
    }

    public static void testLifx() {
            try {
                    LifxCommanderW lifx = new LifxCommanderW();
                    lifx.discoverDevices();
                    RGBLightDevice ld = lifx.toRGBLightDevice(new Device("192.168.0.56", 56700));
                    ld.setLightPowerState(true, 0);

                    HSBK col;
                    double num = Math.random();
                    if (num < (1 / 4f)) {
                            col = HSBK.CRIMSON;
                            System.out.println("red");
                    } else if (num < (2 / 4f)) {
                            col = HSBK.INDIGO;
                            System.out.println("indigo");
                    } else if (num < (3 / 4f)) {
                            col = HSBK.FOREST_GREEN;
                            System.out.println("green");
                    } else {
                            col = HSBK.INCANDESCENT;
                            System.out.println("incan");
                    }
                    ld.setLightColor(HSBK.DAYLIGHT, 0);
                    ld.setLightBrightness(0.47, 0);
            } catch (IOException ex) {
                    Logger.getLogger(LifxCommanderW.class.getName()).log(Level.SEVERE, null, ex);
            }
    }

    public static void testPhilipsHue() {
            PhilipsAPIV ph = new PhilipsAPIV();
            Collection<Device> devices = ph.discoverDevices();

            System.out.println("num devices: " + devices.size());
            for (Device d : devices) {
                    System.out.println(d);
                    if (!(d instanceof LightDevice))
                            continue;
                    try {
                            LightDevice ld = (LightDevice) d;
                            ld.setLightPowerState(false, 0);
                            ld.setLightBrightness(0.47, 0);
                    } catch (IOException ex) {
                            Logger.getLogger(PhilipsAPIV.class.getName()).log(Level.SEVERE, null, ex);
                    }
            }
    }

	public static void testBlindsApi() {
		try {
			BlindsAPI api = new BlindsAPI();
			Collection<Device> devices = api.discoverDevices();
			for (Device dev : devices) {
				BlindsDevice blindDev = (BlindsDevice) dev;
				
				int angle = blindDev.getAngle();
				System.out.println("Current angle before increment of 10 : " + angle);
				blindDev.setAngle(angle + 1);
				System.out.println("Current angle after increment of 10 : " + blindDev.getAngle());
			}

		} catch (IOException ex) {
			Logger.getLogger(PhilipsAPIV.class.getName()).log(Level.SEVERE, null, ex);
		}

	}
	
	public static void testTPLink() throws IOException {
		TPLinkAPI api = new TPLinkAPI();
		Collection<Device> devices = api.discoverDevices();
		for (Device dev : devices) {
			OnOffDevice onOffDev = (OnOffDevice) dev;
			System.out.println("ip:" + onOffDev.ip_id + ", port=" +onOffDev.port+ ", label=" + onOffDev.label);
			boolean power = onOffDev.getPowerState();
			System.out.println("Current power  : " + power);
			
			//onOffDev.setPowerState(false);
			//Thread.sleep(2000);
			//onOffDev.setPowerState(true);
		}

	}

}
