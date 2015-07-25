package com.qopru.thedot;

import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;
import com.qopru.thedot.interfaces.GameInitializer;
import com.qopru.thedot.interfaces.GameRenderer;
import com.qopru.thedot.interfaces.GameUpdater;

/**
 * Game Loop.
 * 
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class GameThread extends Thread
{
    private static final String TAG = "GameThread";
    
    private static final int MAX_FPS = 60;
    private static final int MAX_FRAME_SKIP = MAX_FPS / 10;
    private static final int FRAME_PERIOD = 1000 / MAX_FPS;
    
    public volatile boolean running = true;

    private final SurfaceHolder surfaceHolder;
    private final GameInitializer initializer;
    private final GameUpdater updater;
    private final GameRenderer renderer;

    public GameThread(SurfaceHolder surfaceHolder, GameInitializer initializer, GameUpdater updater, GameRenderer renderer)
    {
        super();
        if (surfaceHolder == null || initializer == null || updater == null || renderer == null)
            throw new IllegalArgumentException("all parameters must not be null");
        this.surfaceHolder = surfaceHolder;
        this.initializer = initializer;
        this.updater = updater;
        this.renderer = renderer;
    }
    
    boolean initialized = false;
    
    @Override
    public void run()
    {
        Canvas canvas;
        Log.d(TAG, "Start");
        
        // time when the loop starts
        long startTime;
        // time it takes to do a complete loop
        long loopTime;
        // time to sleep if the loop is done early
        int sleepTime;
        // frames to skip if the loop is late
        int skippedFrames;
        
        sleepTime = 0;
        while (running)
        {
            canvas = null;
            try
            {
                canvas = surfaceHolder.lockCanvas();
                // Skip a frame if no canvas ready
                if (canvas == null)
                    try {Thread.sleep(sleepTime);} catch (InterruptedException e) {running = false;}
                
                if (!initialized && canvas != null)
                {
                    initializer.init(canvas);
                    initialized = true;
                }
                
                synchronized (surfaceHolder)
                {
                    startTime = System.currentTimeMillis();
                    skippedFrames = 0;
                    updater.update(FRAME_PERIOD);
                    renderer.render(canvas);
                    loopTime = System.currentTimeMillis() - startTime;
                    
                    sleepTime = (int) (FRAME_PERIOD - loopTime);
                    // sleep time > 0 means that the loop must wait
                    if (sleepTime > 0)
                    {
                        try {Thread.sleep(sleepTime);} catch (InterruptedException e) {running = false;}
                    }
                    else
                        // If sleep time is < 0 it means that the update must catch up
                        while (sleepTime < 0 && skippedFrames < MAX_FRAME_SKIP)
                        {
                            updater.update(FRAME_PERIOD);
                            sleepTime += FRAME_PERIOD;
                            skippedFrames++;
                        }
                    
                    if (skippedFrames > 0)
                        Log.d(TAG, "Skipped "+skippedFrames+" frames");
                }
            }
            finally
            {
                if (canvas != null)
                    surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }
}
