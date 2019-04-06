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
public abstract class BlindsDevice extends Device {
    
    public BlindsDevice(String ip, int port, String label) {
        super(ip, port, label);
    }
    public BlindsDevice(String ip, int port) {
        super(ip, port);
    }
    public BlindsDevice(Device d) {
        super(d);
    }
    
    public abstract void setAngle(int degrees) throws IOException;
    
    public abstract int getAngle() throws IOException;
    
}
