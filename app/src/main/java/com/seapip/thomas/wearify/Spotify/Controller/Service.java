package com.seapip.thomas.wearify.Spotify.Controller;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.seapip.thomas.wearify.R;
import com.seapip.thomas.wearify.Spotify.Objects.CurrentlyPlaying;

import java.util.ArrayList;

public class Service extends android.app.Service implements Callbacks {
    NotificationManager mNotificationManager;
    private IBinder mBinder;
    private ArrayList<Callbacks> mCallbacks;
    private NativeController mNativeController;
    private ConnectController mConnectController;
    private Controller mCurrentController;
    private Notification.Builder mNotificationBuilder;

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new ControllerBinder();
        mCallbacks = new ArrayList<>();
        mNotificationBuilder = new Notification.Builder(this);
        mNativeController = new NativeController(this);
        mCurrentController = mNativeController;
        //mConnectController = new ConnectController(this);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationBuilder.setContentIntent(null)
                .setSmallIcon(R.drawable.ic_play_arrow_black_24dp)
                .setTicker("Uhm?")
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText("Some kind of song I guess???");
        Log.e("WEARIFY", "CONTROLLER_SERVICE_CREATED!!!");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("WEARIFY", "CONTROLLER_SERVICE_STARTED!!!");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setCallbacks(Callbacks callbacks) {
        mCallbacks.add(callbacks);
    }

    public void unsetCallbacks(Callbacks callbacks) {
        mCallbacks.remove(callbacks);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //mNativeController.destroy();
        //mConnectController.destroy();
        Log.e("WEARIFY", "CONTROLLER_SERVICE_DESTROYED!!!");
    }

    public Controller getController() {
        return mCurrentController;
    }

    public void onPlaybackBind(CurrentlyPlaying currentlyPlaying) {
        for (Callbacks callbacks : mCallbacks) {
            callbacks.onPlaybackState(currentlyPlaying);
            callbacks.onPlaybackShuffle(currentlyPlaying);
            callbacks.onPlaybackRepeat(currentlyPlaying);
            callbacks.onPlaybackPrevious(currentlyPlaying);
            callbacks.onPlaybackNext(currentlyPlaying);
            callbacks.onPlaybackVolume(currentlyPlaying);
            callbacks.onPlaybackSeek(currentlyPlaying);
            callbacks.onPlaybackMetaData(currentlyPlaying);
            callbacks.onPlaybackDevice(currentlyPlaying);
        }
    }

    @Override
    public void onPlaybackState(CurrentlyPlaying currentlyPlaying) {
        mNotificationBuilder.setOngoing(currentlyPlaying.is_playing);
        mNotificationManager.notify(1337, mNotificationBuilder.build());
        if(currentlyPlaying.is_playing) {
            startService(new Intent(getApplicationContext(), Service.class));
            startForeground(0, null);
        } else {
            stopForeground(false);
        }
        for (Callbacks callbacks : mCallbacks) {
            callbacks.onPlaybackState(currentlyPlaying);
        }
    }

    @Override
    public void onPlaybackShuffle(CurrentlyPlaying currentlyPlaying) {
        for (Callbacks callbacks : mCallbacks) {
            callbacks.onPlaybackShuffle(currentlyPlaying);
        }
    }

    @Override
    public void onPlaybackRepeat(CurrentlyPlaying currentlyPlaying) {
        for (Callbacks callbacks : mCallbacks) {
            callbacks.onPlaybackRepeat(currentlyPlaying);
        }
    }

    @Override
    public void onPlaybackPrevious(CurrentlyPlaying currentlyPlaying) {
        for (Callbacks callbacks : mCallbacks) {
            callbacks.onPlaybackPrevious(currentlyPlaying);
        }
    }

    @Override
    public void onPlaybackNext(CurrentlyPlaying currentlyPlaying) {
        for (Callbacks callbacks : mCallbacks) {
            callbacks.onPlaybackNext(currentlyPlaying);
        }
    }

    @Override
    public void onPlaybackVolume(CurrentlyPlaying currentlyPlaying) {
        for (Callbacks callbacks : mCallbacks) {
            callbacks.onPlaybackVolume(currentlyPlaying);
        }
    }

    @Override
    public void onPlaybackSeek(CurrentlyPlaying currentlyPlaying) {
        for (Callbacks callbacks : mCallbacks) {
            callbacks.onPlaybackSeek(currentlyPlaying);
        }
    }

    @Override
    public void onPlaybackMetaData(CurrentlyPlaying currentlyPlaying) {
        for (Callbacks callbacks : mCallbacks) {
            callbacks.onPlaybackMetaData(currentlyPlaying);
        }
    }

    @Override
    public void onPlaybackDevice(CurrentlyPlaying currentlyPlaying) {
        for (Callbacks callbacks : mCallbacks) {
            callbacks.onPlaybackDevice(currentlyPlaying);
        }
    }

    public class ControllerBinder extends Binder {
        public Service getServiceInstance() {
            return Service.this;
        }
    }
}
