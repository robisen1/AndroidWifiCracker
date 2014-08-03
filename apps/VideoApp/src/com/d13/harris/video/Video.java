package com.d13.harris.video;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
//import android.view.View;
import android.view.Window;
import android.view.WindowManager;
//import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

public class Video extends Activity {
	private VideoView m;  
	private String path = "/video/new_drone.mp4";
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
                   WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.video);
		m = (VideoView) findViewById(R.id.video_view);   
        m.setVideoPath(path);
        m.setMediaController(new MediaController(this));
        m.requestFocus();
        m.start();
        
 //       Button myButton = (Button)findViewById(R.id.button);
 //       myButton.setOnClickListener(new Button.OnClickListener()
 //       {
 
  //          public void onClick(View arg0) 
  //          {
    //        	Video.this.finish();
  //          }        
 //       });
	}
}

final class MyVideoView extends VideoView {
	public MyVideoView(Context c) { super(c); }
	public MyVideoView(Context c, AttributeSet a) { super(c, a); }
	public MyVideoView(Context c, AttributeSet a, int d) { super(c, a, d); }
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		return true;
	}
}
