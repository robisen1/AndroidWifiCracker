package com.department13.skryfi;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class SurveyManagerService extends Service {

	private static final boolean TEST_MODE = true;
	
	private static final String LOG_TAG = "com.department13.skryfi";
	private boolean started = false;
	private final Object LAST_LOCATION_LOCK = new Object();

	private LocationManager location_manager;

	private String bestProvider;
	
	private Location last_location = new Location("init");

	private WifiManager wifi_manager;

	private boolean previous_wifi_state = false;

	private boolean cracker_available = false;
	
	protected Timer timer = null;
	
	public class LocalBinder extends Binder {
		SurveyManagerService getService() {
			return SurveyManagerService.this;
		}
	}
	
	private final IBinder mBinder = new LocalBinder();
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		stopServices();
	}
	
    @Override
    public void onStart( Intent intent, int startId ) 
    {
	  super.onStart( intent, startId );
      
      if (!started)
      {
    	  startServices();
      }
    }
    
	private void notify_error(Exception e)
	{
		Log.e(this.getClass().getName(), e.getMessage(), e);
	}
    
	private LocationListener location_listener = new LocationListener()
	{
		public void onStatusChanged(String provider, int status, Bundle extras)
		{
		}

		public void onProviderEnabled(String provider)
		{
		}

		public void onProviderDisabled(String provider)
		{
		}

		public void onLocationChanged(Location location)
		{
			if (location != null)
			{
				try
				{
					Log.d(LOG_TAG,"onLocationChanged");
					synchronized (LAST_LOCATION_LOCK)
					{
						last_location = location;
					}

					if (wifi_manager != null)
					{
						Log.d(LOG_TAG,"onLocationChanged -- wifi is on");
						wifi_manager.startScan();
					}
					else
					{
						Log.d(LOG_TAG,"onLocationChanged -- wifi is off");
					}
				}
				catch (Exception e)
				{
					notify_error(e);
				}
			}
		}
	};
    
	protected void cracker_survey()
	{
    	Log.d(LOG_TAG, "beginning survey...");
    	last_location = Cracker.getInstance().getLocation();
    	List<Survey> scans = Cracker.getInstance().getSurveyResults();
    	for (Survey survey : scans)
    	{
			// do we have the network already?
			Network network = SurveyManager.getInstance().getNetwork(survey.bssid);
			if (network == null) {
				network = new Network(survey.bssid);
				SurveyManager.getInstance().networks.put(network.getBssid(), network);
			}
			SurveyManager.getInstance().visibleNetworks.put(survey.bssid, network);
			survey.updatePosition(last_location.getLatitude(), last_location.getLongitude(),
					last_location.getAltitude());
			network.addSurvey(survey);
			network.last_seen = System.currentTimeMillis();
			network.save(); 		
    	}
    	SurveyManager.getInstance().close();
    	Log.d(LOG_TAG, "end survey");
    	SurveyManager.getInstance().notifyListeners();
    	Log.d(LOG_TAG, "survey complete");
	}
    
	public void startServices()
	{
		// first lets check if the cracking service is available 
		cracker_available = Cracker.getInstance().isAvailable();
		
		if (cracker_available)
		{
			// we are just going to use the cracker for our location and surveying
			Log.d(LOG_TAG, "using cracker rpc for location and surveys");
			// first lets start surveying
			Cracker.getInstance().startSurvey();
			// next lets setup the timer so we can perform a new survey
			timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					cracker_survey();
				}

			}, 0, 10000);			
			// cracker_survey();
		}
		else
		{
			Log.d(LOG_TAG, "using android services for location and surveys");
			if (!TEST_MODE)
			{
				startLocation();
				startWifi();			
				wifi_manager.startScan();
			}
			else
			{
				Log.d(NetworkDatabase.LOG_TAG, "starting test mode");
				timer = new Timer();
				timer.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						test_survey();
					}

				}, 0, 10000);					
				Toast.makeText(this, "wifi: testing", Toast.LENGTH_SHORT).show();
			}
		}
		
	}
	
	public void test_survey()
	{
    	Log.d(LOG_TAG, "beginning test survey...");
    	//last_location = Cracker.getInstance().getLocation();
    	List<Survey> scans = new Vector<Survey>();
    	
    	scans.add(new Survey("test1bssid", "test1", 2.422, 177, "WEP"));
    	scans.add(new Survey("test2bssid", "test2", 2.412, 80, "WPA"));
    	scans.add(new Survey("test3bssid", "test3", 2.407, 20, "OPEN"));
    	Log.d(NetworkDatabase.LOG_TAG, "getting manager instance");
    	SurveyManager manager = SurveyManager.getInstance();
    	Log.d(NetworkDatabase.LOG_TAG, "got manager instance");
    	for (Survey survey : scans)
    	{
			// do we have the network already?
			Network network = manager.getNetwork(survey.bssid);
	    	Log.d(NetworkDatabase.LOG_TAG, "got network: " + network);

			if (network == null) {
				Log.d(NetworkDatabase.LOG_TAG, "new network" + survey.bssid);
				network = new Network(survey.bssid);
				manager.networks.put(network.getBssid(), network);
			}
			Log.d(NetworkDatabase.LOG_TAG, "got a network: " + network);
			manager.visibleNetworks.put(survey.bssid, network);
			Log.d(NetworkDatabase.LOG_TAG, "save position: " + network);
			survey.updatePosition(last_location.getLatitude(), last_location.getLongitude(),
					last_location.getAltitude());
			network.addSurvey(survey);
			network.last_seen = System.currentTimeMillis();
	    	Log.d(NetworkDatabase.LOG_TAG, "saving network");
			network.save(); 		
	    	Log.d(NetworkDatabase.LOG_TAG, "saved network");

    	}
    	manager.close();
    	Log.d(LOG_TAG, "end test survey");
    	manager.notifyListeners();
    	Log.d(LOG_TAG, "test survey complete");
	}
	
    public void startLocation()
    {
		location_manager = location_manager == null ? (LocationManager) getSystemService(LOCATION_SERVICE) : location_manager;
  	
		if (location_manager != null)
		{
			// List all providers:
			List<String> providers = location_manager.getAllProviders();
			for (String provider : providers) 
			{
				Log.d(LOG_TAG, provider);
			}
			
			Criteria criteria = new Criteria();
			bestProvider = location_manager.getBestProvider(criteria, false);
			Log.d(LOG_TAG, "best provider: " + bestProvider);
			Toast.makeText(this, "location: " + bestProvider, Toast.LENGTH_SHORT).show();
			if (bestProvider != null)
			{
				last_location = location_manager.getLastKnownLocation(bestProvider);
		
				location_manager.requestLocationUpdates(bestProvider, SurveyManager.getInstance().getScanFreq(),
						SurveyManager.getInstance().getScanDistance(), location_listener);	
			}
			else
			{
				Log.e(NetworkDatabase.LOG_TAG, "no location provider found!");
			}
		}
    }
    
    protected void update(double lat, double lng, double alt)
    {
		boolean added = false;
		SurveyManager.getInstance().visibleNetworks.clear();
		List<ScanResult> results = wifi_manager.getScanResults();
		for (ScanResult result : results)
		{
			// do we have the network already?
			Network network = SurveyManager.getInstance().getNetwork(result.BSSID);
			if (network == null) {
				network = new Network(result.BSSID);
				SurveyManager.getInstance().networks.put(network.getBssid(), network);
				added = true;
			}
			SurveyManager.getInstance().visibleNetworks.put(result.BSSID, network);
			Survey survey = new Survey(result);
			survey.updatePosition(lat, lng, alt);
			network.addSurvey(survey);
			network.last_seen = System.currentTimeMillis();
			network.save();
		}

		if (added)
		{
			notification_bar_message("New WiFi(s) added to database.");
		}
		SurveyManager.getInstance().close();
		SurveyManager.getInstance().notifyListeners();
    }
    
    private BroadcastReceiver wifiEvent = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			Double lat = null, lon = null, alt = null;
			try
			{
				synchronized (LAST_LOCATION_LOCK)
				{
					if (last_location != null)
					{
						lat = last_location.getLatitude();
						lon = last_location.getLongitude();
						alt = last_location.getAltitude();
					}
				}

				if (lat != null && lon != null && alt != null)
				{
					update(lat, lon, alt);
				}
			}
			catch (Exception e)
			{
				notify_error(e);
			}
		}
	};    
    
    public void startWifi()
    {
    	// check if the cracking service is available

		Log.d(LOG_TAG, "using android for survey");
		wifi_manager = wifi_manager == null ? (WifiManager) getSystemService(Context.WIFI_SERVICE) : wifi_manager;

		if (wifi_manager != null)
		{
			previous_wifi_state = wifi_manager.isWifiEnabled();
			if (!previous_wifi_state)
			{
				wifi_manager.setWifiEnabled(true);
			}

			IntentFilter i = new IntentFilter();
			i.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
			registerReceiver(wifiEvent, i);		
			
			Toast.makeText(this, "wifi: android", Toast.LENGTH_SHORT).show();
		}    	
    }
    
    public Network createTestNetwork(String name, String bssid, int level, int freq, String caps)
    {
    	Log.d(LOG_TAG, "CREATETESTNETWORK");
		Network network = SurveyManager.getInstance().getNetwork(bssid);
		if (network == null) {
			network = new Network(bssid);
			network.name = name;
			network.last_seen = System.currentTimeMillis();
			SurveyManager.getInstance().networks.put(network.getBssid(), network);
		}
		SurveyManager.getInstance().visibleNetworks.put(network.getBssid(), network);
		Survey survey = new Survey(bssid, name, freq, level, caps);
		network.addSurvey(survey);
		network.save();
		return network;
    }
    
    public void stopServices()
    {
		super.onDestroy();
		if (timer != null)
		{
			timer.cancel();
			timer = null;
		}
		
		if (cracker_available)
		{
			Cracker.getInstance().stopSurvey();
			Cracker.getInstance().stop();
			Toast.makeText(this, "cracker: stopped", Toast.LENGTH_SHORT).show();
		}
		else
		{
			if (!TEST_MODE)
			{
				this.unregisterReceiver(wifiEvent);
				Toast.makeText(this, "survey: stopped", Toast.LENGTH_SHORT).show();
			}
		}
    }
    
	private boolean notifications_enabled = false;
	public void notification_bar_message(String message)
	{
		if (notifications_enabled )
		{
			NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			Notification n = new Notification(R.drawable.icon, message, System.currentTimeMillis());
			n.defaults |= Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND;
			n.flags |= Notification.FLAG_AUTO_CANCEL;
			Context context = getApplicationContext();
			Intent notificationIntent = new Intent(this, SurveyManagerService.class);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
			n.setLatestEventInfo(context, message, "", contentIntent);
			nm.notify(1, n);
		}
	}
    
    
}
