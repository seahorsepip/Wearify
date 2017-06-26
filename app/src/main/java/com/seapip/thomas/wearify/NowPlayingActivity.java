package com.seapip.thomas.wearify;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.seapip.thomas.wearify.browse.Activity;
import com.seapip.thomas.wearify.spotify.controller.Controller;
import com.seapip.thomas.wearify.spotify.Service;
import com.seapip.thomas.wearify.spotify.objects.CurrentlyPlaying;
import com.seapip.thomas.wearify.spotify.Util;
import com.squareup.picasso.Picasso;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.seapip.thomas.wearify.spotify.Util.largestImageUrl;

public class NowPlayingActivity extends Activity implements Controller.Callbacks {

    private boolean mIsBound;
    private Service mController;
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            mIsBound = false;
            mController = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            mIsBound = true;
            mController = ((Service.ControllerBinder) service).getServiceInstance();
            mController.setCallbacks(NowPlayingActivity.this);
            mController.getController().bind();
        }
    };
    private boolean mAmbient;
    private WearableDrawerLayout mDrawerLayout;
    private WearableNavigationDrawer mNavigationDrawer;
    private ImageView mBackgroundImage;
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
    private CurrentlyPlaying mCurrentlyPlaying;
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

        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsPlaying) {
                    mController.getController().pause();
                } else {
                    mController.getController().resume();
                }
            }
        });
        mPlay.setClickAlpha(80);

        mPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mController.getController().previous();
            }
        });
        mPrev.setClickAlpha(20);

        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mController.getController().next();
            }
        });
        mNext.setClickAlpha(20);

        mVolDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mController.getController().volume(Math.max(0, mVolume - 5));
            }
        });
        mVolDown.setClickAlpha(20);

        mVolUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mController.getController().volume(Math.min(100, mVolume + 5));
            }
        });
        mVolUp.setClickAlpha(20);

        Menu menu = mActionDrawer.getMenu();
        mShuffleMenuItem = menu.add("Shuffle").setIcon(getDrawable(R.drawable.ic_shuffle_black_24dp));
        mShuffleMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                mController.getController().shuffle(!mShuffle);
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
                mController.getController().repeat(mRepeat);
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
        onProgress();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent mIntent = new Intent(this, Service.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
    }

    private void setLoading(boolean loading) {
        mBackgroundImage.setVisibility(loading ? GONE : VISIBLE);
        if (loading) {
            mTitle.setText("");
            mSubTitle.setText("");
            mProgress.setVisibility(GONE);
        }
        mProgressBar.setVisibility(loading ? VISIBLE : GONE);
    }

    private void onProgress() {
        (new Runnable() {
            @Override
            public void run() {
                if (mCurrentlyPlaying != null && mCurrentlyPlaying.item.duration_ms > 0 && mCurrentlyPlaying.is_playing) {
                    mProgress.setProgress((float) mCurrentlyPlaying.progress_ms
                            / (float) mCurrentlyPlaying.item.duration_ms * 100f);
                    mBackgroundImage.invalidate();
                }
                mProgressHandler.postDelayed(this, 20);
            }
        }).run();
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
        Picasso.with(getApplicationContext())
                .load(largestImageUrl(currentlyPlaying.item.album.images))
                .fit().into(mBackgroundImage);
        mTitle.setText(currentlyPlaying.item.name);
        mSubTitle.setText(Util.names(currentlyPlaying.item.artists));
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
        mBackgroundImage.setVisibility(GONE);
        mProgress.setVisibility(GONE);
        mActionDrawer.setVisibility(GONE);
        setPlayButton();
        mNext.setImageDrawable(getDrawable(R.drawable.ic_skip_next_black_burn_in_24dp));
        mPrev.setImageDrawable(getDrawable(R.drawable.ic_skip_previous_black_burn_in_24dp));
        mVolDown.setImageDrawable(getDrawable(R.drawable.ic_volume_down_black_burn_in_24dp));
        mVolUp.setImageDrawable(getDrawable(R.drawable.ic_volume_up_black_burn_in_24dp));
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        mAmbient = false;
        onProgress();
        mDrawerLayout.setBackgroundColor(Color.parseColor("#141414"));
        mNavigationDrawer.setVisibility(VISIBLE);
        mBackgroundImage.setVisibility(VISIBLE);
        mProgress.setVisibility(VISIBLE);
        mActionDrawer.setVisibility(VISIBLE);
        setPlayButton();
        mNext.setImageDrawable(getDrawable(R.drawable.ic_skip_next_black_24dp));
        mPrev.setImageDrawable(getDrawable(R.drawable.ic_skip_previous_black_24dp));
        mVolDown.setImageDrawable(getDrawable(R.drawable.ic_volume_down_black_24dp));
        mVolUp.setImageDrawable(getDrawable(R.drawable.ic_volume_up_black_24dp));
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsBound) {
            mController.unsetCallbacks(this);
            unbindService(mConnection);
        }
    }
}
