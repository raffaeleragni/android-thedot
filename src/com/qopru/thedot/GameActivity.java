package com.qopru.thedot;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

/**
 * Main activity
 * 
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class GameActivity extends Activity
{
    private static final String TAG = "GameActivity";
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreate");
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Sets the volume to be change to be the media one, not the ringtone.
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        // Make the sound service initialize in this way
        startService(new Intent(this, SoundService.class));
    }
}
