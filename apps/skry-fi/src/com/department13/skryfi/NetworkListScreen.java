package com.department13.skryfi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NetworkListScreen extends ListActivity
{
	private final static String LOG_TAG = "NetworkListScreen";
	private IconAdapter adapter;
	//protected Map<String, Network> networks;
	List<Network> data;
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.networklistscreen);
	
	    ImageButton menuButton = (ImageButton)findViewById(R.id.networklistscreen_menu_button);
	    menuButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) 
			{
				Log.d(LOG_TAG,"on-click options menu");
				openOptionsMenu();				
			}
		});
	    data = new ArrayList<Network>(SurveyManager.getInstance().getNetworks().values());
  
	    adapter = new IconAdapter(this);
        setListAdapter(adapter);
	     
	}
	
	//Show Menu
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.networklistscreenmenu, menu);
		return true;
	}
	
	//Menu item has been clicked
	public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) 
        {
	        case R.id.back_networklistscreen:
	        	Log.d(LOG_TAG,"Menu Back");
	        	finish();
	        default:
	            return super.onOptionsItemSelected(item);
        }
    }
	
	
	
	class IconAdapter extends ArrayAdapter<Network> implements OnClickListener
	{
		//LinearLayout moreInfoLayout;
		Activity context;
		Network networkItem;
		List<String> itemTags = new ArrayList<String>();
		public IconAdapter(Activity context)
		{
			super(context,R.layout.row,data);
			
			Log.d(LOG_TAG,"IconAdapter");
			this.context=context;
		}

		//show icons
		public View getView(int position, View convertView, ViewGroup parent) 
		{
			
			View row = View.inflate(context,R.layout.row, null);
			Survey displaySurvey = data.get(position).last_survey;
			networkItem = data.get(position);
									
			TextView networkName = (TextView)row.findViewById(R.id.network_name);
			networkName.setText(displaySurvey.ssid);
	
			TextView decibelLevel = (TextView)row.findViewById(R.id.signal_strength_db_level);
			decibelLevel.setVisibility(View.INVISIBLE);
									
			TextView encryption = (TextView)row.findViewById(R.id.encryption_type);
			encryption.setText(displaySurvey.security);			
		
			//fill in the table 
			TextView moreInfoEssidName = (TextView)row.findViewById(R.id.table_bssid);
			moreInfoEssidName.setText(displaySurvey.bssid);
			
			TextView moreInfoChannel = (TextView)row.findViewById(R.id.table_channel);
			moreInfoChannel.setText(Integer.toString(displaySurvey.channel));
						
			TextView moreInfoEncryption = (TextView)row.findViewById(R.id.table_security);
			moreInfoEncryption.setText(displaySurvey.security);
			
			TextView moreInfoFrequency = (TextView)row.findViewById(R.id.table_freq);
			moreInfoFrequency.setText(Double.toString(displaySurvey.frequency));
						
			/*TextView moreInfoSignal = (TextView)row.findViewById(R.id.table_signal);
			moreInfoSignal.setText(Integer.toString(displaySurvey.level));*/
			
			TextView moreInfoSignalDisplayLevel =(TextView)row.findViewById(R.id.table_signal_display_level);
			moreInfoSignalDisplayLevel.setVisibility(View.INVISIBLE);
			
			Date date = new Date(networkItem.last_seen);
			SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yy hh:mm:ss");  
			
			TextView moreInfoLastSeen=(TextView)row.findViewById(R.id.table_last_seen);
			moreInfoLastSeen.setText(formatter.format(date));
			
			TextView moreInfoKeys=(TextView)row.findViewById(R.id.table_keys);
			if(data.get(position).key != null)
			{
				moreInfoKeys.setText(data.get(position).key);
			}
			else
			{
				moreInfoKeys.setText("None");
			}
			
			TextView moreInfoNumberOfSurveys=(TextView)row.findViewById(R.id.table_number_of_surveys);
			moreInfoNumberOfSurveys.setText(Integer.toString(data.get(position).getSurveys().size()));
						
			ImageView image = (ImageView)row.findViewById(R.id.icon);
			if(displaySurvey.encryption == Survey.EncryptionType.OPEN)
			{
				image.setImageResource(R.drawable.open_lock_open);
			}
			else if(displaySurvey.encryption == Survey.EncryptionType.WEP)
			{
				if(data.get(position).is_cracked)
				{
					image.setImageResource(R.drawable.wep_lock_open);
				}
				else
				{
					image.setImageResource(R.drawable.wep_lock_closed);
				}
				
			}
			else if((displaySurvey.encryption == Survey.EncryptionType.WPA)
					|| (displaySurvey.encryption==Survey.EncryptionType.WPA2))
			{
				if(data.get(position).is_cracked)
				{
					image.setImageResource(R.drawable.wpa_lock_open);
				}
				else
				{
					image.setImageResource(R.drawable.wpa_lock_closed);
				}
			}
						
			
			Button crack_button = (Button)row.findViewById(R.id.table_crack_button);
			crack_button.setTag(networkItem);
			crack_button.setOnClickListener(this);
					
			Log.d(LOG_TAG,"getView");
			row.setOnClickListener(new OnItemClickListener(position));
		    return row;
		}


		public void onClick(View v) {
			// TODO Auto-generated method stub
			// TODO Auto-generated method stub
			Log.d("ICONADAPTER","ON_CLICK");
			Log.d("ICONADAPTER",v.toString());
			View row = (View) v.getParent();
			Button crackButton = (Button)row.findViewById(R.id.table_crack_button);
						

			if(v==crackButton)
			{
				Log.d("ICONADAPTER","CRACK_BUTTON");
				Intent intent = new Intent(NetworkListScreen.this,MoreInfoScreen.class);				
				Cracker crackThis = Cracker.getInstance();				
				Network nt = (Network)v.getTag();
				Log.d(LOG_TAG,Integer.toString(nt.getSurveys().size()));
				crackThis.network=nt;
				NetworkListScreen.this.startActivity(intent);
				
			}//end else
		}//end onclick
		
	}//end iconadapter
	
	
	private class OnItemClickListener implements OnClickListener
	{           
        private int mPosition;
        
        OnItemClickListener(int position){
                mPosition = position;
        }
        public void onClick(View arg0) 
        {
    		View row = arg0;
            Log.d("HELP", "onItemClick at position" + mPosition);
            ImageView aButton = (ImageView)row.findViewById(R.id.show_more_information_arrow_button);
            LinearLayout moreInfoLayout = (LinearLayout) row.findViewById(R.id.show_more_information);
			
			// TODO Auto-generated method stub
			if(moreInfoLayout.getVisibility()==LinearLayout.GONE)
			{
				moreInfoLayout.setVisibility(LinearLayout.VISIBLE);					
				aButton.setBackgroundResource(R.drawable.down_arrow2);
			}
			else
			{
				moreInfoLayout.setVisibility(LinearLayout.GONE);
				aButton.setBackgroundResource(R.drawable.arrow);
			}
        }               
    }

}
