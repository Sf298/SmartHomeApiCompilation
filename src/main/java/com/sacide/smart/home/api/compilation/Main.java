/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sacide.smart.home.api.compilation;

import LifxCommander.Messages.DataTypes.HSBK;
import com.sacide.smart.home.api.compilation.backend.*;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author saud
 */
public class Main {
    
    public static void main(String[] args) {
    	testFroodleSpinner();
    }
    
    public static void testLifx() {
        try {
            LifxCommanderW lifx = new LifxCommanderW();
            lifx.discoverDevices();
            RGBLightDevice ld = lifx.toRGBLightDevice(new Device("192.168.0.56", 56700));
            ld.setLightPowerState(true, 0);
            
            
            HSBK col;
            double num = Math.random();
            if(num<(1/4f)) {
                col = HSBK.CRIMSON;
                System.out.println("red");
            } else if(num<(2/4f)) {
                col = HSBK.INDIGO;
                System.out.println("indigo");
            } else if(num<(3/4f)) {
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
        
        System.out.println("num devices: "+devices.size());
        for(Device d : devices) {
            System.out.println(d);
            if(!(d instanceof LightDevice)) continue;
            try {
                LightDevice ld = (LightDevice) d;
                ld.setLightPowerState(false, 0);
                ld.setLightBrightness(0.47, 0);
            } catch (IOException ex) {
                Logger.getLogger(PhilipsAPIV.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static void testFroodleSpinner() { 
    	BlindsAPI api = new BlindsAPI();
    	Collection<Device> devices = api.discoverDevices();
    }
    
}
