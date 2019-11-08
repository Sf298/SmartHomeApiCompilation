/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sacide.smart.home.api.compilation;

import com.sacide.smart.home.api.compilation.backend.IActionAPI;
import com.sacide.smart.home.api.compilation.backend.LightDevice;
import com.sacide.smart.home.api.compilation.backend.Device;
import com.sacide.smart.home.api.compilation.backend.RGBLightDevice;
import com.sacide.smart.home.api.compilation.backend.utils.NetUtils;

import LifxCommander.Messages.DataTypes.HSBK;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.security.sasl.AuthenticationException;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.json.*;
import sauds.toolbox.propsFile.PropertiesFile;

/**
 *
 * @author saud
 */
public final class PhilipsAPIV implements IActionAPI {
    
    private final String DISCOVERY_URL = "https://discovery.meethue.com/";
    private HashMap<String, BridgeObj> allBridges;                   //*
    private HashMap<String, BridgeObj> onlineBridges;
    private final PropertiesFile saveFile; // username, selected bridge mac
    
    /**
     * Creates a PhilipsAPIV Object.
     */
    public PhilipsAPIV() {
        this(new File("./philips.prop"));
    }
    
    /**
     * Creates a PhilipsAPIV Object.
     * @param persistenceFile The file in which to store the username and preferred bridge mac. 
     */
    public PhilipsAPIV(File persistenceFile) {
        saveFile = new PropertiesFile(persistenceFile);
        if(saveFile.fileExists()) {
            load();
        } else {
            allBridges = new HashMap<>();
            discoverBridges();
            save();
        }
    }
    
    
    @Override
    public Collection<Device> discoverDevices() {
        discoverBridges();
        HashSet<Device> out = new HashSet<>();
        out.addAll(discoverAllLights());
        return out;
    }
    
    /**
     * Updates cache of bridges.
     */
    public void discoverBridges() {
        try {
            String str = NetUtils.getFromHttps(new URL(DISCOVERY_URL));
            JSONArray objs = new JSONArray(str);
            onlineBridges = new HashMap<>();
            for(int i=0; i<objs.length(); i++) {
                BridgeObj temp = new BridgeObj(objs.getJSONObject(i));
                if(allBridges.containsKey(temp.id)) {
                    BridgeObj old = allBridges.get(temp.id);
                    old.ip = temp.ip;
                    onlineBridges.put(old.id, old);
                } else {
                    allBridges.put(temp.id, temp);
                    onlineBridges.put(temp.id, temp);
                }
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(PhilipsAPIV.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PhilipsAPIV.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Gets a list of available lights from all available bridges.
     * @return returns all the lights
     */
    public HashSet<LightDevice> discoverAllLights() {
        HashSet<LightDevice> out = new HashSet<>();
        for(Map.Entry<String, BridgeObj> entry : onlineBridges.entrySet()) {
            BridgeObj bridge = entry.getValue();
            if(bridge.uname == null) continue;
            
            try {
                String resp = NetUtils.makeAPIRequest(parseURL(bridge, "/api/<uname>/lights"), "GET", null);
				checkResponse(resp);
                if(!NetUtils.isJSONObj(resp)) continue;
                JSONObject lights = new JSONObject(resp);
                for(String lightID : lights.keySet()) {
                    JSONObject light = lights.getJSONObject(lightID);
                    boolean reachable = light.getJSONObject("state").getBoolean("reachable");
                    if(reachable) {
                        Device d = new Device(bridge.id, Integer.parseInt(lightID));
                        d.setLabel(light.getString("name"));
                        out.add(toLightDevice(d));
                    }
                }
            } catch (AuthenticationException ex) {
                bridge.uname = null;
            } catch (IOException ex) {
                Logger.getLogger(PhilipsAPIV.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return out;
    }

    /**
     * Attempts to log in to all discovered bridges
     */
    public void tryAuth() {
        for (Map.Entry<String, BridgeObj> entry : onlineBridges.entrySet()) {
            BridgeObj val = entry.getValue();
            if(val.uname != null) continue;
            try {
				String resp = NetUtils.makeAPIRequest(parseURL(val, "/api"), "POST",
                        NetUtils.properties2body("\"devicetype\"", "\"EverythingBridge#EBUser\""));
				checkResponse(resp);
                JSONArray response = new JSONArray(resp);
                JSONObject reponse2 = response.getJSONObject(0);
                if(reponse2.has("success"))
                    val.uname = reponse2.getJSONObject("success").getString("username");
                else
                    throw new AuthenticationException("link button not pressed");
            } catch (AuthenticationException ex) {
            } catch (IOException ex) {
                Logger.getLogger(PhilipsAPIV.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
	
	private void checkResponse(String resp) throws AuthenticationException {
		if(!resp.contains("Device is set to off") && resp.contains("error")) {
			throw new AuthenticationException(resp);
		}
	}
    
    
   
    
    /**
     * 
     * @param b
     * @param context eg "/api/<uname>/lights/<deviceId>/state"
     * @return
     * @throws MalformedURLException 
     */
    private URL parseURL(BridgeObj b, String context) throws MalformedURLException {
        context = context.replace("<uname>", b.uname);
        return new URL("http://"+b.ip+context);
    }
    /**
     * 
     * @param d
     * @param context eg "/api/<uname>/lights/<deviceId>/state"
     * @return
     * @throws MalformedURLException 
     */
    private URL parseURL(Device d, String context) throws MalformedURLException {
        BridgeObj bridge = onlineBridges.get(d.getIp_id());
        context = context.replace("<uname>", bridge.uname);
        context = context.replace("<deviceId>", d.getPort()+"");
        if(!context.startsWith("/")) context = "/"+context;
        return new URL("http://" + bridge.ip + context);
    }
    
    
    
    /**
     * Shows a UI for editing the settings.
     * @param parentWindow the parent window
     */
    @Deprecated
	public void show(Window parentWindow) {
        discoverBridges();
        
        JPanel mainPanel = new JPanel(new BorderLayout());
            
            mainPanel.add(new JLabel("Press the button on the bridges you wish to authenticate with then click 'Auth'"));
            JButton unameButton = new JButton("Auth");
            unameButton.addActionListener((ActionEvent e) -> {
                discoverBridges();
                tryAuth();
            });
            mainPanel.add(unameButton);
            
        
        final JComponent[] inputs = new JComponent[] {mainPanel};
        JOptionPane.showConfirmDialog(parentWindow, inputs, "Philips API Config", JOptionPane.PLAIN_MESSAGE);
        saveFile.save();
    }
    
    public void save() {
        for(BridgeObj val : allBridges.values()) {
            saveFile.put(val.id, val.uname);
        }
        saveFile.save();
    }
    public void load() {
        saveFile.load();
        allBridges = new HashMap<>();
        
        for (Map.Entry<String, String> entry : saveFile.getMap().entrySet()) {
            String id = entry.getKey();
            String uname = entry.getValue();
            
            BridgeObj b = new BridgeObj(null, id);
            b.uname = uname;
            allBridges.put(id, b);
        }
    }
    
    
    
    public LightDevice toLightDevice(Device d) {
        return new LightDevice(d) {
			@Override
			public void setPowerState(boolean on) throws IOException {
				setLightPowerState(on, 0);
			}
			@Override
			public boolean getPowerState() throws IOException {
				return getLightPowerState();
			}
            @Override
            public void setLightPowerState(boolean on, long duration) throws IOException {
            	NetUtils.makeAPIRequest(parseURL(this, "/api/<uname>/lights/<deviceId>/state"), "PUT",
                        NetUtils.properties2body(
                                "\"on\"", on+"",
                                "\"transitiontime\"", (duration/100)+""));
            }
            @Override
            public boolean getLightPowerState() throws IOException {
                String response = NetUtils.makeAPIRequest(parseURL(this, "/api/<uname>/lights/<deviceId>"), "GET", null);
				checkResponse(response);
                JSONObject resp = new JSONObject(response);
                return resp.getJSONObject("state").getBoolean("on");
            }
            @Override
            public void setLightBrightness(double brightness, long duration) throws IOException {
            	NetUtils.makeAPIRequest(parseURL(this, "/api/<uname>/lights/<deviceId>/state"), "PUT",
                        NetUtils.properties2body("\"bri\"", ( (int)(brightness*253)+1 )+""));
            }
            @Override
            public double getLightBrightness() throws IOException {
                String response = NetUtils.makeAPIRequest(parseURL(this, "/api/<uname>/lights/<deviceId>"), "GET", null);
				checkResponse(response);
                JSONObject state = new JSONObject(response).getJSONObject("state");
                return (state.getInt("bri")-1)/253.0;
            }
        };
    }
    public RGBLightDevice toRGBLightDevice(Device d) {
        return new RGBLightDevice(d) {
			@Override
			public void setPowerState(boolean on) throws IOException {
				setLightPowerState(on, 0);
			}
			@Override
			public boolean getPowerState() throws IOException {
				return getLightPowerState();
			}
            @Override
            public void setLightPowerState(boolean on, long duration) throws IOException {
            	NetUtils.makeAPIRequest(parseURL(this, "/api/<uname>/lights/<deviceId>/state"), "PUT",
                        NetUtils.properties2body(
                                "\"on\"", on+"",
                                "\"transitiontime\"", (duration/100)+""));
            }
            @Override
            public boolean getLightPowerState() throws IOException {
                String response = NetUtils.makeAPIRequest(parseURL(this, "/api/<uname>/lights/<deviceId>"), "GET", null);
				checkResponse(response);
                JSONObject resp = new JSONObject(response);
                return resp.getJSONObject("state").getBoolean("on");
            }
            @Override
            public void setLightColor(HSBK hsbk, long duration) throws IOException {
                if(hsbk.hasEmpty())
                    hsbk.updateEmptyWith(getLightColor());

                NetUtils.makeAPIRequest(parseURL(this, "/api/<uname>/lights/<deviceId>/state"), "PUT",
                        NetUtils.properties2body(
                                "\"hue\"", hsbk.getHue(65535)+"",
                                "\"sat\"", hsbk.getSaturation(254)+"",
                                "\"bri\"", (hsbk.getBrightness(253)+1)+"" ));
            }
            @Override
            public void setLightBrightness(double brightness, long duration) throws IOException {
                setLightColor(new HSBK(-1, -1, (int) (HSBK.MAX_BRI*brightness), -1), duration);
            }
            @Override
            public HSBK getLightColor() throws IOException {
                String response = NetUtils.makeAPIRequest(parseURL(this, "/api/<uname>/lights/<deviceId>"), "GET", null);
				checkResponse(response);
                JSONObject state = new JSONObject(response).getJSONObject("state");
                HSBK out = new HSBK();
                out.setHue(state.getInt("hue"), 65535);
                out.setSaturation(state.getInt("sat"), 254);
                out.setBrightness(state.getInt("bri")-1, 253);
                return out;
            }
            @Override
            public double getLightBrightness() throws IOException {
                return getLightColor().getBrightness()/HSBK.MAX_BRI;
            }
        };
    }
    
    
    
    
    private class BridgeObj {
        public String ip;
        public final String id;
        public String uname = null;
        public BridgeObj(String ip, String id) {
            this.ip = ip;
            this.id = id;
        }
        public BridgeObj(JSONObject obj) {
            this.id = obj.getString("id");
            this.ip = obj.getString("internalipaddress");
        }
    }
}
