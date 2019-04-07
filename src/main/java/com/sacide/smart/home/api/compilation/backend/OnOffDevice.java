package com.sacide.smart.home.api.compilation.backend;

import java.io.IOException;

public abstract class OnOffDevice extends Device{

	public OnOffDevice(String ip, int port, String label) {
        super(ip, port, label);
    }
    public OnOffDevice(String ip, int port) {
        super(ip, port);
    }
    public OnOffDevice(Device d) {
        super(d);
    }
    
    public abstract void setPowerState(boolean bool) throws IOException;
    
    public abstract boolean getPowerState() throws IOException;

}
