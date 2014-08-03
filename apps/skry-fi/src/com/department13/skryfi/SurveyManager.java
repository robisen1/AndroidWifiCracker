package com.department13.skryfi;

import java.util.HashMap;
import java.util.Map;

import org.alexd.jsonrpc.JSONRPCClient;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class SurveyManager {

	private static SurveyManager instance = null;
	protected SurveyListener listener = null;
	protected int scan_freq = 300; // in seconds
	protected float scan_distance = 3.0f; // in meters
	protected SQLiteDatabase database = null;
	protected Activity parentActivity = null;
    protected JSONRPCClient client = null;
	
	protected Map<String, Network> networks;
	protected Map<String, Network> visibleNetworks;
	
	protected SurveyManager()
	{
		networks = new HashMap<String, Network>();	
		visibleNetworks = new HashMap<String, Network>();
	}

	protected synchronized void initDB()
	{
		if (!NetworkDatabase.Exists())
		{
			Log.d(NetworkDatabase.LOG_TAG, "creating database and tables");
			database = NetworkDatabase.Open();
		}
		else
		{
			// make sure the database opens
			Log.d(NetworkDatabase.LOG_TAG, "open database");
			database = NetworkDatabase.Open();
			if (database == null)
			{
				Log.e(NetworkDatabase.LOG_TAG, "database failed to open...deleting");
				NetworkDatabase.Delete();
				database = NetworkDatabase.Open();
			}
		}
		
		database.execSQL(NetworkDatabase.CREATE_TABLE_NETWORKS);
		database.execSQL(NetworkDatabase.CREATE_TABLE_SURVEYS);
		database.execSQL(NetworkDatabase.CREATE_TABLE_DEVICES);
		database.execSQL(NetworkDatabase.CREATE_TABLE_DEVICE_SURVEYS);
		database.execSQL(NetworkDatabase.OPTIMIZATION_SQL);
		
		this._loadNetworks();
	}
	
	protected void _loadNetworks()
	{

		Cursor cursor = database.query(NetworkDatabase.TABLE_NETWORKS,
				new String[] { NetworkDatabase.TABLE_NETWORKS_FIELD_BSSID,
				NetworkDatabase.TABLE_NETWORKS_FIELD_SSID,
				NetworkDatabase.TABLE_NETWORKS_FIELD_CRACKED,
				NetworkDatabase.TABLE_NETWORKS_FIELD_LASTSEEN,
				NetworkDatabase.TABLE_NETWORKS_FIELD_KEYS },
				null, null, null, null, null);
			
		if (cursor.moveToFirst())
		{
			int bssid_col = cursor.getColumnIndex(NetworkDatabase.TABLE_NETWORKS_FIELD_BSSID);
			int ssid_col = cursor.getColumnIndex(NetworkDatabase.TABLE_NETWORKS_FIELD_SSID);
			int cracked_col = cursor.getColumnIndex(NetworkDatabase.TABLE_NETWORKS_FIELD_CRACKED);
			int lastseen_col = cursor.getColumnIndex(NetworkDatabase.TABLE_NETWORKS_FIELD_LASTSEEN);
			int keys_col = cursor.getColumnIndex(NetworkDatabase.TABLE_NETWORKS_FIELD_KEYS);
			
			do
			{
				Network network = new Network(cursor.getString(ssid_col), cursor.getString(bssid_col),
						cursor.getInt(cracked_col) == 1, cursor.getInt(lastseen_col), cursor.getString(keys_col));
				networks.put(network.bssid, network);
			} while (cursor.moveToNext());
		}			
		cursor.close();
	}


	public void start(Activity parentActivity, SurveyListener listener) {
		this.parentActivity = parentActivity;
		this.listener = listener;
		Intent svc = new Intent(parentActivity, SurveyManagerService.class);
		parentActivity.startService(svc);
	}

	public void stop() {
		if (parentActivity != null)
		{
			Intent svc = new Intent(parentActivity, SurveyManagerService.class);
			parentActivity.stopService(svc);
			parentActivity = null;
		}
	}
	
	public synchronized SQLiteDatabase getDatabase() {
		if (database == null)
		{
			database = NetworkDatabase.Open();
		}
		return database;
	}
	
	public synchronized void close()
	{
    	Log.d(NetworkDatabase.LOG_TAG, "closing db");

		if (database != null)
		{
			database.close();
			database = null;
		}
	}
	
	public int getCrackedCount()
	{
		SQLiteStatement sql = database.compileStatement(NetworkDatabase.SELECT_NUMBER_OF_KEYS_CRACKED);
		int count = (int)sql.simpleQueryForLong();
		sql.close();
		return count;
	}
	
	public int getNetworkCount()
	{
		SQLiteStatement sql = database.compileStatement(NetworkDatabase.SELECT_COUNT_WIFIS);
		int count = (int)sql.simpleQueryForLong();
		sql.close();
		return count;
	}

	public Network getNetwork(String bssid) {
		return networks.get(bssid);
		
	}

	public boolean isNetworkVisible(String bssid) {
		return visibleNetworks.get(bssid) != null;
	}
	
	public Map<String, Network> getVisibleNetworks() {
		return visibleNetworks;
	}
	
	public Map<String, Network> getNetworks() {
		return networks;
	}

	public int getScanFreq() {
		return scan_freq;
	}

	public float getScanDistance() {
		return scan_distance;
	}

	public synchronized static SurveyManager getInstance() {
		if (instance == null) {
			instance = new SurveyManager();
			instance.initDB();
		}
		return instance;
	}
	
	public void notifyListeners() {
		if (listener != null) {
			Log.d("SM", "calling the listener");
			listener.survey_event(this, networks);
		}
		else
		{
			Log.d("SM", "no listeners");
			Log.d("SM", this.toString());
		}
	}
}
