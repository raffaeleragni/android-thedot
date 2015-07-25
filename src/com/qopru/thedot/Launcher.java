package com.qopru.thedot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Launcher proxy.
 * 
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class Launcher extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(getApplicationContext(), GameActivity.class));
        finish();
    }
}
