package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Add your code here to run after the device has completed booting
            Toast.makeText(context, "Device booted", Toast.LENGTH_SHORT).show();

            // For example, start a service or an activity
            // Intent serviceIntent = new Intent(context, MyService.class);
            // context.startService(serviceIntent);
        }
    }
}