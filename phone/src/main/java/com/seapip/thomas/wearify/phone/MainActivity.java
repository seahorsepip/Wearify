package com.seapip.thomas.wearify.phone;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    KeyguardManager.KeyguardLock mKeyguardLock;
    PowerManager.WakeLock mWakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        mKeyguardLock = km.newKeyguardLock("MyKeyguardLock");
        //mKeyguardLock.disableKeyguard();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "MyWakeLock");
        //mWakeLock.acquire();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Intent launcher = new Intent(Intent.ACTION_VIEW, Uri.parse("spotify:user:spotifycharts:playlist:37i9dQZEVXbLiRSasKsNU9:play"));
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setAction(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
        intent.setData(Uri.parse("spotify:user:spotifycharts:playlist:37i9dQZEVXbLiRSasKsNU9:play"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        //startActivity(intent);

        MediaSessionManager sessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        List<MediaController> controllers = null;
        controllers = sessionManager.getActiveSessions(
                new ComponentName(this, NotificationListener.class));
        Log.e("WEARIFY", "found " + controllers.size() + " controllers");
        if (controllers.size() > 0) {
            controllers.get(0).getTransportControls().pause();
            Log.e("WEARIFY", controllers.get(0).getPackageName());
            Log.e("WEARIFY", controllers.get(0).getMetadata().getString(MediaMetadata.METADATA_KEY_TITLE));
            if (controllers.get(0).getMetadata().keySet() != null) {
                for (String key : controllers.get(0).getMetadata().keySet()) {
                    Log.e("WEARIFY", key);
                }
            }
        }
    }
}
