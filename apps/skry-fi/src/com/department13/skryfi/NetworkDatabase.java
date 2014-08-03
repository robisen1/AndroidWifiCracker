package com.department13.skryfi;

import java.io.File;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class NetworkDatabase {
	static public String LOG_TAG = "com.department13.skryfi";
	static public String path = "/data/data/com.department13.skryfi/";
	static public String filename = "skryfi.db3";
	
	public static final String TABLE_NETWORKS = "networks";
	public static final String CREATE_TABLE_NETWORKS = "create table if not exists networks (bssid text primary key, ssid text, lat real, lon real, alt real, lastseen integer,cracked boolean,keys text)";

	public static final String TABLE_NETWORKS_FIELD_BSSID = "bssid";
	public static final String TABLE_NETWORKS_FIELD_SSID = "ssid";
	
	public static final String TABLE_NETWORKS_FIELD_LAT = "lat";
	public static final String TABLE_NETWORKS_FIELD_LON = "lon";
	public static final String TABLE_NETWORKS_FIELD_ALT = "alt";

	public static final String TABLE_NETWORKS_FIELD_SUM_LAT = "sum(lat)";
	public static final String TABLE_NETWORKS_FIELD_SUM_LON = "sum(lon)";
	public static final String TABLE_NETWORKS_FIELD_LASTSEEN = "lastseen";
	
	public static final String TABLE_NETWORKS_FIELD_CRACKED = "cracked";
	public static final String TABLE_NETWORKS_FIELD_KEYS = "keys";	
	
	public static final String TABLE_SURVEYS = "surveys";	
	public static final String CREATE_TABLE_SURVEYS = "create table if not exists surveys (id integer primary key autoincrement, bssid text , ssid text, security text, enc integer, cipher integer, auth integer, level integer, frequency real, channel integer, lat real, lon real, alt real, timestamp integer)";

	public static final String TABLE_SURVEYS_FIELD_BSSID = "bssid";

	public static final String TABLE_SURVEYS_FIELD_COUNT_BSSID = "count(bssid)";
	public static final String TABLE_SURVEYS_FIELD_COUNT_ROWID = "count(rowid)";
	
	public static final String TABLE_SURVEYS_FIELD_BSSID_EQUALS = "bssid = ?";
	public static final String TABLE_SURVEYS_FIELD_SSID = "ssid";
	public static final String TABLE_SURVEYS_FIELD_SECURITY = "security";
	public static final String TABLE_SURVEYS_FIELD_CIPHER = "cipher";
	public static final String TABLE_SURVEYS_FIELD_ENCRYPTION = "enc";
	public static final String TABLE_SURVEYS_FIELD_AUTHENTICATION = "auth";
	
	public static final String TABLE_SURVEYS_FIELD_LEVEL = "level";
	public static final String TABLE_SURVEYS_FIELD_FREQUENCY = "frequency";
	public static final String TABLE_SURVEYS_FIELD_LAT = "lat";
	public static final String TABLE_SURVEYS_FIELD_LON = "lon";
	public static final String TABLE_SURVEYS_FIELD_ALT = "alt";
	public static final String TABLE_SURVEYS_FIELD_TIMESTAMP = "timestamp";
	public static final String TABLE_SURVEYS_FIELD_TIMESTAMP_AFTER = "timestamp > ?";
	public static final String TABLE_SURVEYS_LOCATION_BETWEEN = "lat >= ? and lat <= ? and lon >= ? and lon <= ?";
	public static final String TABLE_SURVEYS_OPEN_CONDITION = "capabilities = ''";
	public static final String TABLE_SURVEYS_CLOSED_CONDITION = "capabilities <> ''";
	public static final String SELECT_COUNT_WIFIS = "select count(*) from networks";
	public static final String SELECT_COUNT_OPEN = "select count(*) from networks where capabilities = ''";
	public static final String SELECT_COUNT_LAST = "select count(*) from networks where timestamp >= ?";
	public static final String SELECT_NUMBER_OF_KEYS_CRACKED = "select count(*) from networks where cracked = \"true\"";
	public static final String CREATE_INDEX_LATLON = "create index if not exists networks_latlon_idx on surveys(lat, lon)";
	public static final String CREATE_INDEX_CAPABILITIES = "create index if not exists networks_capabilities_idx on surveys(capabilities)";
	public static final String DELETE_ALL_WIFI = "delete from networks";
	
	public static final String TABLE_DEVICES = "devices";	
	public static final String CREATE_TABLE_DEVICES = "create table if not exists devices (id integer primary key autoincrement, mac text , os text, lat real, lon real, alt real, first_seen integer, last_seen integer)";

	public static final String TABLE_DEVICE_SURVEYS = "device_surveys";	
	public static final String CREATE_TABLE_DEVICE_SURVEYS = "create table if not exists device_surveys (id integer primary key autoincrement, mac text , bssid text, power integer, packets integer, lat real, lon real, alt real, timestamp integer)";
	
	
	public static final String OPTIMIZATION_SQL = "PRAGMA synchronous=OFF; PRAGMA count_changes=OFF; VACUUM;";
	
	
	
	
	static public String GetAbsolutePath()
	{
		return new File(path, filename).getAbsolutePath();
	}
	
	static public boolean Exists()
	{
		return  new File(GetAbsolutePath()).exists();
	}

	static public void Delete()
	{
		if (Exists())
			new File(GetAbsolutePath()).delete();
	}
	
	static public SQLiteDatabase Open()
	{
		try
		{
			return SQLiteDatabase.openOrCreateDatabase(GetAbsolutePath(), null);	
		}
		catch (SQLiteException e)
		{
			Log.e(LOG_TAG, "NetworkDatabase.Open(): "+  e.getMessage());
		}
		catch (IllegalStateException e)
		{
			Log.e(LOG_TAG, "NetworkDatabase.Open(): " + e.getMessage());
		}
		return null;
	}
	
}
