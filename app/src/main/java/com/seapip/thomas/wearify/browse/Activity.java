package com.seapip.thomas.wearify.browse;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.seapip.thomas.wearify.NavigationDrawerAdapter;
import com.seapip.thomas.wearify.NowPlayingActivity;
import com.seapip.thomas.wearify.R;
import com.seapip.thomas.wearify.Callback;
import com.seapip.thomas.wearify.spotify.Service;
import com.seapip.thomas.wearify.spotify.controller.Controller;
import com.seapip.thomas.wearify.spotify.objects.CurrentlyPlaying;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.seapip.thomas.wearify.spotify.Service.INTERVAL;

public class Activity extends WearableActivity implements Controller.Callbacks {

    private boolean mIsBound;
    private Service mService;
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            mIsBound = false;
            mService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            mIsBound = true;
            mService = ((Service.ControllerBinder) service).getServiceInstance();
            mService.setCallbacks(Activity.this);
            mService.getController(new Callback<Controller>() {
                @Override
                public void onSuccess(Controller controller) {
                    controller.bind();
                }

                @Override
                public void onError() {

                }
            });
        }
    };
    private WearableDrawerLayout mLayout;
    private WearableActionDrawer mActionDrawer;
    private ImageView mActionImage;
    private Drawable mDrawablePlay;
    private AnimatedVectorDrawable mDrawablePlaying;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setDrawers(final WearableDrawerLayout layout,
                           WearableNavigationDrawer navigationDrawer,
                           final WearableActionDrawer actionDrawer,
                           int position) {
        // Main Wearable Drawer Layout that wraps all content
        mLayout = layout;
        mLayout.peekDrawer(Gravity.BOTTOM);

        // Top Navigation Drawer
        if (navigationDrawer != null) {
            NavigationDrawerAdapter adapter = new NavigationDrawerAdapter(this);
            navigationDrawer.setAdapter(adapter);
            if (position > 0) {
                navigationDrawer.setCurrentItem(1, false);
            }
            adapter.enabledSelect();
        }

        // Bottom Action Drawer
        if (actionDrawer != null) {
            mActionDrawer = actionDrawer;
            mActionDrawer.setVisibility(INVISIBLE);
            mActionDrawer.setShouldPeekOnScrollDown(true);
            mDrawablePlay = getDrawable(R.drawable.ic_play_arrow_black_24dp);
            mDrawablePlay.setTint(Color.argb(180, 0, 0, 0));
            mDrawablePlaying = (AnimatedVectorDrawable) getDrawable(R.drawable.ic_audio_waves_animated);
            mDrawablePlaying.setTint(Color.argb(180, 0, 0, 0));
            Menu menu = mActionDrawer.getMenu();
            menu.add("Now Playing").setIcon(null);
            LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            final LinearLayout view = (LinearLayout) layoutInflater.inflate(R.layout.action_drawer_view, null);
            mActionDrawer.setPeekContent(view);
            mActionImage = (ImageView) view.findViewById(R.id.action_image);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Activity.this, NowPlayingActivity.class);
                    startActivityForResult(intent, 0);
                }
            });
        }
    }

    public void setGradientOverlay(RecyclerView recyclerView, final ImageView imageView) {
        Display display = getWindowManager().getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);
        final Bitmap overlayBitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888);
        final Canvas overlayCanvas = new Canvas(overlayBitmap);
        final int overlayColorStop = Color.parseColor("#141414");
        final Paint gradient = new Paint();
        final Paint solid = new Paint();
        solid.setColor(overlayColorStop);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int offset = recyclerView.computeVerticalScrollOffset();
                if (offset < size.y) {
                    overlayCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    Shader shader = new LinearGradient(0, -offset - size.y * 2, 0, size.y - offset, Color.TRANSPARENT,
                            overlayColorStop, Shader.TileMode.CLAMP);
                    gradient.setShader(shader);
                    overlayCanvas.drawRect(0, -offset, size.x, (float) size.y - offset, gradient);
                    overlayCanvas.drawRect(0, (float) size.y - offset, size.x, size.y, solid);
                } else {
                    overlayCanvas.drawColor(overlayColorStop);
                }
                imageView.setImageBitmap(overlayBitmap);

            }
        });
    }

    public Service getService() {
        return mService;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent mIntent = new Intent(this, Service.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mIsBound) {
            getService().getController(new Callback<Controller>() {
                @Override
                public void onSuccess(Controller controller) {
                    controller.setInterval(INTERVAL);
                    controller.requestPlayback();
                }

                @Override
                public void onError() {

                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mIsBound) {
            getService().getController(new Callback<Controller>() {
                @Override
                public void onSuccess(Controller controller) {
                    controller.setInterval(30000);
                }

                @Override
                public void onError() {

                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (mIsBound) {
            mService.unsetCallbacks(this);
            unbindService(mConnection);
        }
        super.onDestroy();
    }

    @Override
    public void onPlaybackState(CurrentlyPlaying currentlyPlaying) {
        if (currentlyPlaying.item == null) {
            mActionDrawer.setVisibility(INVISIBLE);
        } else {
            mActionDrawer.setVisibility(VISIBLE);
            if (currentlyPlaying.is_playing) {
                mActionImage.setImageDrawable(mDrawablePlaying);
                mDrawablePlaying.start();
                mLayout.peekDrawer(Gravity.BOTTOM);
            } else {
                mActionImage.setImageDrawable(mDrawablePlay);
            }
        }
    }

    @Override
    public void onPlaybackShuffle(CurrentlyPlaying currentlyPlaying) {

    }

    @Override
    public void onPlaybackRepeat(CurrentlyPlaying currentlyPlaying) {

    }

    @Override
    public void onPlaybackPrevious(CurrentlyPlaying currentlyPlaying) {

    }

    @Override
    public void onPlaybackNext(CurrentlyPlaying currentlyPlaying) {

    }

    @Override
    public void onPlaybackVolume(CurrentlyPlaying currentlyPlaying) {

    }

    @Override
    public void onPlaybackSeek(CurrentlyPlaying currentlyPlaying) {

    }

    @Override
    public void onPlaybackMetaData(CurrentlyPlaying currentlyPlaying) {

    }

    @Override
    public void onPlaybackDevice(CurrentlyPlaying currentlyPlaying) {
        if (currentlyPlaying.device.is_active) {
            mActionDrawer.setVisibility(VISIBLE);
            mLayout.peekDrawer(Gravity.BOTTOM);
        } else {
            mActionDrawer.setVisibility(INVISIBLE);
        }
    }

    @Override
    public void onPlaybackBuffering() {
        mActionDrawer.setVisibility(VISIBLE);
        mLayout.peekDrawer(Gravity.BOTTOM);
    }
}
