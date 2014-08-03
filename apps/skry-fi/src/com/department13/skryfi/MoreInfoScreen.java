package com.department13.skryfi;


import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class MoreInfoScreen extends Activity implements CrackerListener
{
	private final static String LOG_TAG = "CrackingPage";
	private TextView ivs;
	private Network currentNetwork;
	public void onCreate(Bundle savedInstanceState) 
	{	
		super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.aircrack);
	          
	    Cracker crackThis = Cracker.getInstance();
	    crackThis.network.crack(this);
    
	    currentNetwork = crackThis.network; 
	   
	    TextView essid = (TextView)findViewById(R.id.air_crack_essid);
	    essid.setText(currentNetwork.getName());
	    
	    TextView bssid = (TextView)findViewById(R.id.aircrack_table_bssid);
	    bssid.setText(currentNetwork.bssid);
	   
	    int channelNumber =currentNetwork.last_survey.channel;
	    Log.d(LOG_TAG,"channel number " + Integer.toString(channelNumber));
	    TextView channel = (TextView)findViewById(R.id.aircrack_table_channel);
	    channel.setText(Integer.toString(channelNumber));
	    
	    TextView keyType = (TextView)findViewById(R.id.aircrack_table_key_type);
	    keyType.setText(currentNetwork.last_survey.security);
		   
	    TextView freq = (TextView)findViewById(R.id.aircrack_table_freq);
	    freq.setText(Double.toString(currentNetwork.last_survey.frequency));
	   
	    TextView signal = (TextView)findViewById(R.id.aircrack_table_signal);
	    signal.setText(Integer.toString(currentNetwork.last_survey.level));
	   
	    Date date = new Date(currentNetwork.last_seen);
	    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy hh:mm:ss");   
	   
	    TextView lastSeen = (TextView)findViewById(R.id.aircrack_table_last_seen);
	    lastSeen.setText(formatter.format(date));
	    
	    ivs= (TextView)findViewById(R.id.aircrack_table_ivs);
	  	ivs.setText(Integer.toString(crackThis.ivs));
	   
	  	TextView surveys = (TextView)findViewById(R.id.aircrack_table_number_of_surveys);
	  	surveys.setText(Integer.toString(crackThis.network.surveys.size()));
	   
	  	TextView key = (TextView)findViewById(R.id.air_crack_key_actual_key);
	  	if(currentNetwork.last_survey.encryption == Survey.EncryptionType.OPEN)
	  	{
	  		key.setVisibility(View.GONE);
	  		((TextView)findViewById(R.id.air_crack_key_display_name)).setVisibility(View.GONE);
	  	}
	  	else if(currentNetwork.is_cracked)
	  	{
	  		key.setText(currentNetwork.key);
	  	}
	   
	  	
		ImageButton showOptionsMenuButton = (ImageButton)findViewById(R.id.air_crack_menu_button);
	    showOptionsMenuButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.d(LOG_TAG,"on-click options menu");
				openOptionsMenu();
				
			}
		});
	  	
	 }
	
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.air_crack_menu, menu);
		return true;
	}
	
	 public boolean onPrepareOptionsMenu(Menu menu)
	 {
		 super.onPrepareOptionsMenu(menu);
		 MenuItem item = menu.findItem(R.id.crackorgrabkey);
		 if(currentNetwork.is_cracked)
		 {
			 item = menu.findItem(R.id.crackorgrabkey);
			 item.setTitle(R.string.clear);
			 return true;
		 }
		 else if(currentNetwork.last_survey.encryption == Survey.EncryptionType.WEP)
		 {
			 item.setTitle(R.string.crackwpekey);
			 return true;
		 }
		 else if(currentNetwork.last_survey.encryption == Survey.EncryptionType.WPA)
		 {
			 item.setTitle(R.string.getwpakey);
			 return true;
		 }
		 else if(currentNetwork.last_survey.encryption == Survey.EncryptionType.OPEN)
		 {
			 item.setVisible(false);
		 }

		 return true;
	 }
	
	public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.back:
        	Log.d(LOG_TAG,"Menu Hit Back"); 
        	Cracker.getInstance().stop();	
			finish();
            return true;
        case R.id.map:
        	Log.d("menu","Menu Hit Map");
        	Toast.makeText(this, "Not an available feature at this time", Toast.LENGTH_SHORT).show();
        	return true;
        case R.id.crackorgrabkey:
        	Log.d("menu","Menu Hit Crackorgrabkey");
        	Toast.makeText(this, "Not an available feature at this time", Toast.LENGTH_SHORT).show();
        	return true;
        case R.id.viewnodes:
        	Log.d("menu","Menu Hit Viewnodes");
        	Intent intent = new Intent(this,NodesScreen.class);
        	startActivity(intent);
        	return true;
        case R.id.sniffing:
        	Log.d("menu","Menu Hit Sniffing");
        	Toast.makeText(this, "Not an available feature at this time", Toast.LENGTH_SHORT).show();
        	return true;
        case R.id.deauthenticate:
        	Log.d("menu","Menu Hit Deauthenticate");
        	Toast.makeText(this, "Not an available feature at this time", Toast.LENGTH_SHORT).show();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
	
	
	

	public void crack_complete(Cracker cracker) {
		// TODO Auto-generated method stub
	    this.runOnUiThread(new Runnable() {
            public void run() 
            {
            	ivs.setText(Integer.toString(Cracker.getInstance().getIVs()));
         	   	TextView key = (TextView)findViewById(R.id.air_crack_key_actual_key);
            	key.setText(Cracker.getInstance().network.key);
            }
          });	
	}

	public void crack_progress(Cracker cracker) 
	{
	    this.runOnUiThread(new Runnable() 
	    {
            public void run() 
            {
            	ivs.setText(Integer.toString(Cracker.getInstance().getIVs()));
            }
         });		
	}
}
