/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.qopru.thedot;

import android.app.IntentService;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class SoundService extends IntentService
{
    public static final String SOUND = "sound";
    
    private static final int[] SOUND_IDS = new int[]{};
    private static SoundPool pool;
    private static Map<Integer, Integer> sounds;
    
    public SoundService()
    {
        super("SoundService");
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Preload all sounds in a pool
        if (pool == null)
        {
            sounds = new HashMap<Integer, Integer>();
            pool = new SoundPool(SOUND_IDS.length, AudioManager.STREAM_NOTIFICATION, 100);
            int ct = 1;
            for (int id: SOUND_IDS)
                sounds.put(id, pool.load(this, id, ct++));
        }
    }
    
    @Override
    protected void onHandleIntent(Intent intent)
    {
        if (intent == null || !intent.hasExtra(SOUND) || pool == null || sounds == null)
            return;
        
        int sound = intent.getIntExtra(SOUND, 0);
        if (sounds.containsKey(sound))
            pool.play(sounds.get(sound), 1, 1, 1, 0, 1f);
    }
}
