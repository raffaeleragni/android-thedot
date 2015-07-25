package com.qopru.thedot.engine;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Vibrator;
import com.qopru.thedot.U;
import com.qopru.thedot.engine.objects.AnimatedEnemy;
import com.qopru.thedot.engine.objects.AnimatedGate;
import com.qopru.thedot.engine.objects.AnimatedGravityWell;
import com.qopru.thedot.interfaces.GameInitializer;
import com.qopru.thedot.interfaces.GameRenderer;
import com.qopru.thedot.interfaces.GameUpdater;
import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.random;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.List;

/**
 * Logic and engine stuff for the game.
 * 
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class GameEngine
{
    private static final String TAG = "GameEngine";
    
    enum Status {PLAY, PAUSE, COUNTDOWN}
    
    private final int DEFAULT_ORIENTATION;
    Context context;
    Vibrator vibrator;
        
    // Game metrics proportional to screen
    Rect gameBounds;
    float unitPixels;

    public GameEngine(Context context)
    {
        this.context = context;
        DEFAULT_ORIENTATION = U.getDeviceDefaultOrientation(context);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
    
    public void destroy()
    {
        
    }

    public void togglePause()
    {
        switch (status)
        {
            case PAUSE:
                if (countdown > 0)
                    status = Status.COUNTDOWN;
                else
                    status = Status.PLAY;
                break;
            case PLAY:
            case COUNTDOWN:
                    status = Status.PAUSE;
                break;
        }
    }
    
    // -------------------------------------------------------------------------

    private void init(Canvas c)
    {
        Rect bounds = c.getClipBounds();
        
        // Caltulate the unit
        unitPixels = bounds.width() > bounds.height() ?
            bounds.height() / UNIT_FRACTION :
            bounds.width() / UNIT_FRACTION;
        
        // Recalculate bounds
        gameBounds = new Rect(bounds);
        int gw = gameBounds.width(), gh = gameBounds.height();
        // Make a "centering"
        gameBounds.left = -gw / 2;
        gameBounds.top = -gh / 2;
        gameBounds.right = +gw / 2;
        gameBounds.bottom = +gh / 2;
        // Convert into unit measurement
        if (unitPixels != 0)
        {
            gameBounds.left /= unitPixels;
            gameBounds.top /= unitPixels;
            gameBounds.right /= unitPixels;
            gameBounds.bottom /= unitPixels;
        }
        
        updateInit();
        renderInit();
    }
    
    // -------------------------------------------------------------------------
    
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;
    
    private static final int COUNTDOWN_MS = 3000;
    
    private static final float DOT_MASS = 0.005f;
    private static final float DOT_BOUNCE = 0.5f;
    private static final float DOT_SIZE = 1f;
    private static final float TRIGGERING_VIBRATION_BOUNCE_SPEED = 3f;
    private static final float ENDING_ANIMATION_TICKS = 10;
    private static final float DEATH_ANIMATION_TICKS = 10;
    
    private static final float ENEMY_SPEED = 25f;
    private static final float GRAVITY_WELL_MAX = 15f;
    private static final float GRAVITY_WELL_MAXDISTANCE = 50f;
    
    int countdown;
    
    // Player position is intended in 'units', a virtual measurement for this game.
    // Units are later converted into pixels, this is a logical value.
    float playerX;
    float playerY;
    // Player's velocity
    float playerVX;
    float playerVY;
    
    // Proximity with the gate to consider it 'exiting' (in units).
    // Distance is center-to-center.
    private static final float GATE_PROXIMITY = 3.5f;
    // Number of milliseconds to remain in proximity in order to 'exit'
    // 1/4 of a second
    private static final int GATE_EXIT_TIME = 100;
    // Number of milliseconds of actual proximity with the gate
    float gateProximityTime;
    // Current level
    int currentLevel;
    // When this is true, the dot will be 'falling' inside the gate for the
    // last remaining grames, then a new level is called.
    boolean endingAnimation;
    // The progress of the ending animation
    int endingAnimationTick;
    // When this is ture, the dot will be destroyed.
    // This always has precedence to the engingAnimation
    boolean deathAnimation;
    // The progress of the death animation
    int deathAnimationTick;
    
    volatile float timeFactor = 1f;
    
    List<AnimatedGravityWell> gravityWells;
    List<AnimatedEnemy> enemies;
    
    boolean touchedBorderHorizontal;
    boolean touchedBorderVertical;
    
    Status status;
    
    /**
     * Initializes the game update part.
     */
    private void updateInit()
    {
        gravityWells = new ArrayList<AnimatedGravityWell>();
        enemies = new ArrayList<AnimatedEnemy>();
        
        currentLevel = 1;
        loadLevel(currentLevel);
        status = Status.PAUSE;
    }
    
    private void loadLevel(int num)
    {
        status = Status.COUNTDOWN;
        countdown = COUNTDOWN_MS;
        
        endingAnimation = false;
        touchedBorderHorizontal = false;
        touchedBorderVertical = false;
        gateProximityTime = 0;
        endingAnimationTick = 0;
        playerX = 0;
        playerY = 0;
        playerVX = 0;
        playerVY = 0;
        gate = new AnimatedGate(
            (float) (random() * (gameBounds.width() - GATE_SIZE*4) + gameBounds.left + GATE_SIZE*2),
            (float) (random() * (gameBounds.height() - GATE_SIZE*4) + gameBounds.top + GATE_SIZE*2), unitPixels);
        // Handle difficulty based on level number
        // Determine number of obstacles, enemies on fractions of the level
        gravityWells.clear();
        gravityWells.add(new AnimatedGravityWell(GRAVITY_WELL_MAX, 
            (float) (random() * (gameBounds.width() - GATE_SIZE*4) + gameBounds.left + GATE_SIZE*2),
            (float) (random() * (gameBounds.height() - GATE_SIZE*4) + gameBounds.top + GATE_SIZE*2),
            unitPixels));
        enemies.clear();
        for (int i = 0; i < 5; i++)
            enemies.add(new AnimatedEnemy(
                (float) (random() * (gameBounds.width() - GATE_SIZE*4) + gameBounds.left + GATE_SIZE*2),
                (float) (random() * (gameBounds.height() - GATE_SIZE*4) + gameBounds.top + GATE_SIZE*2),
                unitPixels));
    }
    
    private void update(int deltaT)
    {
        if (status == null)
            return;
        switch (status)
        {
            case PLAY:
                updateGame(deltaT);
                break;
            case COUNTDOWN:
                countdown -= deltaT;
                if (countdown <= 0)
                    status = Status.PLAY;
                break;
        }
    }
    
    /**
     * Runs on game loop
     *
     * @param deltaT how much an update is "worth" in milliseconds. This is
     * essential to determine how much velocity to apply having an acceleration.
     * Also to know how much time is passed for things like gate proximity.
     */
    private void updateGame(int deltaT)
    {   
        // Must be a valid gate (exit) in game
        if (gate == null)
            return;
        
        // Modify the deltaT for the time factor: game can slow down or
        // speed up based on this.
        deltaT *= timeFactor;
        float seconds = ((float)deltaT) / 1000f;
        
        float[] sensors = pullSensors();
        float x = -sensors[X];
        float y = sensors[Y];
        
        // For phones and defaulted "portrait" devices, axes are based on device OBJECT, not SCREEN.
        // Invert the axes in this case.
        if (DEFAULT_ORIENTATION == Configuration.ORIENTATION_PORTRAIT)
        {
            x = sensors[Y];
            y = sensors[X];
        }
        
        // While animating the player death, it continues going with the last
        // direction
        if (deathAnimation)
        {
            deathAnimationTick++;
            playerX += playerVX * seconds;
            playerY += playerVY * seconds;
            if (deathAnimationTick > DEATH_ANIMATION_TICKS)
                onDeath();
            return;
        }
        
        // While animating these last frames, move slightly the dot towards the
        // Gate, and reduce the dot size.
        if (endingAnimation)
        {
            endingAnimationTick++;
            playerX += (gate.getX() - playerX) / 2;
            playerY += (gate.getY() - playerY) / 2;
            if (endingAnimationTick > ENDING_ANIMATION_TICKS)
                onExitLevel();
            return;
        }
        
        // Calculate if present, the proximity to the gravity wells and their
        // direction to apply the gravity effect.
        for (AnimatedGravityWell well: gravityWells)
        {
            float wX = well.getX();
            float wY = well.getY();
            float wP = well.getPower();
            float deltaX = wX - playerX;
            float deltaY = wY - playerY;
            double distance = sqrt(pow(deltaX, 2) + pow(deltaY, 2));
            if (distance > GRAVITY_WELL_MAXDISTANCE)
                continue;
            distance++;
            double angle = atan2(deltaY, deltaX);
            x += 1/distance * wP * cos(angle);
            y += 1/distance * wP * sin(angle);
        }
        // Apply acceleration to velocity
        // Acceleration is in m/s^2 so the result in velocity will be m/s.
        // In our case we have a virtual 'unit' that is not necessarily a meter.
        // This means some correction will apply later on.
        // Also mass is taken into account when accelerating.
        playerVX += x/DOT_MASS * seconds;
        playerVY += y/DOT_MASS * seconds;
        
        playerX += playerVX * seconds;
        playerY += playerVY * seconds;
        
        moveEnemies(seconds);
        
        handleEnemyCollision();
        
        handleGateProximity(deltaT);
        
        handleBorderCollision();
    }

    private void moveEnemies(float seconds)
    {
        for (AnimatedEnemy enemy: enemies)
        {
            float deltaX = playerX - enemy.getX();
            float deltaY = playerY - enemy.getY();
            double angle = atan2(deltaY, deltaX);
            // Move using a constant speed
            enemy.setX(enemy.getX() + ENEMY_SPEED * seconds * (float)cos(angle));
            enemy.setY(enemy.getY() + ENEMY_SPEED * seconds * (float)sin(angle));
        }
    }
    
    private void handleGateProximity(int deltaT)
    {
        if (gate == null)
            return;
        
        double dist = sqrt(pow(playerX - gate.getX(), 2) + pow(playerY - gate.getY(), 2));
        if (dist > GATE_PROXIMITY)
        {
            gateProximityTime = 0;
            return;
        }
        
        gateProximityTime += deltaT;
        if (gateProximityTime >= GATE_EXIT_TIME)
            endingAnimation = true;
    }
    
    private void handleEnemyCollision()
    {
        for (AnimatedEnemy enemy: enemies)
        {
            double dist = sqrt(pow(playerX - enemy.getX(), 2) + pow(playerY - enemy.getY(), 2));
            double targetDist = DOT_SIZE/2 + (enemy.getW() > enemy.getH() ? enemy.getW() : enemy.getH())/2;
            if (dist < targetDist)
                onEnemyCollision(enemy);
        }
    }
    
    private void handleBorderCollision()
    {
        // Now detect the collision with the game's borders.
        // If any, the velocity must be reset (or inverted, simulating bouncing)
        // the onBorderCollision is only called when velocity is greather than 0
        // otherwise it means we're "walking" along the border.
        if (playerX < gameBounds.left)
        {
            playerX = gameBounds.left;
            playerVX = -playerVX * DOT_BOUNCE;
            if (!touchedBorderHorizontal)
            {
                onBorderCollision(playerVX);
                touchedBorderHorizontal = true;
            }
        }
        else if (playerX > gameBounds.right)
        {
            playerX = gameBounds.right;
            playerVX = -playerVX * DOT_BOUNCE;
            if (!touchedBorderHorizontal)
            {
                onBorderCollision(playerVX);
                touchedBorderHorizontal = true;
            }
        }
        else
            touchedBorderHorizontal = false;
        
        if (playerY < gameBounds.top)
        {
            playerY = gameBounds.top;
            playerVY = -playerVY * DOT_BOUNCE;
            if (!touchedBorderVertical)
            {
                onBorderCollision(playerVY);
                touchedBorderVertical = true;
            }
        }
        else if (playerY > gameBounds.bottom)
        {
            playerY = gameBounds.bottom;
            playerVY = -playerVY * DOT_BOUNCE;
            if (!touchedBorderVertical)
            {
                onBorderCollision(playerVY);
                touchedBorderVertical = true;
            }
        }
        else
            touchedBorderVertical = false;
    }
    
    private void onBorderCollision(float v)
    {
        // Only vibrate when higher than a certain amount
        if (abs(v) > TRIGGERING_VIBRATION_BOUNCE_SPEED)
            vibrator.vibrate(40);
    }
    
    private void onEnemyCollision(AnimatedEnemy enemy)
    {
        vibrator.vibrate(40);
        deathAnimation = true;
        deathAnimationTick = 0;
    }
    
    /**
     * This happens when the player dies.
     */
    private void onDeath()
    {
        vibrator.vibrate(1000);
        deathAnimation = false;
        currentLevel++;
        loadLevel(currentLevel);
        // TODO
//        status = Status.PAUSE;
    }
    
    /**
     * This happens when the level has been finished by exiting the 'gate'.
     */
    private void onExitLevel()
    {
        currentLevel++;
        loadLevel(currentLevel);
    }
    
    // -------------------------------------------------------------------------
    
    // Fraction to determine how much a unit is.
    // the minimum of either game width or height gets divided by this
    // and thus the game 'unit' is obtained.
    private static final int UNIT_FRACTION = 100;
    
    // Size of the gates (in units)
    private static final int GATE_SIZE = 6;
    
    Paint backgroundPaint;
    Paint playerPaint;
    Paint messagePaint;
    Paint messagePaintBkg;
    Paint paintBlackFill;
    Paint paintWhiteFill;
    Paint paintHelpTextLeft;
    Paint paintHelpTextRight;
    Paint paintHelpTextCenter;

    // Keep an instance of this sprite since it's going to be only one per level
    // anyway.
    AnimatedGate gate;
    
    float fontScale;
    
    AnimatedEnemy helpPageEnemy;
    AnimatedGate helpPageGate;
    AnimatedGravityWell helpPageGravtyWell;
    
    /**
     * Initializes rendering.
     */
    private void renderInit()
    {
        helpPageEnemy = new AnimatedEnemy(-10, -10, unitPixels);
        helpPageGravtyWell = new AnimatedGravityWell(0, -20, -20, unitPixels);
        helpPageGate = new AnimatedGate(10, 10, unitPixels);
        
        fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
        backgroundPaint.setStyle(Style.FILL);
        
        playerPaint = new Paint();
        playerPaint.setColor(Color.BLACK);
        playerPaint.setStyle(Style.FILL);
        playerPaint.setAntiAlias(true);
    
        messagePaintBkg = new Paint();
        messagePaintBkg.setColor(Color.WHITE);
        messagePaintBkg.setStyle(Style.FILL);
        messagePaintBkg.setAntiAlias(true);
        messagePaint = new Paint();
        messagePaint.setColor(Color.BLACK);
        messagePaint.setStyle(Style.FILL);
        messagePaint.setAntiAlias(true);
        messagePaint.setTextSize(18 * fontScale);
        messagePaint.setTextAlign(Paint.Align.CENTER);
            
        paintBlackFill = new Paint();
        paintBlackFill.setColor(Color.BLACK);
        paintBlackFill.setStyle(Style.FILL);
        paintBlackFill.setAntiAlias(true);    
        paintWhiteFill = new Paint();
        paintWhiteFill.setColor(Color.WHITE);
        paintWhiteFill.setStyle(Style.FILL);
        paintWhiteFill.setAntiAlias(true);
        
        paintHelpTextLeft = new Paint();
        paintHelpTextLeft.setColor(Color.BLACK);
        paintHelpTextLeft.setStyle(Style.FILL);
        paintHelpTextLeft.setAntiAlias(true);
        paintHelpTextLeft.setTextSize(16 * fontScale);
        paintHelpTextLeft.setTextAlign(Paint.Align.LEFT);
        paintHelpTextRight = new Paint();
        paintHelpTextRight.setColor(Color.BLACK);
        paintHelpTextRight.setStyle(Style.FILL);
        paintHelpTextRight.setAntiAlias(true);
        paintHelpTextRight.setTextSize(16 * fontScale);
        paintHelpTextRight.setTextAlign(Paint.Align.RIGHT);
        paintHelpTextCenter = new Paint();
        paintHelpTextCenter.setColor(Color.BLACK);
        paintHelpTextCenter.setStyle(Style.FILL);
        paintHelpTextCenter.setAntiAlias(true);
        paintHelpTextCenter.setTextSize(16 * fontScale);
        paintHelpTextCenter.setTextAlign(Paint.Align.CENTER);
    }
    
    /**
     * Rendering loop.
     * Runs on game loop.
     * 
     * @param c canvas where to draw.
     */
    private void render(Canvas c)
    {
        // Don't draw on a null canvas
        if (c == null)
            return;

        Rect bounds = c.getClipBounds();
        int left = bounds.left;
        int top = bounds.top;
        int w = bounds.width();
        int h = bounds.height();
        // Center point
        int centerX = w / 2;
        int centerY = h / 2;
        
        // Player's position in Pixels
        // the logical 0,0 in units corresponds to the center of the screen.
        // That is intented as a cartesian plane with 4 quadrants.
        int playerDotX = centerX + (int)(playerX * unitPixels);
        int playerDotY = centerY + (int)(playerY * unitPixels);
        if (playerDotX < left)
            playerDotX = left;
        if (playerDotY < top)
            playerDotY = top;
        if (playerDotX > w)
            playerDotX = w;
        if (playerDotY > h)
            playerDotY = h;
        
        // Clear screen
        c.drawRect(bounds, backgroundPaint);
        
        // Draw gravity wells, should be only positive
        for (AnimatedGravityWell well: gravityWells)
            well.draw(c);
        
        // Draw enemies
        for (AnimatedEnemy enemy: enemies)
            enemy.draw(c);
        
        // Draw the exit gate
        if (gate != null)
            gate.draw(c);
        
        // Draw player
        float dotSize = DOT_SIZE * unitPixels;
        
        if (deathAnimation)
        {
            dotSize -= dotSize * deathAnimationTick / DEATH_ANIMATION_TICKS;
            c.drawArc(new RectF(playerDotX - dotSize, playerDotY - dotSize, playerDotX + dotSize, playerDotY + dotSize), 0, 360, true, playerPaint);
            return;
        }
        
        if (endingAnimation)
            dotSize -= dotSize * endingAnimationTick / ENDING_ANIMATION_TICKS;
        
        c.drawArc(new RectF(playerDotX - dotSize, playerDotY - dotSize, playerDotX + dotSize, playerDotY + dotSize), 0, 360, true, playerPaint);
        
        // Draw countdown
        if (Status.COUNTDOWN.equals(status))
        {
            String msg = "Start in "+((int)((float)countdown/1000f)+1);
            Rect textBounds = new Rect();
            messagePaint.getTextBounds(msg, 0, msg.length(), textBounds);
            c.drawRect(textBounds, messagePaintBkg);
            c.drawText(msg, centerX, centerY - centerY/2, messagePaint);
        }
        
        if (Status.PAUSE.equals(status))
            drawPauseScreen(c);
    }

    private void drawPauseScreen(Canvas c)
    {
        Rect bounds = c.getClipBounds();
        int w = bounds.width();
        int h = bounds.height();
        int centerX = w / 2;
        int centerY = h / 2;
        
        Rect r = new Rect();
        r.left = centerX - w/2 * 3/4;
        r.top = centerY - h/2 * 3/4;
        r.right = centerX + w/2 * 3/4;
        r.bottom = centerY + h/2 * 3/4;
        c.drawRect(r, paintBlackFill);
        r.left += unitPixels;
        r.top += unitPixels;
        r.right -= unitPixels;
        r.bottom -= unitPixels;
        c.drawRect(r, paintWhiteFill);
        
        float dotSize = DOT_SIZE * unitPixels;
        c.drawArc(new RectF(centerX - dotSize, centerY - dotSize, centerX + dotSize, centerY + dotSize), 0, 360, true, playerPaint);
        
        helpPageEnemy.draw(c);
        helpPageGravtyWell.draw(c);
        helpPageGate.draw(c);
        
        c.drawText("Gravity Well", centerX - 17*unitPixels, centerY - 19*unitPixels, paintHelpTextLeft);
        c.drawText("Enemies", centerX - 7*unitPixels, centerY - 9*unitPixels, paintHelpTextLeft);
        c.drawText("Player", centerX - 3*unitPixels, centerY + 1*unitPixels, paintHelpTextRight);
        c.drawText("Exit Gate", centerX + 5*unitPixels, centerY + 11*unitPixels, paintHelpTextRight);
        
        c.drawText("!! PAUSED !!", centerX, centerY - h/2 * 3/4 + 5 * unitPixels, paintHelpTextCenter);
        c.drawText("TAP THE SCREEN TO RESUME", centerX, centerY + h/2 * 3/4 - 4 * unitPixels, paintHelpTextCenter);
    }
    
    // -------------------------------------------------------------------------
    
    // Just pass the call directly
    GameInitializer initializer = new GameInitializer()
    {
        @Override
        public void init(Canvas c)
        {
            GameEngine.this.init(c);
        }
    };
    
    // Just pass the call directly
    GameUpdater updater = new GameUpdater()
    {
        @Override
        public void update(int deltaT)
        {
            GameEngine.this.update(deltaT);
        }
    };
    
    // Just pass the call directly
    GameRenderer renderer = new GameRenderer()
    {
        @Override
        public void render(Canvas canvas)
        {
            GameEngine.this.render(canvas);
        }
    };
    
    // Runs on some system's loop - called event by system
    MySensorEventListener sensorListener = new MySensorEventListener();
    private class MySensorEventListener implements SensorEventListener
    {
        // This section handles operatons on sensors.
        // It gathers avg, min, max of sensors to be pulled at a later moment
        // and so being reset at that time.
        float[] sensorsAvg;
        int[] sensorsCount;
        float[] sensorsMax;
        float[] sensorsMin;
        
        // default initializer
        {
            reset();
        }
        
        // resets to the original values
        private void reset()
        {
            sensorsAvg = new float[]{0f, 0f, 0f};
            sensorsCount = new int[]{0, 0, 0};
            sensorsMax = new float[]{Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};
            sensorsMin = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        }
    
        @Override
        public void onSensorChanged(SensorEvent event)
        {
            if (event == null || event.values == null || event.values.length < 3)
                return;
            
            synchronized (this)
            {
                for (int i = 0; i < sensorsAvg.length; i++)
                {
                    if (sensorsCount[i] > 0)
                    {
                        float sum = (sensorsAvg[i] * sensorsCount[i] + event.values[i]);
                        sensorsCount[i]++;
                        sensorsAvg[i] = sum / sensorsCount[i];
                    }
                    else
                        sensorsAvg[i] = event.values[i];

                    sensorsMax[i] = event.values[i] > sensorsMax[i] ? event.values[i] : sensorsMax[i];
                    sensorsMin[i] = event.values[i] < sensorsMin[i] ? event.values[i] : sensorsMin[i];
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy)
        {
        }
        
        public float[] pullSensors()
        {
            float[] result = new float[sensorsAvg.length];
            synchronized (this)
            {
                System.arraycopy(sensorsAvg, 0, result, 0, result.length);
                reset();
            }
            return result;
        }
    };
    
    public float[] pullSensors()
    {
        return sensorListener.pullSensors();
    }
    
    public GameInitializer getInitializer()
    {
        return initializer;
    }

    public GameUpdater getUpdater()
    {
        return updater;
    }

    public GameRenderer getRenderer()
    {
        return renderer;
    }

    public SensorEventListener getSensorListener()
    {
        return sensorListener;
    }
}
