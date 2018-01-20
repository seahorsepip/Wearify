package com.seapip.thomas.wearify;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.view.View;

import com.seapip.thomas.wearify.spotify.Service;
import com.seapip.thomas.wearify.wearify.Token;

import static com.seapip.thomas.wearify.wearify.Manager.getToken;

public class LaunchActivity extends WearableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        getToken(LaunchActivity.this, new com.seapip.thomas.wearify.wearify.Callback() {
            @Override
            public void onSuccess(Token token) {
                Intent intent = new Intent(LaunchActivity.this, LibraryActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                finish();
                startActivity(intent);
            }

            @Override
            public void onError() {
                Intent intent = new Intent(LaunchActivity.this, LoginAltActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                finish();
                startActivity(intent);
            }
        });
        /*
        boolean isRunning = isServiceRunning(Service.class);
        if (!isRunning) {
            findViewById(R.id.logo).setVisibility(View.VISIBLE);
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getToken(LaunchActivity.this, new com.seapip.thomas.wearify.wearify.Callback() {
                    @Override
                    public void onSuccess(Token token) {
                        Intent intent = new Intent(LaunchActivity.this, LibraryActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                        finish();
                        startActivity(intent);
                    }

                    @Override
                    public void onError() {
                        Intent intent = new Intent(LaunchActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                        finish();
                        startActivity(intent);
                    }
                });
            }
        }, isRunning ? 0 : 1000);
        */
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
