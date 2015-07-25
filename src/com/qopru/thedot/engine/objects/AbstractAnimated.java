package com.qopru.thedot.engine.objects;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * An abstract sprite.
 * 
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public abstract class AbstractAnimated extends Drawable
{
    long duration = 0;
    long lastTick = 0;
    final long totalFramesCount;
    float frameTime = 0;
    int frameIndex = 0;
    final float unitPixels;
    
    float x, y, w, h;
    
    Bitmap sprite;
    Canvas spriteCanvas;
    
    public AbstractAnimated(long totalFramesCount, float unitPixels)
    {
        this.totalFramesCount = totalFramesCount;
        this.unitPixels = unitPixels;
    }

    public long getDuration()
    {
        return duration;
    }

    public void setDuration(long duration)
    {
        this.duration = duration;
        if (duration != 0)
            frameTime = (float)duration / (float)totalFramesCount;
    }

    /**
     * Position is the GAME COORDINATE position.
     * @param x
     * @param y
     */
    public void setPosition(float x, float y)
    {
        this.x = x;
        this.y = y;
    }

    /**
     * Size is in GAME PROPORTIONS, not Canvas ones.
     * @param w
     * @param h
     */
    public void setSize(float w, float h)
    {
        this.w = w;
        this.h = h;
        sprite = Bitmap.createBitmap((int)(w*(float)unitPixels), (int)(h*(float)unitPixels), Bitmap.Config.ARGB_8888);
        spriteCanvas = new Canvas(sprite);
    }

    public float getX()
    {
        return x;
    }

    public float getY()
    {
        return y;
    }

    public float getW()
    {
        return w;
    }

    public float getH()
    {
        return h;
    }
    
    @Override
    public void draw(Canvas c)
    {
        // Cycle animation only if enough time has passed.
        // Basically throttle the animation based on frames count
        // and specified duration.
        if (lastTick == 0 || System.currentTimeMillis() - lastTick > frameTime)
        {
            frameIndex++;
            lastTick = System.currentTimeMillis();
        }
        // Restart animation from beginning when reaching the end
        if (frameIndex >= totalFramesCount)
            frameIndex = 0;
        
        // Transform coordinates to canvas coordinates
        Rect bounds = c.getClipBounds();
        int bw = bounds.width();
        int bh = bounds.height();
        // Center point
        int centerX = bounds.left + bw / 2;
        int centerY = bounds.left + bh / 2;
        int ax = (int)(centerX + unitPixels * (int)x);
        int ay = (int)(centerY + unitPixels * (int)y);
        
        drawTick(c, new Rect(ax - (int)(w*unitPixels) / 2, 
            ay - (int)(h*unitPixels) / 2,
            ax + (int)(w*unitPixels) / 2,
            ay + (int)(h*unitPixels) / 2),
            sprite,
            spriteCanvas);
    }
    
    @Override
    public void setAlpha(int alpha)
    {
    }

    @Override
    public void setColorFilter(ColorFilter cf)
    {
    }

    @Override
    public int getOpacity()
    {
        return 100;
    }

    /**
     * Draw a single frame of the animation.
     * @param c canvas where to draw.
     * @param bounds the bounds specific to this sprite only.
     */
    abstract void drawTick(Canvas c, Rect bounds, Bitmap sprite, Canvas spriteCanvas);
}
