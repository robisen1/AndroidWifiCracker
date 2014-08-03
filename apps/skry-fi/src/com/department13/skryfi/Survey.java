package com.department13.skryfi;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.net.wifi.ScanResult;

public class Survey {
	public long timestamp;
	public String bssid;
	public String ssid;
	public double latitude;
	public double longitude;
	public double altitude;
	public int level;
	public int channel;
	public double frequency;
	
	public static enum EncryptionType{ OPEN, WEP, WPA, WPA2 };
	public EncryptionType encryption = EncryptionType.OPEN;
	
	public static enum CipherType { NONE, UNKNOWN, WEP40, WEP104, CCMP, WRAP, TKIP };
	public CipherType cipher = CipherType.NONE;
	
	public static enum AuthType { NONE, UNKNOWN, PSK, SKA, MGT, OPEN };
	public AuthType authentication = AuthType.UNKNOWN;
	// human readable version of the security features 
	public String security = "";
	
	public Survey(Cursor cursor) 
	{
		// TODO Auto-generated constructor stub
	}
	
	public Survey(String bssid, String ssid, double freq, int level, String sec)
	{
		this.timestamp = System.currentTimeMillis();
		this.bssid = bssid;
		this.ssid = ssid;
		this.frequency = freq;
		this.level = level;
		this.channel = FrequencyToChannel(this.frequency);
		
		this.security = sec;
		if (security.contains("WEP"))
			encryption = EncryptionType.WEP;
		else if (security.contains("WPA2"))
			encryption = EncryptionType.WPA2;
		else if (security.contains("WPA"))
			encryption = EncryptionType.WPA;
		else if ((security.contains("OPEN")) || (security.contains("OPN")))
			encryption = EncryptionType.OPEN;
	}
	
	public Survey(ScanResult result)
	{
		this.timestamp = System.currentTimeMillis();
		this.bssid = result.BSSID;
		this.ssid = result.SSID;
		this.level = result.level;
		this.frequency = result.frequency;
		this.channel = FrequencyToChannel(this.frequency);
		this.security = result.capabilities.toUpperCase();
		
		if (security.contains("WEP"))
			encryption = EncryptionType.WEP;
		else if (security.contains("WPA2"))
			encryption = EncryptionType.WPA2;
		else if (security.contains("WPA"))
			encryption = EncryptionType.WPA;
		else if ((security.contains("OPEN")) || (security.contains("OPN")))
			encryption = EncryptionType.OPEN;
	}
	
	public Survey(JSONObject obj) throws JSONException
	{
		this.timestamp = System.currentTimeMillis();
		this.bssid = obj.getString("mac");
		this.ssid = obj.getString("essid");
		this.level = obj.getInt("power");
		this.channel = obj.getInt("channel");
		this.frequency = ChannelToFrequency(this.channel);
		String priv = obj.getString("privacy");
		String cip = obj.getString("cipher");
		String auth = obj.getString("auth");
		this.security = priv + ", " + cip + ", " + auth;
		
		if (priv.equalsIgnoreCase("WEP"))
			encryption = EncryptionType.WEP;
		else if (priv.equalsIgnoreCase("WPA"))
			encryption = EncryptionType.WPA;
		else if (priv.equalsIgnoreCase("WPA2"))
			encryption = EncryptionType.WPA2;
		else if ((priv.equalsIgnoreCase("OPEN"))||(priv.equalsIgnoreCase("OPN")))
			encryption = EncryptionType.OPEN;
		
		if ((cip.equalsIgnoreCase("WEP"))||(cip.equalsIgnoreCase("WEP104")))
			cipher = CipherType.WEP104;
		else if (cip.equalsIgnoreCase("WEP40"))
			cipher = CipherType.WEP40;
		else if (cip.equalsIgnoreCase("CCMP"))
			cipher = CipherType.CCMP;
		else if (cip.equalsIgnoreCase("TKIP"))
			cipher = CipherType.TKIP;
		else if (cip.equalsIgnoreCase("WRAP"))
			cipher = CipherType.WRAP;
		
		if ((cip.equalsIgnoreCase("OPN"))||(cip.equalsIgnoreCase("OPEN")))
			authentication = AuthType.OPEN;
		else if (cip.equalsIgnoreCase("MGT"))
			authentication = AuthType.MGT;
		else if (cip.equalsIgnoreCase("SKA"))
			authentication = AuthType.SKA;
		else if (cip.equalsIgnoreCase("PSK"))
			authentication = AuthType.PSK;
	}


	public void updatePosition(double lat, double lng, double alt)
	{
		latitude = lat;
		longitude = lng;
		altitude = alt;
	}
	
	static public int FrequencyToChannel(double freq)
	{
		double delta = freq - 2.407;
		return (int)(delta/0.005);
	}
	
	static public double ChannelToFrequency(int channel)
	{
		return ((double)channel * 0.005) + 2.407;
	}
}
