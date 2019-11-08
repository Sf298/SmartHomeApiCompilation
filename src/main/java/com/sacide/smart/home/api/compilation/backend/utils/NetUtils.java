package com.sacide.smart.home.api.compilation.backend.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.security.sasl.AuthenticationException;

import com.sacide.smart.home.api.compilation.PhilipsAPIV;
import com.sacide.smart.home.api.compilation.backend.Device;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.xml.bind.DatatypeConverter;

public class NetUtils {

	private static final long REQUEST_FREQ_TIME = 100;
	private static long nextRequestTime = 0;

	/**
	 * Sends a request and receives a response.
	 *
	 * @param url The url to send the request to.
	 * @param method The HTTP method to use.
	 * @return The String response as received from the bridge. Refer to the Hue
	 * API.
	 * @throws javax.security.sasl.AuthenticationException
	 * @throws IOException if an I/O error occurs.
	 */
	public static String makeAPIRequest(URL url, String method) throws AuthenticationException, IOException {
		return makeAPIRequest(url, method, null);
	}

	/**
	 * Sends a request and receives a response.
	 *
	 * @param url The url to send the request to.
	 * @param method The HTTP method to use.
	 * @param body The body to be sent in the message.
	 * @return The String response as received from the bridge. Refer to the Hue
	 * API.
	 * @throws javax.security.sasl.AuthenticationException
	 * @throws IOException If an I/O error occurs.
	 */
	public static String makeAPIRequest(URL url, String method, String body)
			throws AuthenticationException, IOException {
		String[] t = null;
		return makeAPIRequest(url, method, body, t);
	}
	
	/**
	 * Sends a request and receives a response.
	 *
	 * @param url The url to send the request to.
	 * @param method The HTTP method to use.
	 * @param body The body to be sent in the message.
	 * @param headders
	 * @return The String response as received from the bridge. Refer to the Hue
	 * API.
	 * @throws javax.security.sasl.AuthenticationException
	 * @throws IOException If an I/O error occurs.
	 */
	public static String makeAPIRequest(URL url, String method, String body, String... headders)
			throws AuthenticationException, IOException {
		if(System.currentTimeMillis() < nextRequestTime) {
			try {
				Thread.sleep(nextRequestTime - System.currentTimeMillis());
			} catch(InterruptedException ex) {
				Logger.getLogger(PhilipsAPIV.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(method.toUpperCase());
		conn.setDoOutput(true);
		conn.setConnectTimeout(200);
		if(headders != null) {
			for(int i=0; i<headders.length/2; i+=2) {
				//String encoding0 = Base64.getEncoder().encodeToString(headders[i].getBytes(StandardCharsets.UTF_8));
				//String encoding1 = Base64.getEncoder().encodeToString(headders[i+1].getBytes(StandardCharsets.UTF_8));
				conn.setRequestProperty(headders[i], headders[i+1]);
			}
		}
		if(body != null && body.length() > 0) {
			conn.setRequestProperty("Content-Length", Integer.toString(body.length()));

			OutputStream os = conn.getOutputStream();
			os.write(body.getBytes("UTF8"));
			os.flush();
			os.close();
		}
		
		Scanner in = new Scanner(conn.getInputStream());
		StringBuilder sb = new StringBuilder();
		while(in.hasNext()) {
			sb.append(in.nextLine()).append("\n");
		}
		in.close();
		nextRequestTime = System.currentTimeMillis() + REQUEST_FREQ_TIME;
		String resp = sb.toString();

		return resp;
	}

	/**
	 * Filters addresses that respond by the response content.
	 *
	 * @param rc the filter to use.
	 * @return returns the filtered list of IP addresses that are alive.
	 * @throws Exception
	 */
	public static ArrayList<Device> getAvaiableDevicesByIP(ResponseChecker rc) throws Exception {
		String localIp = InetAddress.getLocalHost().getHostAddress();
		String subnet = localIp.substring(0, localIp.lastIndexOf('.')) + ".";

		List<Device> found = Collections.synchronizedList(new ArrayList<Device>());
		ArrayList<Thread> threads = new ArrayList<>();
		for(int i = 1; i < 255; i++) {
			String host = subnet + i;

			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						InetAddress adress = InetAddress.getByName(host);
						if(adress.isReachable(1000)) {
							Device device = rc.checkResponse(host);
							if(device != null) {
								found.add(device);
							}
						}
					} catch(IOException e) {
						// Ignore error and dont add associated ip to list
					}
				}
			});
			t.start();
			threads.add(t);
		}

		for(int i = 0; i < threads.size(); i++) {
			threads.get(i).join();
		}

		return new ArrayList<>(found);
	}

	public static String properties2body(String... properties) {
		StringBuilder body = new StringBuilder("{");
		for(int i = 0; i < properties.length; i += 2) {
			body = body.append(properties[i]).append(": ").append(properties[i + 1]).append(",");
		}
		return body.deleteCharAt(body.length() - 1).append("}").toString();
	}

	public static boolean isJSONArr(String json) {
		int arrInd = json.indexOf("[");
		if(arrInd == -1) {
			return false;
		}
		int objInd = json.indexOf("{");
		if(objInd == -1) {
			return false;
		}
		return arrInd < objInd;
	}

	public static boolean isJSONObj(String json) {
		int objInd = json.indexOf("{");
		if(objInd == -1) {
			return false;
		}
		int arrInd = json.indexOf("[");
		if(arrInd == -1) {
			return true;
		}
		return objInd < arrInd;
	}

	public interface ResponseChecker {
		public Device checkResponse(String host) throws IOException;
	}
	
	/**
	 * Simple get String from URL.
	 * @param url URL to access
	 * @return The String content from the URL
	 * @throws IOException 
	 */
	public static String readURL(URL url) throws IOException {
		IOException e = new IOException();
		for(int i=0; i<3; i++) {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				String inputLine;
				StringBuilder sb = new StringBuilder();
				while ((inputLine = in.readLine()) != null)
					sb.append(inputLine).append("\n");
				in.close();
				return sb.toString();
			} catch(IOException ex) {
				e = ex;
			}
		}
		throw e;
	}
	
	/**
	 * Performs a get request on the provided HTTPS url.
	 * @param url The HTTPS url
	 * @return The String response from the url
	 * @throws IOException
	 */
	public static String getFromHttps(URL url) throws IOException {
		HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
		BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

		StringBuilder sb = new StringBuilder();
		String input;
		while ((input = br.readLine()) != null) {
			sb.append(input).append("\n");
		}
		br.close();

		return sb.toString();
	}
    
}
