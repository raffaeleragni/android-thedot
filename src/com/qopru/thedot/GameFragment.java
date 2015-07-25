package com.qopru.thedot;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import com.qopru.thedot.engine.GameEngine;

/**
 * Game fragment, handles android object hierarchy and events.
 * 
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class GameFragment extends Fragment
{
    SurfaceView glView;
    GameThread gameThread;
    GameEngine engine;
    SensorManager sensorManager;
    Sensor sensor;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Keep the instance on reattaches as long as it remains in the foreground.
        setRetainInstance(true);
        
        Context context = getActivity().getApplicationContext();
        
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        glView = new SurfaceView(context);
        engine = new GameEngine(context);
        
        glView.setOnClickListener(onClickListener);
        
        return glView;
    }

    View.OnClickListener onClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            if (engine != null)
                engine.togglePause();
        }
    };
    
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        
        gameThread = new GameThread(glView.getHolder(), engine.getInitializer(), engine.getUpdater(), engine.getRenderer());
        gameThread.start();
        
        if (sensor != null)
            sensorManager.registerListener(engine.getSensorListener(), sensor, SensorManager.SENSOR_DELAY_GAME);
    }
    
    @Override
    public void onPause()
    {
        super.onPause();
        
        if (gameThread != null)
            gameThread.interrupt();
        
        if (sensor != null)
            sensorManager.unregisterListener(engine.getSensorListener(), sensor);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (engine != null)
            engine.destroy();
    }
}
