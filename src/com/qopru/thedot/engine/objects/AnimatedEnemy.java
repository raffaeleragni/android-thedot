package com.qopru.thedot.engine.objects;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class AnimatedEnemy extends AbstractAnimated
{
    // DEFAULT DURATION
    private static final int DURATION = 500;
    // More frames count means more fluit, but takes more resources
    private static final int FRAMES_COUNT = 60*DURATION/1000;

    private static final Paint paint;
    
    static
    {        
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
    }

    public void setX(float x)
    {
        this.x = x;
    }

    public void setY(float y)
    {
        this.y = y;
    }
    
    public AnimatedEnemy(float x, float y, float unitPixels)
    {
        super(FRAMES_COUNT, unitPixels);
        setDuration(DURATION);
        setPosition(x, y);
        setSize(3, 3);
    }

    @Override
    void drawTick(Canvas c, Rect bounds, Bitmap sprite, Canvas spriteCanvas)
    {   
        if (bounds.width() == 0 || bounds.height() == 0)
            return;
        
        // Try to linearly claculate the animation based on frames count.
        // Since it is a geometric progression, this way the frames count can
        // be changed without rewriting this code.
        // This animation is based on rotation, so proportion of FRAMES_COUNT
        // equals to 360°.
        float angle = (float)360 * (float)frameIndex / (float)FRAMES_COUNT;

        int bw = bounds.width();
        int bh = bounds.height();
        spriteCanvas.drawArc(new RectF(bw/4, 0, bw*3/4, bh/2), 90, 180, false, paint);
        spriteCanvas.drawArc(new RectF(bw/2, bh/4, bw, bh*3/4), 0, 180, false, paint);
        spriteCanvas.drawArc(new RectF(bw/4, bh/2, bw *3/4, bh), 270, 180, false, paint);
        spriteCanvas.drawArc(new RectF(0, bh/4, bw/2, bh*3/4), 180, 180, false, paint);
        
        Matrix matrix = new Matrix();
        matrix.setRotate(angle, bw/2, bh/2);
        matrix.postTranslate(bounds.left, bounds.top);
        c.drawBitmap(sprite, matrix, paint);
    }
}
