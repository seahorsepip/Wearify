package com.seapip.thomas.wearify;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.seapip.thomas.wearify.browse.Activity;
import com.seapip.thomas.wearify.spotify.Util;
import com.seapip.thomas.wearify.spotify.controller.Controller;
import com.seapip.thomas.wearify.spotify.objects.CurrentlyPlaying;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.seapip.thomas.wearify.spotify.Service.INTERVAL;
import static com.seapip.thomas.wearify.spotify.Util.largestImageUrl;

public class NowPlayingActivity extends Activity implements Controller.Callbacks {

    private boolean mAmbient;
    private WearableDrawerLayout mDrawerLayout;
    private WearableNavigationDrawer mNavigationDrawer;
    private ImageView mBackgroundImage;
    private FrameLayout mControls;
    private ProgressBar mProgressBar;
    private ImageButton mPlay;
    private ImageButton mPrev;
    private ImageButton mNext;
    private ImageButton mVolDown;
    private ImageButton mVolUp;
    private TextView mTitle;
    private TextView mSubTitle;
    private CircularProgressView mProgress;
    private Handler mProgressHandler;
    private WearableActionDrawer mActionDrawer;
    private MenuItem mShuffleMenuItem;
    private MenuItem mRepeatMenuItem;
    private MenuItem mDeviceMenuItem;
    private boolean mIsPlaying = true;
    private boolean mShuffle;
    private String mRepeat;
    private int mVolume;
    private Runnable mProgressRunnable;
    private long mProgressTimestamp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAmbientEnabled();
        setContentView(R.layout.activity_now_playing);

        mNavigationDrawer = findViewById(R.id.top_navigation_drawer);
        mActionDrawer = findViewById(R.id.bottom_action_drawer);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mBackgroundImage = findViewById(R.id.background_image);
        mControls = findViewById(R.id.controls);
        mProgressBar = findViewById(R.id.progress_bar);
        mPlay = findViewById(R.id.button_play);
        mPrev = findViewById(R.id.button_prev);
        mNext = findViewById(R.id.button_next);
        mVolDown = findViewById(R.id.button_vol_down);
        mVolUp = findViewById(R.id.button_vol_up);
        mTitle = findViewById(R.id.title);
        mSubTitle = findViewById(R.id.sub_title);
        mTitle.setSelected(true);
        mTitle.setSingleLine(true);
        mSubTitle.setSelected(true);
        mSubTitle.setSingleLine(true);
        mProgressHandler = new Handler();
        mProgressRunnable = new Runnable() {
            @Override
            public void run() {
                mProgressBar.setProgress((int) (System.currentTimeMillis() - mProgressTimestamp));
                mProgressHandler.postDelayed(this, 32);
            }
        };
        mProgressTimestamp = System.currentTimeMillis();

        setDrawers(mDrawerLayout, null, null, 1);

        mNavigationDrawer.setVisibility(GONE);

        //Chin workaround
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int chin = displayMetrics.widthPixels - displayMetrics.heightPixels;
        if (chin > 0) {
            mControls.getLayoutParams().height = displayMetrics.widthPixels;
            mVolDown.getLayoutParams().height -= chin / 2;
            mVolUp.getLayoutParams().height -= chin / 2;
            mVolDown.getLayoutParams().width -= chin / 2;
            mVolUp.getLayoutParams().width -= chin / 2;
            ((FrameLayout.LayoutParams) mVolDown.getLayoutParams()).bottomMargin += chin;
            ((FrameLayout.LayoutParams) mVolUp.getLayoutParams()).bottomMargin += chin;
        }

        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsPlaying) {
                    getService().getController().pause();
                } else {
                    getService().getController().resume();
                }
            }
        });

        mPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getService().getController().previous();
            }
        });
        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getService().getController().next();
            }
        });

        mVolDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getService().getController().volume(Math.max(0, mVolume - 5));
            }
        });

        mVolUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getService().getController().volume(Math.min(100, mVolume + 5));
            }
        });

        Menu menu = mActionDrawer.getMenu();
        mShuffleMenuItem = menu.add("Shuffle").setIcon(getDrawable(R.drawable.ic_shuffle_black_24dp));
        mShuffleMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                getService().getController().shuffle(!mShuffle);
                return false;
            }
        });
        mRepeatMenuItem = menu.add("Repeat").setIcon(getDrawable(R.drawable.ic_repeat_black_24dp));
        mRepeatMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                switch (mRepeat) {
                    case "off":
                        mRepeat = "context";
                        break;
                    case "context":
                        mRepeat = "track";
                        break;
                    case "track":
                        mRepeat = "off";
                        break;
                }
                getService().getController().repeat(mRepeat);
                return false;
            }
        });
        mDeviceMenuItem = menu.add("Devices").setIcon(getDrawable(R.drawable.ic_devices_other_black_24dp));
        mDeviceMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                startActivity(new Intent(NowPlayingActivity.this, DeviceActivity.class));
                return false;
            }
        });
    }

    private void setLoading(final boolean loading) {
        mBackgroundImage.setVisibility(loading || mAmbient ? INVISIBLE : VISIBLE);
        if (loading) {
            mTitle.setText("");
            mSubTitle.setText("");
        }
        mProgressBar.setIndeterminate(loading);
    }

    private void setPlayButton() {
        mPlay.setImageDrawable(
                getDrawable(mIsPlaying ?
                        mAmbient ?
                                R.drawable.ic_pause_black_burn_in_24dp :
                                R.drawable.ic_pause_black_24dp :
                        mAmbient ?
                                R.drawable.ic_play_arrow_black_burn_in_24dp :
                                R.drawable.ic_play_arrow_black_24dp));
        if (mAmbient) {
            mPlay.setBackgroundResource(R.drawable.round_primary_ambient_button);
            mPlay.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            mPlay.setImageTintMode(PorterDuff.Mode.SRC_ATOP);
        } else {
            mPlay.setBackgroundResource(R.drawable.round_primary_button);
            mPlay.setImageTintList(ColorStateList.valueOf(getColor(R.color.primary_icon)));
            mPlay.setImageTintMode(PorterDuff.Mode.SRC_ATOP);
        }
    }

    @Override
    public void onPlaybackState(CurrentlyPlaying currentlyPlaying) {
        mIsPlaying = currentlyPlaying.is_playing;
        setPlayButton();
        if (currentlyPlaying.item != null) {
            mProgressTimestamp = System.currentTimeMillis() - currentlyPlaying.progress_ms;
            mProgressBar.setProgress((int) (System.currentTimeMillis() - mProgressTimestamp + 1), true);
        }
        mProgressHandler.removeCallbacksAndMessages(null);
        if (mIsPlaying && !mAmbient) {
            mProgressRunnable.run();
        }
    }

    @Override
    public void onPlaybackShuffle(CurrentlyPlaying currentlyPlaying) {
        mShuffle = currentlyPlaying.shuffle_state;
        mShuffleMenuItem.setIcon(mShuffle ?
                R.drawable.ic_shuffle_black_24dp : R.drawable.ic_shuffle_disabled_black_24px);
    }

    @Override
    public void onPlaybackRepeat(CurrentlyPlaying currentlyPlaying) {
        mRepeat = currentlyPlaying.repeat_state;
        switch (mRepeat) {
            case "off":
                mRepeatMenuItem.setIcon(R.drawable.ic_repeat_disabled_black_24px);
                break;
            case "context":
                mRepeatMenuItem.setIcon(R.drawable.ic_repeat_black_24dp);
                break;
            case "track":
                mRepeatMenuItem.setIcon(R.drawable.ic_repeat_one_black_24dp);
                break;
        }
    }

    @Override
    public void onPlaybackPrevious(CurrentlyPlaying currentlyPlaying) {

    }

    @Override
    public void onPlaybackNext(CurrentlyPlaying currentlyPlaying) {

    }

    @Override
    public void onPlaybackVolume(CurrentlyPlaying currentlyPlaying) {
        mVolume = currentlyPlaying.device.volume_percent;
    }

    @Override
    public void onPlaybackSeek(CurrentlyPlaying currentlyPlaying) {

    }

    @Override
    public void onPlaybackMetaData(CurrentlyPlaying currentlyPlaying) {
        Glide.with(getApplicationContext())
                .load(largestImageUrl(currentlyPlaying.item.album.images))
                .fitCenter()
                .dontAnimate()
                .into(mBackgroundImage);
        mTitle.setText(currentlyPlaying.item.name);
        mSubTitle.setText(Util.names(currentlyPlaying.item.artists));
        mProgressTimestamp = System.currentTimeMillis() - currentlyPlaying.progress_ms;
        //mProgressBar.setProgress(0, true);
        mProgressBar.setMax(currentlyPlaying.item.duration_ms);
        setLoading(false);
    }

    @Override
    public void onPlaybackDevice(CurrentlyPlaying currentlyPlaying) {
        mDeviceMenuItem.setTitle(currentlyPlaying.device.name);
        switch (currentlyPlaying.device.type) {
            case "Native":
                mDeviceMenuItem.setIcon(R.drawable.ic_watch_black_24dp);
                break;
            case "Smartphone":
                mDeviceMenuItem.setIcon(R.drawable.ic_smartphone_black_24dp);
                break;
            case "Tablet":
                mDeviceMenuItem.setIcon(R.drawable.ic_tablet_black_24dp);
                break;
            default:
            case "Computer":
                mDeviceMenuItem.setIcon(R.drawable.ic_computer_black_24dp);
                break;
        }
    }

    @Override
    public void onPlaybackBuffering() {
        setLoading(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mProgressHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        mAmbient = true;
        mProgressHandler.removeCallbacksAndMessages(null);
        mDrawerLayout.setBackgroundColor(Color.BLACK);
        mNavigationDrawer.setVisibility(GONE);
        mBackgroundImage.setVisibility(INVISIBLE);
        mTitle.setSelected(false);
        mSubTitle.setSelected(false);
        mProgressBar.setVisibility(INVISIBLE);
        mActionDrawer.setVisibility(GONE);
        setPlayButton();
        mNext.setImageDrawable(getDrawable(R.drawable.ic_skip_next_black_burn_in_24dp));
        mPrev.setImageDrawable(getDrawable(R.drawable.ic_skip_previous_black_burn_in_24dp));
        mVolDown.setImageDrawable(getDrawable(R.drawable.ic_volume_down_black_burn_in_24dp));
        mVolUp.setImageDrawable(getDrawable(R.drawable.ic_volume_up_black_burn_in_24dp));
        getService().getController().setInterval(30000);
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        mAmbient = false;
        mDrawerLayout.setBackgroundColor(getColor(R.color.background));
        mBackgroundImage.setVisibility(VISIBLE);
        mNavigationDrawer.setVisibility(VISIBLE);
        mBackgroundImage.setVisibility(VISIBLE);
        mTitle.setSelected(true);
        mSubTitle.setSelected(true);
        mProgressBar.setVisibility(VISIBLE);
        if (mIsPlaying) mProgressRunnable.run();
        mActionDrawer.setVisibility(VISIBLE);
        setPlayButton();
        mNext.setImageDrawable(getDrawable(R.drawable.ic_skip_next_black_24dp));
        mPrev.setImageDrawable(getDrawable(R.drawable.ic_skip_previous_black_24dp));
        mVolDown.setImageDrawable(getDrawable(R.drawable.ic_volume_down_black_24dp));
        mVolUp.setImageDrawable(getDrawable(R.drawable.ic_volume_up_black_24dp));
        getService().getController().setInterval(INTERVAL);
    }
}
