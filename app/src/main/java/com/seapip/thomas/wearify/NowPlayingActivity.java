package com.seapip.thomas.wearify;

import android.content.Intent;
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
    private RoundImageButtonView mPlay;
    private RoundImageButtonView mPrev;
    private RoundImageButtonView mNext;
    private RoundImageButtonView mVolDown;
    private RoundImageButtonView mVolUp;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAmbientEnabled();
        setContentView(R.layout.activity_now_playing);

        mNavigationDrawer = (WearableNavigationDrawer) findViewById(R.id.top_navigation_drawer);
        mActionDrawer = (WearableActionDrawer) findViewById(R.id.bottom_action_drawer);
        mDrawerLayout = (WearableDrawerLayout) findViewById(R.id.drawer_layout);
        mBackgroundImage = (ImageView) findViewById(R.id.background_image);
        mControls = (FrameLayout) findViewById(R.id.controls);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressBar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        mProgressBar.setVisibility(GONE);
        mPlay = (RoundImageButtonView) findViewById(R.id.button_play);
        mPrev = (RoundImageButtonView) findViewById(R.id.button_prev);
        mNext = (RoundImageButtonView) findViewById(R.id.button_next);
        mVolDown = (RoundImageButtonView) findViewById(R.id.button_vol_down);
        mVolUp = (RoundImageButtonView) findViewById(R.id.button_vol_up);
        mTitle = (TextView) findViewById(R.id.title);
        mSubTitle = (TextView) findViewById(R.id.sub_title);
        mProgress = (CircularProgressView) findViewById(R.id.circle_progress);
        mProgressHandler = new Handler();

        setDrawers(mDrawerLayout, null, null, 1);

        mNavigationDrawer.setVisibility(GONE);

        //Chin workaround
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mBackgroundImage.getLayoutParams().height = displayMetrics.widthPixels;
        mControls.getLayoutParams().height = displayMetrics.widthPixels;
        int chin = displayMetrics.widthPixels - displayMetrics.heightPixels;
        mVolDown.getLayoutParams().height -= chin;
        mVolUp.getLayoutParams().height -= chin;
        mVolDown.setPadding(mVolDown.getPaddingLeft(), mVolDown.getPaddingTop(),
                mVolDown.getPaddingRight(), mVolDown.getPaddingBottom() + chin);
        mVolUp.setPadding(mVolUp.getPaddingLeft(), mVolUp.getPaddingTop(),
                mVolUp.getPaddingRight(), mVolUp.getPaddingBottom() + chin);

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
        mPlay.setClickAlpha(80);

        mPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getService().getController().previous();
            }
        });
        mPrev.setClickAlpha(20);

        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getService().getController().next();
            }
        });
        mNext.setClickAlpha(20);

        mVolDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getService().getController().volume(Math.max(0, mVolume - 5));
            }
        });
        mVolDown.setClickAlpha(20);

        mVolUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getService().getController().volume(Math.min(100, mVolume + 5));
            }
        });
        mVolUp.setClickAlpha(20);

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

    private void setLoading(boolean loading) {
        mBackgroundImage.setVisibility(loading || mAmbient ? INVISIBLE : VISIBLE);
        if (loading) {
            mTitle.setText("");
            mSubTitle.setText("");
            mProgress.setVisibility(GONE);
        }
        mProgressBar.setVisibility(loading ? VISIBLE : GONE);
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
            mPlay.setBackgroundColor(Color.TRANSPARENT);
            mPlay.setTint(Color.WHITE);
            mPlay.setBorder(Color.parseColor("#777777"));
        } else {
            mPlay.setBorder(Color.TRANSPARENT);
            mPlay.setBackgroundColor(Color.parseColor("#00ffe0"));
            mPlay.setTint(Color.argb(180, 0, 0, 0));
        }
    }

    @Override
    public void onPlaybackState(CurrentlyPlaying currentlyPlaying) {
        mIsPlaying = currentlyPlaying.is_playing;
        setPlayButton();
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
    protected void onStop() {
        super.onStop();
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
        mProgress.setVisibility(GONE);
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
        mDrawerLayout.setBackgroundColor(Color.parseColor("#141414"));
        mBackgroundImage.setVisibility(VISIBLE);
        mNavigationDrawer.setVisibility(VISIBLE);
        mBackgroundImage.setVisibility(VISIBLE);
        mProgress.setVisibility(VISIBLE);
        mActionDrawer.setVisibility(VISIBLE);
        setPlayButton();
        mNext.setImageDrawable(getDrawable(R.drawable.ic_skip_next_black_24dp));
        mPrev.setImageDrawable(getDrawable(R.drawable.ic_skip_previous_black_24dp));
        mVolDown.setImageDrawable(getDrawable(R.drawable.ic_volume_down_black_24dp));
        mVolUp.setImageDrawable(getDrawable(R.drawable.ic_volume_up_black_24dp));
        getService().getController().setInterval(INTERVAL);
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
    }
}
