package com.qopru.thedot.engine.objects;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class AnimatedGate extends AbstractAnimated
{
    // DEFAULT DURATION
    private static final int DURATION = 3000;
    // More frames count means more fluit, but takes more resources
    private static final int FRAMES_COUNT = 60*DURATION/1000;
    
    private static final int STROKE_WIDTH = 2;
    private static final Paint ovalPaint;
    
    static
    {
        ovalPaint = new Paint();
        ovalPaint.setColor(Color.BLACK);
        ovalPaint.setStyle(Style.STROKE);
        ovalPaint.setStrokeWidth(STROKE_WIDTH);
        ovalPaint.setAntiAlias(true);
    }
    
    public AnimatedGate(float x, float y, float unitPixels)
    {
        super(FRAMES_COUNT, unitPixels);
        setDuration(DURATION);
        setPosition(x, y);
        setSize(6, 6);
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
        
        RectF ref = new RectF(spriteCanvas.getClipBounds());
        // cut out some space for the stroke width
        ref.left += STROKE_WIDTH;
        ref.right -= STROKE_WIDTH;
        ref.top += STROKE_WIDTH;
        ref.bottom -= STROKE_WIDTH;
        float rw = ref.width();
        // make the 'square' an oval.
        ref.left += rw/4;
        ref.right -= rw/4;
        spriteCanvas.drawArc(ref, 0, 360, true, ovalPaint);
        
        Matrix matrix = new Matrix();
        matrix.setRotate(angle, sprite.getWidth() / 2, sprite.getHeight() / 2);
        matrix.postTranslate(bounds.left, bounds.top);
        c.drawBitmap(sprite, matrix, ovalPaint);
//        c.drawBitmap(split, this.x - this.w, this.y - this.h, ovalPaint);
    }
}
