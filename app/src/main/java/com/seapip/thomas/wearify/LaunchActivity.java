package com.seapip.thomas.wearify;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;

import com.seapip.thomas.wearify.spotify.Service;
import com.seapip.thomas.wearify.wearify.Manager;
import com.seapip.thomas.wearify.wearify.Token;

public class LaunchActivity extends WearableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean isRunning = isServiceRunning(Service.class);
        if (!isRunning) {
            setContentView(R.layout.activity_launch);
        }
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Manager.getToken(LaunchActivity.this, new com.seapip.thomas.wearify.wearify.Callback() {
                    @Override
                    public void onSuccess(Token token) {
                        finish();
                        startActivity(new Intent(LaunchActivity.this, LibraryActivity.class));
                    }

                    @Override
                    public void onError() {
                        finish();
                        startActivity(new Intent(LaunchActivity.this, LoginActivity.class));
                    }
                });
            }
        }, isRunning ? 0 : 2000);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
