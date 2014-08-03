package com.department13.skryfi;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;

public class NodesScreen extends Activity
{
	private final static String LOG_TAG = "NodeScreen";
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.nodescreen);
	
	    ImageButton menuButton = (ImageButton)findViewById(R.id.nodescreen_menu_button);
	    menuButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) 
			{
				Log.d(LOG_TAG,"on-click options menu");
				openOptionsMenu();				
			}
		});
	    
	}
	
	//Show Menu
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.nodescreenmenu, menu);
		return true;
	}
	
	//Menu item has been clicked
	public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.back_nodescreen:
        	Log.d(LOG_TAG,"Menu Hit Back"); 
			finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}

