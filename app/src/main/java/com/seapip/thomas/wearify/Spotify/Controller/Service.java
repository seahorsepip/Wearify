package com.seapip.thomas.wearify.Spotify.Controller;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.wearable.media.MediaControlConstants;
import android.util.Log;
import android.view.KeyEvent;

import com.seapip.thomas.wearify.NowPlayingActivity;
import com.seapip.thomas.wearify.R;
import com.seapip.thomas.wearify.Spotify.Objects.CurrentlyPlaying;
import com.seapip.thomas.wearify.Spotify.Util;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;

public class Service extends android.app.Service implements Callbacks {
    private static final String ACTION_CMD = "com.seapip.thomas.wearify.ACTION_CMD";
    private static final String CMD_NAME = "CMD_NAME";
    private static final String CMD_DESTROY = "CMD_DESTROY";
    private NotificationManager mNotificationManager;
    private IBinder mBinder;
    private ArrayList<Callbacks> mCallbacks;
    private NativeController mNativeController;
    private ConnectController mConnectController;
    private Controller mCurrentController;
    private Notification.Builder mNotificationBuilder;
    private MediaSession mSession;
    private PlaybackState.Builder mPlaybackStateBuilder;
    private MediaMetadata.Builder mMediaMetadataBuilder;
    private Target mMediaMetadataTarget;

    @Override
    public void onCreate() {
        super.onCreate();
        mSession = new MediaSession(this, "WEARIFY");
        mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                mCurrentController.resume();
            }

            @Override
            public void onPause() {
                mCurrentController.pause();
            }

            @Override
            public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
                if (Intent.ACTION_MEDIA_BUTTON.equals(mediaButtonIntent.getAction())) {
                    KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY) {
                        mCurrentController.resume();
                        return false;
                    } else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                        mCurrentController.pause();
                        return false;
                    }
                }
                return super.onMediaButtonEvent(mediaButtonIntent);
            }


            @Override
            public void onSkipToPrevious() {
                mCurrentController.previous();
            }

            @Override
            public void onSkipToNext() {
                mCurrentController.next();
            }
        });
        PendingIntent sessionActivity = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), NowPlayingActivity.class), 0);
        mSession.setSessionActivity(sessionActivity);
        mPlaybackStateBuilder = new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SKIP_TO_NEXT);
        mSession.setPlaybackState(mPlaybackStateBuilder.build());
        mMediaMetadataBuilder = new MediaMetadata.Builder();
        mSession.setMetadata(mMediaMetadataBuilder.build());
        mMediaMetadataTarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                mMediaMetadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap);
                mSession.setMetadata(mMediaMetadataBuilder.build());
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };
        Bundle sessionExtras = new Bundle();
        sessionExtras.putBoolean(MediaControlConstants.EXTRA_RESERVE_SLOT_SKIP_TO_PREVIOUS, true);
        sessionExtras.putBoolean(MediaControlConstants.EXTRA_RESERVE_SLOT_SKIP_TO_NEXT, true);
        mSession.setExtras(sessionExtras);
        mBinder = new ControllerBinder();
        mCallbacks = new ArrayList<>();
        mNotificationBuilder = new Notification.Builder(this);
        mNativeController = new NativeController(this);
        mCurrentController = mNativeController;
        //mConnectController = new ConnectController(this);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent destroyIntent = new Intent(getApplicationContext(), Service.class);
        destroyIntent.setAction(ACTION_CMD);
        destroyIntent.putExtra(CMD_NAME, CMD_DESTROY);
        mNotificationBuilder.setContentIntent(sessionActivity)
                .setSmallIcon(R.drawable.ic_logo)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .setPriority(Notification.PRIORITY_MAX)
                .setStyle(new Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2).setMediaSession(mSession.getSessionToken()))
                .setDeleteIntent(PendingIntent.getService(getApplicationContext(), 0, destroyIntent, 0));
        Log.e("WEARIFY", "CONTROLLER_SERVICE_CREATED!!!");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("WEARIFY", "CONTROLLER_SERVICE_STARTED!!!");
        if (intent != null) {
            Log.e("WEARIFY", intent.toString());
            Log.e("WEARIFY", intent.getAction() + "-");
            Log.e("WEARIFY", intent.getStringExtra(CMD_NAME) + "-");
            String action = intent.getAction();
            String command = intent.getStringExtra(CMD_NAME);
            if (ACTION_CMD.equals(action)) {
                if (CMD_DESTROY.equals(command)) {
                    Log.e("WEARIFY", "DESTROY!!!");
                    mNativeController.destroy();
                    mSession.setActive(false);
                    stopForeground(false);
                    for (Callbacks callbacks : mCallbacks) {
                        ((Activity) callbacks).finish();
                    }
                }
            }
        }
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
        Log.e("WEARIFY", "CONTROLLER_SERVICE_DESTROYED!!!");
        mNativeController.destroy();
        mSession.setActive(false);
        stopForeground(false);
        super.onDestroy();
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
        mPlaybackStateBuilder.setState(currentlyPlaying.is_playing ?
                PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, 0, 1);
        mSession.setPlaybackState(mPlaybackStateBuilder.build());
        mNotificationBuilder.setOngoing(currentlyPlaying.is_playing);
        mNotificationManager.notify(1337, mNotificationBuilder.build());
        if (currentlyPlaying.is_playing) {
            mSession.setActive(true);
            startService(new Intent(getApplicationContext(), Service.class));
            startForeground(0, null);
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
        mMediaMetadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, currentlyPlaying.item.name.trim());
        mMediaMetadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, Util.names(currentlyPlaying.item.artists).trim());
        mMediaMetadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, null);
        mSession.setMetadata(mMediaMetadataBuilder.build());
        Picasso.with(getApplicationContext()).load(Util.smallestImageUrl(currentlyPlaying.item.album.images)).into(mMediaMetadataTarget);
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
