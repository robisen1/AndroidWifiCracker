package com.department13.skryfi;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.alexd.jsonrpc.JSONRPCClient;
import org.alexd.jsonrpc.JSONRPCException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.util.Log;

public class Cracker {
	protected Network network;
	protected int ivs;
	protected long started_at;
	
	private static Cracker instance = null;
	protected CrackerListener listener = null;
	protected JSONRPCClient client = null;	
	protected Timer timer = null;
	static protected String LOG_TAG = "com.department13.skryfi";
	
	public static Cracker getInstance()
	{
		if (instance == null)
		{
			instance = new Cracker();
		}
		return instance;
	}
	
	public Cracker()
	{
		client = JSONRPCClient.create("http://localhost:8000");
	}
	
	public synchronized boolean isAvailable()
	{
		try {
			Log.d(LOG_TAG, "calling isCracking to test if rpc service available");
			client.callBoolean("isCracking");
		} catch (JSONRPCException e) {
			Log.d(LOG_TAG, "isAvaible... " + e.getMessage());
			return false;
		}
		return true;
	}
	
	public synchronized void startSurvey()
	{
		try {
			Log.d(LOG_TAG, "calling startSurvey");
			client.call("startSurvey");
		} catch (JSONRPCException e) {
			Log.d(LOG_TAG, "startSurvey " + e.getMessage());
		}
	}
	
	public synchronized List<Survey> getSurveyResults()
	{
		JSONArray jarry = null;
		List<Survey> results = new Vector<Survey>();
		try {
			jarry = client.callJSONArray("getNetworks");
			Log.d(LOG_TAG, "received " + jarry.length() + " results");
			for (int i=0; i<jarry.length(); ++i)
			{
				JSONObject obj = jarry.getJSONObject(i);
				results.add(new Survey(obj));
			}
			Log.d(LOG_TAG, "parsed " + results.size() + " results");
		} catch (JSONRPCException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(LOG_TAG, e.getMessage());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(LOG_TAG, e.getMessage());
		}
		
		return results;		
	}
	
	public synchronized void stopSurvey()
	{
		try {
			client.call("stopSurvey");
		} catch (JSONRPCException e) {
			Log.d(LOG_TAG, e.getMessage());
		}
	}	
	
	public synchronized Location getLocation()
	{
		Location loc = new Location("cracker");
		try {
			JSONObject obj = client.callJSONObject("getLastGpsPosition");
			loc.setAltitude(obj.getDouble("altitude"));
			loc.setLatitude(obj.getDouble("latitude"));
			loc.setLongitude(obj.getDouble("longitude"));
		} catch (JSONRPCException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return loc;
	}
	
	public synchronized boolean isCracking() throws JSONRPCException
	{
		return client.callBoolean("isCracking");
	}
	
	public long getStartTime()
	{
		return started_at;
	}
	
	public int getIVs()
	{
		return ivs;
	}
	
	public synchronized void start(CrackerListener listener, Network target)
	{
		started_at = System.currentTimeMillis();
		this.network = target;
		this.listener = listener;
		try {
			client.call("startCracking", target.bssid);
		} catch (JSONRPCException e) {
			Log.d(LOG_TAG, e.getMessage());
		}
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				timer_event();
			}

		}, 0, 2000);
	
	}
	
	public synchronized JSONObject getStatus() throws JSONRPCException
	{
		return client.callJSONObject("getStatus");
	}
	
	public void timer_event()
	{
		try {
			JSONObject state = getStatus();
			ivs = state.getInt("ivs");
			boolean cracked = state.getBoolean("cracked");
			if (cracked)
			{
				network.key = state.getString("key");
				network.is_cracked = true;
				network.save();
				listener.crack_complete(this);
				timer.cancel();
			}
		
			listener.crack_progress(this);
		} catch (JSONRPCException e) {
			// TODO Auto-generated catch block
			Log.e(LOG_TAG, e.getMessage());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			Log.e(LOG_TAG, e.getMessage());
		}
	}
	
	public synchronized void stop()
	{
		if (timer != null)
			timer.cancel();
		
		try {
			client.call("abortCracking");
		} catch (JSONRPCException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.network = null;
	}
	
	public synchronized void deauthenticate() throws JSONRPCException
	{
		client.call("deauthenticate");
	}
}
