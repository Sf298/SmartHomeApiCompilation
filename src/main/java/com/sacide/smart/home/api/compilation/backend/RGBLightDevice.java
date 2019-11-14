/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sacide.smart.home.api.compilation.backend;

import java.io.IOException;

/**
 *
 * @author saud
 */
public abstract class RGBLightDevice extends LightDevice {
    
    public RGBLightDevice(String ip, int port, String label) {
        super(ip, port, label);
    }

    public RGBLightDevice(String ip, int port) {
        super(ip, port);
    }

    public RGBLightDevice(Device d) {
        super(d);
    }
    
    public abstract void setLightColor(HSVK hsvk, int duration) throws IOException;
    
    public abstract HSVK getLightColor() throws IOException;
    
}
