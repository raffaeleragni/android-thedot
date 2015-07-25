package com.qopru.thedot;

import android.content.Context;
import static android.content.Context.WINDOW_SERVICE;
import android.content.res.Configuration;
import android.view.Surface;
import android.view.WindowManager;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class U
{
    /**
     * Returns the default device orientation.
     * Phones are portrait by defaults, some tablets are portrait, others landscape by default.
     * This is needed to determine x,y axis for the accelerometers, because they're based on DEVICE, not SCREEN.
     * 
     * @param ctx
     * @return 
     */
    public static final int getDeviceDefaultOrientation(Context ctx)
    {
        WindowManager windowManager = (WindowManager) ctx.getSystemService(WINDOW_SERVICE);

        Configuration config = ctx.getResources().getConfiguration();

        int rotation = windowManager.getDefaultDisplay().getRotation();

        if (((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)
            && config.orientation == Configuration.ORIENTATION_LANDSCAPE)
            || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
            && config.orientation == Configuration.ORIENTATION_PORTRAIT))
            return Configuration.ORIENTATION_LANDSCAPE;
        else
            return Configuration.ORIENTATION_PORTRAIT;
    }
}
