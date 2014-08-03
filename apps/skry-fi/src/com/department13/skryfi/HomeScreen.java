package com.department13.skryfi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class HomeScreen extends Activity
{
	private final static String LOG_TAG = "HomeScreen";
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.home);
	
	    ImageButton menuButton = (ImageButton)findViewById(R.id.home_menu_button);
	    menuButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) 
			{
				Log.d(LOG_TAG,"on-click options menu");
				openOptionsMenu();				
			}
		});
	    SurveyManager.getInstance().initDB();
	    
	   TextView network = (TextView)findViewById(R.id.home_knownnetworks);
	   network.setText(String.valueOf(SurveyManager.getInstance().getNetworkCount()));

	   TextView crackedKeys = (TextView)findViewById(R.id.home_keyscracked);
	   crackedKeys.setText(String.valueOf(SurveyManager.getInstance().getCrackedCount()));

	    SurveyManager.getInstance().close();
	   
        new Handler().postDelayed(new Runnable() { 
            public void run() { 
            	openOptionsMenu(); 
            } 
        }, 1000);
	}
	
	//Show Menu
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.home_menu, menu);
		return true;
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		SurveyManager.getInstance().stop();
		SurveyManager.getInstance().close();
	}
	
	//Menu item has been clicked
	public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.home_quit:
        	Log.d(LOG_TAG,"Menu Hit quit");
    		SurveyManager.getInstance().stop();
    		SurveyManager.getInstance().close();
			finish();
            return true;
        case R.id.home_service:
        	Log.d(LOG_TAG,"Menu survey screen");
        	Intent intent = new Intent(this,SurveyListScreen.class);
        	startActivity(intent);
        	return true;
        case R.id.home_networklist:
        	Log.d(LOG_TAG,"Menu survey screen");
        	Intent network = new Intent(this,NetworkListScreen.class);
        	startActivity(network);
        	return true;
        case R.id.home_saveklms:
        	Toast.makeText(this, "Not an available feature at this time", Toast.LENGTH_SHORT).show();
        	return true;
        case R.id.home_clear:
        	SurveyManager.getInstance().close();
        	NetworkDatabase.Delete();
        	Toast.makeText(this, "database deleted", Toast.LENGTH_SHORT).show();
        	SurveyManager.getInstance().initDB();
        	return true;
        case R.id.home_options:
        	Toast.makeText(this, "Not an available at this time", Toast.LENGTH_SHORT).show();
        	return true;
        case R.id.home_networkdevices:
        	Toast.makeText(this, "Not an available at this time", Toast.LENGTH_SHORT).show();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
     }

 
}
