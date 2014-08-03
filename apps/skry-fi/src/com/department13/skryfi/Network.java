package com.department13.skryfi;

import java.util.List;
import java.util.Vector;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

public class Network {
	protected String name;
	protected String bssid;
	protected boolean is_cracked;
	protected String key;
	protected Survey last_survey;
	protected long last_seen;
	protected List<Survey> surveys;
	
	public Network(String name, String bssid, boolean is_cracked, int last_seen, String key)
	{
		Log.d(NetworkDatabase.LOG_TAG, name + ", " + bssid);
		this.name = name;
		this.bssid = bssid;
		this.is_cracked = is_cracked;
		this.last_seen = last_seen;
		this.key = key;
		this.surveys = new Vector<Survey>();
		this.loadLastScans();
	}
	
	public Network(String bssid) {
		this.surveys = new Vector<Survey>();
		this.bssid = bssid;
		// check if this bssid exists in the db.. if yes load
		Cursor cursor = SurveyManager.getInstance().getDatabase().query(
				NetworkDatabase.TABLE_NETWORKS,
				new String[] { NetworkDatabase.TABLE_NETWORKS_FIELD_SSID,
						NetworkDatabase.TABLE_NETWORKS_FIELD_CRACKED,
						NetworkDatabase.TABLE_NETWORKS_FIELD_LASTSEEN,
						NetworkDatabase.TABLE_NETWORKS_FIELD_KEYS },
						NetworkDatabase.TABLE_SURVEYS_FIELD_BSSID_EQUALS,
				new String[] { bssid }, null, null, null);
		
		if (cursor.moveToFirst())
		{
			this.name = cursor.getString(0);
			this.is_cracked = cursor.getInt(1) == 1;
			this.last_seen = cursor.getInt(2);
			this.key = cursor.getString(3);
		}
	}
	
	public void save() {
		// check if this bssid exists in the db.. if yes load otherwise create
		Cursor cursor = SurveyManager.getInstance().getDatabase().query(
				NetworkDatabase.TABLE_NETWORKS,
				new String[] { NetworkDatabase.TABLE_NETWORKS_FIELD_SSID },
				NetworkDatabase.TABLE_SURVEYS_FIELD_BSSID_EQUALS,
				new String[] { bssid }, null, null, null);
		
		if (cursor.moveToFirst())
		{		
			// lets do an update
			Log.d(NetworkDatabase.LOG_TAG, "updating network record");
			ContentValues values = new ContentValues();
			values.put(NetworkDatabase.TABLE_NETWORKS_FIELD_SSID, name);
			values.put(NetworkDatabase.TABLE_NETWORKS_FIELD_CRACKED, this.is_cracked);
			values.put(NetworkDatabase.TABLE_NETWORKS_FIELD_KEYS, key);
			values.put(NetworkDatabase.TABLE_NETWORKS_FIELD_LASTSEEN, System.currentTimeMillis());
			
			SurveyManager.getInstance().getDatabase().update(NetworkDatabase.TABLE_NETWORKS, values, 
					NetworkDatabase.TABLE_SURVEYS_FIELD_BSSID_EQUALS,
					new String[] { this.bssid });
		}
		else
		{
			// lets do an insert
			Log.d(NetworkDatabase.LOG_TAG, "inserting network record");
			ContentValues values = new ContentValues();
			values.put(NetworkDatabase.TABLE_NETWORKS_FIELD_BSSID, bssid);
			values.put(NetworkDatabase.TABLE_NETWORKS_FIELD_SSID, name);
			values.put(NetworkDatabase.TABLE_NETWORKS_FIELD_CRACKED, this.is_cracked);
			values.put(NetworkDatabase.TABLE_NETWORKS_FIELD_KEYS, key);
			values.put(NetworkDatabase.TABLE_NETWORKS_FIELD_LASTSEEN, System.currentTimeMillis());
			SurveyManager.getInstance().getDatabase().insert(NetworkDatabase.TABLE_NETWORKS, null, values);			
		}
		cursor.close();
	}
	
	public void loadLastScans() {

    		Cursor cursor = SurveyManager.getInstance().getDatabase().query(NetworkDatabase.TABLE_SURVEYS,
    				new String[] { NetworkDatabase.TABLE_SURVEYS_FIELD_SSID,
	    				NetworkDatabase.TABLE_SURVEYS_FIELD_TIMESTAMP,
	    				NetworkDatabase.TABLE_SURVEYS_FIELD_SECURITY,
	    				NetworkDatabase.TABLE_SURVEYS_FIELD_LEVEL,
	    				NetworkDatabase.TABLE_SURVEYS_FIELD_LAT,
	    				NetworkDatabase.TABLE_SURVEYS_FIELD_LON,
	    				NetworkDatabase.TABLE_SURVEYS_FIELD_ALT
    						}, 
    						NetworkDatabase.TABLE_SURVEYS_FIELD_BSSID_EQUALS, 
    						new String[] { bssid }, null, null, null);
    		
    		if (cursor.moveToFirst())
    		{
    			do
    			{
    				Survey survey = new Survey(cursor);
    				surveys.add(survey);
    				last_survey = survey;
    			} while (cursor.moveToNext());
    		}
    		cursor.close();
	}

	public void addSurvey(Survey survey) {
		this.last_survey = survey;
		
		this.name = survey.ssid;
		this.last_seen = survey.timestamp;

		ContentValues values = new ContentValues();
		values.put(NetworkDatabase.TABLE_SURVEYS_FIELD_BSSID, survey.bssid);
		values.put(NetworkDatabase.TABLE_SURVEYS_FIELD_SSID, survey.ssid);
		values.put(NetworkDatabase.TABLE_SURVEYS_FIELD_SECURITY, survey.security);
		values.put(NetworkDatabase.TABLE_SURVEYS_FIELD_FREQUENCY, survey.frequency);
		values.put(NetworkDatabase.TABLE_SURVEYS_FIELD_LEVEL, survey.level);
		values.put(NetworkDatabase.TABLE_SURVEYS_FIELD_LAT, survey.latitude);
		values.put(NetworkDatabase.TABLE_SURVEYS_FIELD_LON, survey.longitude);
		values.put(NetworkDatabase.TABLE_SURVEYS_FIELD_ALT, survey.altitude);
		values.put(NetworkDatabase.TABLE_SURVEYS_FIELD_TIMESTAMP, System.currentTimeMillis());
   		Log.d(NetworkDatabase.LOG_TAG, "inserting survey...");
		SurveyManager.getInstance().getDatabase().insert(NetworkDatabase.TABLE_SURVEYS, null, values);
		Log.d(NetworkDatabase.LOG_TAG, "done inserting survey.");
		surveys.add(survey);
	}
 
	public boolean isCracked() {
		return is_cracked;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public String getBssid() {
		return bssid;
	}

	public List<Survey> getSurveys() {
		return surveys;
	}

	public Survey getLastSurvey() {
		return this.last_survey;
	}

	public Cracker crack(CrackerListener listener) {
		Cracker cracker = Cracker.getInstance();
		cracker.start(listener, this);
		return cracker;
	}
}
