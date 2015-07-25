package com.qopru.thedot.engine.objects;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class AnimatedGravityWell extends AbstractAnimated
{
    // DEFAULT DURATION
    private static final int DURATION = 200;
    // More frames count means more fluit, but takes more resources
    private static final int FRAMES_COUNT = 60*DURATION/1000;

    private static final Paint paint;
    
    static
    {        
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Style.STROKE);
        paint.setAntiAlias(true);
    }
    
    float power; // negative means attraction
    
    public AnimatedGravityWell(float power, float x, float y, float unitPixels)
    {
        super(FRAMES_COUNT, unitPixels);
        setDuration(DURATION);
        paint.setStrokeWidth(unitPixels);
        this.power = power;
        setPosition(x, y);
        setSize(4, 4);
    }

    public float getPower()
    {
        return power;
    }

    @Override
    void drawTick(Canvas c, Rect bounds, Bitmap sprite, Canvas spriteCanvas)
    {
        // X:PI = curFrame:MAXFRAMES 
        int idx = frameIndex > FRAMES_COUNT / 2 ? FRAMES_COUNT - frameIndex : frameIndex;
        float delta = (float)idx / (float)FRAMES_COUNT;
        paint.setStrokeWidth((float)unitPixels - (float)unitPixels * 0.75f * delta);

        c.drawArc(new RectF(bounds), 0, 360, true, paint);
    }
}
