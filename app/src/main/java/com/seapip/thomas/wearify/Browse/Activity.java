package com.seapip.thomas.wearify.Browse;

import android.content.Intent;
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
import com.seapip.thomas.wearify.Spotify.Callback;
import com.seapip.thomas.wearify.Spotify.CurrentlyPlaying;
import com.seapip.thomas.wearify.Spotify.Manager;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class Activity extends WearableActivity {

    private Callback<CurrentlyPlaying> mPlaybackCallback;
    private Runnable mRunnable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setDrawers(WearableDrawerLayout layout,
                           WearableNavigationDrawer navigationDrawer,
                           final WearableActionDrawer actionDrawer,
                           int position) {
        // Main Wearable Drawer Layout that wraps all content
        layout.peekDrawer(Gravity.BOTTOM);

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
            actionDrawer.setShouldPeekOnScrollDown(true);
            Menu menu = actionDrawer.getMenu();
            final Drawable drawablePlay = getDrawable(R.drawable.ic_play_arrow_black_24dp);
            drawablePlay.setTint(Color.argb(180, 0, 0, 0));
            final AnimatedVectorDrawable drawablePlaying = (AnimatedVectorDrawable) getDrawable(R.drawable.ic_audio_waves_animated);
            menu.add("Now Playing").setIcon(null);
            LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            final LinearLayout view = (LinearLayout) layoutInflater.inflate(R.layout.action_drawer_view, null);
            actionDrawer.setPeekContent(view);
            final ImageView actionImage = (ImageView) view.findViewById(R.id.action_image);
            drawablePlaying.setTint(Color.argb(180, 0, 0, 0));
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Activity.this, NowPlayingActivity.class);
                    startActivityForResult(intent, 0);
                }
            });

            mPlaybackCallback = new Callback<CurrentlyPlaying>() {
                @Override
                public void onSuccess(CurrentlyPlaying currentlyPlaying) {
                    if (currentlyPlaying == null || currentlyPlaying.item == null
                            || currentlyPlaying.device == null || !currentlyPlaying.device.is_active) {
                        actionDrawer.setVisibility(INVISIBLE);
                    } else {
                        actionDrawer.setVisibility(VISIBLE);
                        if (currentlyPlaying.is_playing) {
                            actionImage.setImageDrawable(drawablePlaying);
                            drawablePlaying.start();
                        } else {
                            actionImage.setImageDrawable(drawablePlay);
                        }
                    }
                }

                @Override
                public void onError() {
                    actionDrawer.setVisibility(GONE);
                }
            };

            mRunnable = Manager.onPlayback(mPlaybackCallback);
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
                    overlayCanvas.drawRect(0, -offset, size.x, size.y - offset, gradient);
                    overlayCanvas.drawRect(0, size.y - offset, size.x, size.y, solid);
                } else {
                    overlayCanvas.drawColor(overlayColorStop);
                }
                imageView.setImageBitmap(overlayBitmap);

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPlaybackCallback != null) {
            mRunnable = Manager.onPlayback(mPlaybackCallback);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mRunnable != null) {
            Manager.offPlayback(mRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRunnable != null) {
            Manager.offPlayback(mRunnable);
        }
    }
}
