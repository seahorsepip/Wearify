package com.seapip.thomas.wearify.Spotify.Controller;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
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
import android.view.KeyEvent;

import com.seapip.thomas.wearify.DeviceActivity;
import com.seapip.thomas.wearify.NowPlayingActivity;
import com.seapip.thomas.wearify.R;
import com.seapip.thomas.wearify.Spotify.Callback;
import com.seapip.thomas.wearify.Spotify.Objects.CurrentlyPlaying;
import com.seapip.thomas.wearify.Spotify.Objects.Paging;
import com.seapip.thomas.wearify.Spotify.Objects.PlaylistTrack;
import com.seapip.thomas.wearify.Spotify.Objects.Track;
import com.seapip.thomas.wearify.Spotify.Objects.Transfer;
import com.seapip.thomas.wearify.Spotify.Util;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;

import retrofit2.Call;

import static com.seapip.thomas.wearify.Spotify.Manager.getService;

public class Service extends android.app.Service {
    final static public int NATIVE_CONTROLLER = 1;
    final static public int CONNECT_CONTROLLER = 2;
    final static public int BLUETOOTH_CONTROLLER = 3;
    private static final String ACTION_CMD = "com.seapip.thomas.wearify.ACTION_CMD";
    private static final String CMD_NAME = "CMD_NAME";
    private static final String CMD_DESTROY = "CMD_DESTROY";
    private NotificationManager mNotificationManager;
    private IBinder mBinder;
    private ArrayList<Controller.Callbacks> mCallbacks;
    private NativeController mNativeController;
    private ConnectController mConnectController;
    private int mCurrentControllerId;
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
                getController(new Callback<Controller>() {
                    @Override
                    public void onSuccess(Controller controller) {
                        controller.resume();
                    }
                });
            }

            @Override
            public void onPause() {
                getController(new Callback<Controller>() {
                    @Override
                    public void onSuccess(Controller controller) {
                        controller.pause();
                    }
                });
            }

            @Override
            public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
                if (Intent.ACTION_MEDIA_BUTTON.equals(mediaButtonIntent.getAction())) {
                    KeyEvent event = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY) {
                        getController(new Callback<Controller>() {
                            @Override
                            public void onSuccess(Controller controller) {
                                controller.resume();
                            }
                        });
                        return false;
                    } else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                        getController(new Callback<Controller>() {
                            @Override
                            public void onSuccess(Controller controller) {
                                controller.pause();
                            }
                        });
                        return false;
                    }
                }
                return super.onMediaButtonEvent(mediaButtonIntent);
            }


            @Override
            public void onSkipToPrevious() {
                getController(new Callback<Controller>() {
                    @Override
                    public void onSuccess(Controller controller) {
                        controller.previous();
                    }
                });
            }

            @Override
            public void onSkipToNext() {
                getController(new Callback<Controller>() {
                    @Override
                    public void onSuccess(Controller controller) {
                        controller.next();
                    }
                });
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
        Callbacks callbacks = new Callbacks() {
            @Override
            public void onPlaybackBind(CurrentlyPlaying currentlyPlaying, int controllerId) {
                if (mCurrentControllerId == controllerId) {
                    for (Controller.Callbacks callbacks : mCallbacks) {
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
            }

            @Override
            public void onPlaybackState(CurrentlyPlaying currentlyPlaying, int controllerId) {
                if (mCurrentControllerId == controllerId) {
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
                    for (Controller.Callbacks callbacks : mCallbacks) {
                        callbacks.onPlaybackState(currentlyPlaying);
                    }

                    if (mCurrentControllerId != CONNECT_CONTROLLER) {
                        mConnectController.setInterval(currentlyPlaying.is_playing ? 0 : 3000);
                    }
                } else if (controllerId == CONNECT_CONTROLLER && currentlyPlaying.device != null
                        && currentlyPlaying.device.is_active && currentlyPlaying.is_playing) {
                    Controller controller = getController();
                    if (controller != null) {
                        controller.pause();
                    }
                    mCurrentControllerId = CONNECT_CONTROLLER;
                    getController().bind();
                }
            }

            @Override
            public void onPlaybackShuffle(CurrentlyPlaying currentlyPlaying, int controllerId) {
                if (mCurrentControllerId == controllerId) {
                    for (Controller.Callbacks callbacks : mCallbacks) {
                        callbacks.onPlaybackShuffle(currentlyPlaying);
                    }
                }
            }

            @Override
            public void onPlaybackRepeat(CurrentlyPlaying currentlyPlaying, int controllerId) {
                if (mCurrentControllerId == controllerId) {
                    for (Controller.Callbacks callbacks : mCallbacks) {
                        callbacks.onPlaybackRepeat(currentlyPlaying);
                    }
                }
            }

            @Override
            public void onPlaybackPrevious(CurrentlyPlaying currentlyPlaying, int controllerId) {
                if (mCurrentControllerId == controllerId) {
                    for (Controller.Callbacks callbacks : mCallbacks) {
                        callbacks.onPlaybackPrevious(currentlyPlaying);
                    }
                }
            }

            @Override
            public void onPlaybackNext(CurrentlyPlaying currentlyPlaying, int controllerId) {
                if (mCurrentControllerId == controllerId) {
                    for (Controller.Callbacks callbacks : mCallbacks) {
                        callbacks.onPlaybackNext(currentlyPlaying);
                    }
                }
            }

            @Override
            public void onPlaybackVolume(CurrentlyPlaying currentlyPlaying, int controllerId) {
                if (mCurrentControllerId == controllerId) {
                    for (Controller.Callbacks callbacks : mCallbacks) {
                        callbacks.onPlaybackVolume(currentlyPlaying);
                    }
                }
            }

            @Override
            public void onPlaybackSeek(CurrentlyPlaying currentlyPlaying, int controllerId) {
                if (mCurrentControllerId == controllerId) {
                    for (Controller.Callbacks callbacks : mCallbacks) {
                        callbacks.onPlaybackSeek(currentlyPlaying);
                    }
                }
            }

            @Override
            public void onPlaybackMetaData(CurrentlyPlaying currentlyPlaying, int controllerId) {
                if (mCurrentControllerId == controllerId) {
                    mMediaMetadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, currentlyPlaying.item.name.trim());
                    mMediaMetadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, Util.names(currentlyPlaying.item.artists).trim());
                    mMediaMetadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, null);
                    mSession.setMetadata(mMediaMetadataBuilder.build());
                    Picasso.with(getApplicationContext()).load(Util.smallestImageUrl(currentlyPlaying.item.album.images)).into(mMediaMetadataTarget);
                    for (Controller.Callbacks callbacks : mCallbacks) {
                        callbacks.onPlaybackMetaData(currentlyPlaying);
                    }
                }
            }

            @Override
            public void onPlaybackDevice(CurrentlyPlaying currentlyPlaying, int controllerId) {
                if (mCurrentControllerId == controllerId) {
                    for (Controller.Callbacks callbacks : mCallbacks) {
                        callbacks.onPlaybackDevice(currentlyPlaying);
                    }
                }
            }
        };
        mNativeController = new NativeController(this, callbacks);
        mConnectController = new ConnectController(this, callbacks);
        mConnectController.setInterval(3000);
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            String command = intent.getStringExtra(CMD_NAME);
            if (ACTION_CMD.equals(action)) {
                if (CMD_DESTROY.equals(command)) {
                    mNativeController.destroy();
                    mConnectController.destroy();
                    mSession.setActive(false);
                    stopForeground(false);
                    for (Controller.Callbacks callbacks : mCallbacks) {
                        ((Activity) callbacks).finish();
                    }
                    stopSelf();
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setCallbacks(Controller.Callbacks callbacks) {
        mCallbacks.add(callbacks);
    }

    public void unsetCallbacks(Controller.Callbacks callbacks) {
        mCallbacks.remove(callbacks);
    }

    @Override
    public void onDestroy() {
        mNativeController.destroy();
        mConnectController.destroy();
        mSession.setActive(false);
        stopForeground(false);
        super.onDestroy();
    }

    public void play(final String uris, final String contextUri, final int position,
                     final boolean shuffleState, final String repeatState, final int positionMs) {
        getController(new Callback<Controller>() {
            @Override
            public void onSuccess(Controller controller) {
                controller.play(uris, contextUri, position, shuffleState, repeatState, positionMs);
            }

            @Override
            public void onError() {
                Intent intent = new Intent(Service.this, DeviceActivity.class);
                intent.putExtra("uris", uris);
                intent.putExtra("contextUri", contextUri);
                intent.putExtra("position", position);
                intent.putExtra("shuffleState", shuffleState);
                intent.putExtra("repeatState", repeatState);
                intent.putExtra("positionMs", positionMs);
                startActivity(intent);
            }
        });
    }

    public Controller getController() {
        switch (mCurrentControllerId) {
            case NATIVE_CONTROLLER:
                return mNativeController;
            case CONNECT_CONTROLLER:
                return mConnectController;
            case BLUETOOTH_CONTROLLER:
                //TODO: implement bluetooth controller
                return null;
        }
        return null;
    }

    public void getController(Callback<Controller> callback) {
        Controller controller = getController();
        if (controller != null) {
            callback.onSuccess(controller);
            return;
        }
        callback.onError();
    }

    public void setController(final int controllerId, final String deviceId,
                              final Callback<Controller> callback) {
        if (controllerId == NATIVE_CONTROLLER && mCurrentControllerId == NATIVE_CONTROLLER) {
            //Transferring from current device to current device...
            if(callback != null) {
                callback.onSuccess(getController());
            }
            return;
        } else if (controllerId == CONNECT_CONTROLLER && mCurrentControllerId == CONNECT_CONTROLLER) {
            //Transferring between connected devices
            if (deviceId == null) {
                return;
            }
            getService(this, new Callback<com.seapip.thomas.wearify.Spotify.Service>() {
                @Override
                public void onSuccess(com.seapip.thomas.wearify.Spotify.Service service) {
                    Transfer transfer = new Transfer();
                    transfer.device_ids = new String[1];
                    transfer.device_ids[0] = deviceId;
                    Call<Void> call = service.transfer(transfer);
                    call.enqueue(new retrofit2.Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                            mCurrentControllerId = CONNECT_CONTROLLER;
                            callback.onSuccess(getController());
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            callback.onError();
                        }
                    });
                }
            });
            if(callback != null) {
                callback.onSuccess(getController());
            }
            return;
        }

        getController(new Callback<Controller>() {
            @Override
            public void onSuccess(final Controller controller) {
                //Transferring to a different controller
                controller.getPlayback(new Callback<CurrentlyPlaying>() {
                    @Override
                    public void onSuccess(final CurrentlyPlaying currentlyPlaying) {
                        controller.pause();
                        mConnectController.setInterval(0);
                        mCurrentControllerId = controllerId;
                        getController(new Callback<Controller>() {
                            @Override
                            public void onSuccess(final Controller controller) {
                                if (currentlyPlaying.context == null) {
                                    //Workaround: https://github.com/spotify/web-api/issues/565
                                    if (currentlyPlaying.item != null) {
                                        controller.play(currentlyPlaying.item.uri, null, 0,
                                                currentlyPlaying.shuffle_state,
                                                currentlyPlaying.repeat_state,
                                                currentlyPlaying.progress_ms);
                                    } else {
                                        //Let's resume playback with whatever is on the current controller...
                                        controller.resume();
                                    }
                                    controller.bind();
                                } else if (currentlyPlaying.context.uri.contains(":playlist:")) {
                                    getPlaylistTrackNumber(Service.this, currentlyPlaying.context.uri,
                                            currentlyPlaying.item.uri, 50, 0,
                                            new Callback<Integer>() {
                                                @Override
                                                public void onSuccess(final Integer position) {
                                                    controller.play(null, currentlyPlaying.context.uri,
                                                            position,
                                                            currentlyPlaying.shuffle_state,
                                                            currentlyPlaying.repeat_state,
                                                            currentlyPlaying.progress_ms);
                                                }
                                            });
                                } else if (currentlyPlaying.context.uri.contains(":album:")) {
                                    getAlbumTrackNumber(Service.this, currentlyPlaying.context.uri,
                                            currentlyPlaying.item.uri, 50, 0,
                                            new Callback<Integer>() {
                                                @Override
                                                public void onSuccess(Integer position) {
                                                    controller.play(null, currentlyPlaying.context.uri, position,
                                                            currentlyPlaying.shuffle_state,
                                                            currentlyPlaying.repeat_state,
                                                            currentlyPlaying.progress_ms);
                                                }
                                            });
                                }

                                if (mCurrentControllerId == CONNECT_CONTROLLER) {
                                    mConnectController.setInterval(3000);
                                }
                                if(callback != null) {
                                    callback.onSuccess(getController());
                                }
                            }
                        });
                    }
                });
            }

            @Override
            public void onError() {
                //Transferring to a new controller
                mCurrentControllerId = controllerId;
                if (callback != null) {
                    callback.onSuccess(getController());
                }
            }
        });
    }

    private void getPlaylistTrackNumber(final Context context, final String contextUri,
                                        final String trackUri, final int limit,
                                        final int offset, final Callback<Integer> callback) {
        getService(context, new Callback<com.seapip.thomas.wearify.Spotify.Service>() {
            @Override
            public void onSuccess(com.seapip.thomas.wearify.Spotify.Service service) {
                Call<Paging<PlaylistTrack>> call = service.getPlaylistTracks(
                        contextUri.split(":")[2], contextUri.split(":")[4],
                        "items(track.uri),total,offset", limit, offset, "from_token");
                call.enqueue(new retrofit2.Callback<Paging<PlaylistTrack>>() {
                    @Override
                    public void onResponse(Call<Paging<PlaylistTrack>> call,
                                           retrofit2.Response<Paging<PlaylistTrack>> response) {
                        if (response.isSuccessful()) {
                            Paging<PlaylistTrack> playlistTracks = response.body();
                            int x = 0;
                            for (PlaylistTrack playlistTrack : playlistTracks.items) {
                                if (playlistTrack.track.uri.equals(trackUri)) {
                                    callback.onSuccess(playlistTracks.offset + x);
                                    return;
                                }
                                x++;
                            }
                            if (playlistTracks.total > playlistTracks.offset + 50) {
                                getPlaylistTrackNumber(context, contextUri, trackUri, limit,
                                        playlistTracks.offset + limit, callback);
                                return;
                            }
                            callback.onError();
                        }
                    }

                    @Override
                    public void onFailure(Call<Paging<PlaylistTrack>> call, Throwable t) {

                    }
                });
            }
        });
    }

    private void getAlbumTrackNumber(final Context context, final String contextUri,
                                     final String trackUri, final int limit,
                                     final int offset, final Callback<Integer> callback) {
        getService(context, new Callback<com.seapip.thomas.wearify.Spotify.Service>() {
            @Override
            public void onSuccess(com.seapip.thomas.wearify.Spotify.Service service) {
                Call<Paging<Track>> call = service.getAlbumTracks(contextUri.split(":")[2], limit,
                        offset, "from_token");
                call.enqueue(new retrofit2.Callback<Paging<Track>>() {
                    @Override
                    public void onResponse(Call<Paging<Track>> call,
                                           retrofit2.Response<Paging<Track>> response) {
                        if (response.isSuccessful()) {
                            Paging<Track> albumTracks = response.body();
                            int x = 0;
                            for (Track track : albumTracks.items) {
                                if (track.uri.equals(trackUri)) {
                                    callback.onSuccess(albumTracks.offset + x);
                                    return;
                                }
                                x++;
                            }
                            if (albumTracks.total > albumTracks.offset + 50) {
                                getPlaylistTrackNumber(context, contextUri, trackUri, limit,
                                        albumTracks.offset + limit, callback);
                                return;
                            }
                            callback.onError();
                        }
                    }

                    @Override
                    public void onFailure(Call<Paging<Track>> call, Throwable t) {

                    }
                });
            }
        });
    }

    interface Callbacks {
        void onPlaybackBind(CurrentlyPlaying currentlyPlaying, int controllerId);

        void onPlaybackState(CurrentlyPlaying currentlyPlaying, int controllerId);

        void onPlaybackShuffle(CurrentlyPlaying currentlyPlaying, int controllerId);

        void onPlaybackRepeat(CurrentlyPlaying currentlyPlaying, int controllerId);

        void onPlaybackPrevious(CurrentlyPlaying currentlyPlaying, int controllerId);

        void onPlaybackNext(CurrentlyPlaying currentlyPlaying, int controllerId);

        void onPlaybackVolume(CurrentlyPlaying currentlyPlaying, int controllerId);

        void onPlaybackSeek(CurrentlyPlaying currentlyPlaying, int controllerId);

        void onPlaybackMetaData(CurrentlyPlaying currentlyPlaying, int controllerId);

        void onPlaybackDevice(CurrentlyPlaying currentlyPlaying, int controllerId);
    }

    public class ControllerBinder extends Binder {
        public Service getServiceInstance() {
            return Service.this;
        }
    }
}
