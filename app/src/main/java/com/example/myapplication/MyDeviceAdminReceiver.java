package com.example.myapplication;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        // Called when the application is given device administrator privileges
        Toast.makeText(context, "Device Admin: Enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        // Called when the application loses device administrator privileges
        Toast.makeText(context, "Device Admin: Disabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        super.onPasswordChanged(context, intent);
        // Called when the user changes their device password
        Toast.makeText(context, "Device Admin: Password Changed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        super.onPasswordFailed(context, intent);
        // Called when the user enters an incorrect password
        Toast.makeText(context, "Device Admin: Password Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        super.onPasswordSucceeded(context, intent);
        // Called when the user successfully enters their password
        Toast.makeText(context, "Device Admin: Password Succeeded", Toast.LENGTH_SHORT).show();
    }
}
