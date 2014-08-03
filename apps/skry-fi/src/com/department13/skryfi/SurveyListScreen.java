package com.department13.skryfi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
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

public class SurveyListScreen extends ListActivity implements SurveyListener
{
	private static final String LOG_TAG="MAIN";
	private IconAdapter adapter;
	private List<Network> data;
	private SurveyManager scanner;
	private boolean serviceStatus;
	private boolean menu;
	private SortOptions currentSortingMethod;
	private boolean reversed =false;
	private AnimationDrawable radarAnimation = null;
	
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.main);
	    menu = false;
	    scanner = SurveyManager.getInstance(); 
	    	    
	    currentSortingMethod = SortOptions.SIGNAL_STRENGTH;
	    data = new ArrayList<Network>();
        ImageButton showOptionsMenuButton = (ImageButton)findViewById(R.id.options_menu_button);
        showOptionsMenuButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.d(LOG_TAG,"on-click options menu");
				openOptionsMenu();
				
			}
		});
        
        startOrStopService();
        
        adapter = new IconAdapter(this);
        setListAdapter(adapter);
        
        ImageView radarView = (ImageView) findViewById(R.id.RadarView);
        radarView.setBackgroundResource(R.anim.radar);
        radarAnimation = (AnimationDrawable) radarView.getBackground();
        
        
        final CharSequence[] items = {"Signal Strength", "Network Name", "Encryption Type"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sort by...");
        builder.setCancelable(false);
        builder.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item)
            {
                	
                	if(item==0)
                	{
                		adapter.notifyDataSetChanged();
            			adapter.clear();
            			Collections.sort(data,new SortSignalStrength());
            			Collections.reverse(data);
            			for(Network obj : data)
            			{
            				adapter.add(obj);
            			}
            			currentSortingMethod = SortOptions.SIGNAL_STRENGTH;
            			adapter.notifyDataSetChanged();
                		dialog.dismiss();
                	}
                	else if(item==1)
                	{
                		adapter.notifyDataSetChanged();
            			adapter.clear();
            			Collections.sort(data, new SortName());
            			if(reversed)
            			{
            				Collections.reverse(data);
            				reversed = false;
            			}
            			else
            			{
            				reversed = true;
            			}
            			
            			for(Network obj : data)
            			{
            				adapter.add(obj);
            			}
            			currentSortingMethod = SortOptions.BSSID_NAME;
            			adapter.notifyDataSetChanged();
                  		dialog.dismiss();
                	}
                	else if(item ==2)
                	{
                		adapter.notifyDataSetChanged();
            			adapter.clear();
            			Collections.sort(data, new SortEncryption());
            			for(Network obj : data)
            			{
            				adapter.add(obj);
            			}
            			currentSortingMethod = SortOptions.ENCRYPTION_TYPE;
            			adapter.notifyDataSetChanged();
                		dialog.dismiss();
                	}
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {

			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.cancel();
			}
        	
        });
        
        final AlertDialog alert = builder.create();
        
        ImageButton showSortingButton = (ImageButton)findViewById(R.id.sort_button);
        showSortingButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.d(LOG_TAG,"on-click sort menu");
				alert.show();
				
			}
		});
        
	    
        new Handler().postDelayed(new Runnable() { 
            public void run() { 
            	 radarAnimation.start();
            	
            } 
        }, 1000);
	            
	}
	

	public void survey_event(SurveyManager manager, Map<String, Network> networks)
	{
	    this.runOnUiThread(new Runnable() {
	              public void run() 
	              {
	            	  Log.d(LOG_TAG,"survey_event");
	            	  List<Network> arrayList = new ArrayList<Network>();
	            	 // adapter.notifyDataSetChanged();
          			  //adapter.clear();
	            	  // do your GUI update here
	            	  for (Map.Entry<String, Network> network : SurveyManager.getInstance().getVisibleNetworks().entrySet())
	            	  {  
	            		  Log.d(LOG_TAG, "Survey Event checking to see if there a new networks  -line 183");
	            		  Log.d(LOG_TAG,network.getValue().bssid);
	            		  arrayList.add(network.getValue());
	            		  //adapter.add(network.getValue());
	            		  //adapter.notifyDataSetChanged();
	            		  
	            	  }
	            	  
	            	  data = arrayList;
	            	  updateAdapterList();
	              }
	            });		
		
	}
	
	
	 private void updateAdapterList()
	 {
		 Log.d(LOG_TAG, "DATA COUNT " + Integer.toString(data.size()) );
	    	SortingNetworkObject c = sortBy();
			Collections.sort(data, c);
			
	    	if(currentSortingMethod == SortOptions.SIGNAL_STRENGTH)
			{
				Collections.reverse(data);
			}
			
			adapter.notifyDataSetChanged();
			adapter.clear();
			
			for(Network item : data)
			{
				adapter.add(item);
				adapter.notifyDataSetChanged();
			}
	 }
	    
	   private SortingNetworkObject sortBy()
	    {
	    	switch(currentSortingMethod)
	    	{
	    		case BSSID_NAME:
	    			return new SortName();
	    		case ENCRYPTION_TYPE:
	    			return new SortEncryption();
	    		case SIGNAL_STRENGTH:
	    			return new SortSignalStrength();
	    	}
	    	throw new IllegalArgumentException("Wrong Enum Type");
	    }
	
	
	
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.main_service:
        	Log.d("menu","Menu Hit");  
        	
       
        	if(menu )
        	{
        		radarAnimation.start();
        		item.setTitle("Stop Survey");
        		Log.d("menu",Boolean.toString(serviceStatus)); 
        		menu=false;
        		TextView tx = (TextView)findViewById(R.id.scanning_status);
        		tx.setText("Scanning");
        		serviceStatus = true;
        		scanner.start(this, this);
        	}
        	else
        	{
        		radarAnimation.stop();
        		item.setTitle("Start Survey");
        		menu =true;
        		Log.d("menu",Boolean.toString(serviceStatus));
        		TextView tx = (TextView)findViewById(R.id.scanning_status);
        		tx.setText("Idle");
        		serviceStatus = false;
        		scanner.stop();
        	}
            return true;
        case R.id.main_back:
        	serviceStatus = false;
        	scanner.stop();
        	quit();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
	
    //user chose quit in the menu
    private void quit()
    {
    	//this.onDestroy();
    	SurveyManager.getInstance().close(); 	
    	SurveyManager.getInstance().stop();
    	this.finish();
    }
	
	private void startOrStopService()
	{
		if(serviceStatus)
		{
			serviceStatus=false;
			scanner.stop();
		}
		else
		{
			serviceStatus=true;
			scanner.start(this, this);
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
			decibelLevel.setText(Integer.toString(displaySurvey.level));
									
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
						
			TextView moreInfoSignal = (TextView)row.findViewById(R.id.table_signal);
			moreInfoSignal.setText(Integer.toString(displaySurvey.level));
			
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
				Intent intent = new Intent(SurveyListScreen.this,MoreInfoScreen.class);				
				Cracker crackThis = Cracker.getInstance();				
				Network nt = (Network)v.getTag();
				Log.d(LOG_TAG,Integer.toString(nt.getSurveys().size()));
				crackThis.network=nt;
				SurveyListScreen.this.startActivity(intent);
				
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
