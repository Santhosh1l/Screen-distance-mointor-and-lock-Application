package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class LockScreenReceiver extends BroadcastReceiver {
    public static final String LOCK_SCREEN_ACTION = "com.example.myapplication.LOCK_SCREEN_ACTION";
    private boolean wasReceived = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (LOCK_SCREEN_ACTION.equals(intent.getAction())) {
            wasReceived = true;
        }
    }

    public boolean wasReceived() {
        return wasReceived;
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(LOCK_SCREEN_ACTION);
        return filter;
    }
}

