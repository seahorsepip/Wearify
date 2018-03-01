package com.seapip.thomas.wearify

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.support.wearable.view.drawer.WearableActionDrawer
import android.support.wearable.view.drawer.WearableDrawerLayout
import android.support.wearable.view.drawer.WearableNavigationDrawer
import android.util.DisplayMetrics
import android.view.MenuItem
import android.view.View.*
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.seapip.thomas.wearify.browse.Activity
import com.seapip.thomas.wearify.spotify.Service.INTERVAL
import com.seapip.thomas.wearify.spotify.Util
import com.seapip.thomas.wearify.spotify.Util.largestImageUrl
import com.seapip.thomas.wearify.spotify.controller.Controller
import com.seapip.thomas.wearify.spotify.objects.CurrentlyPlaying
import kotlinx.android.synthetic.main.activity_now_playing.*


class NowPlayingActivity : Activity(), Controller.Callbacks {

    private var mAmbient: Boolean = false
    private var mDrawerLayout: WearableDrawerLayout? = null
    private var mNavigationDrawer: WearableNavigationDrawer? = null
    private var mBackgroundImage: ImageView? = null
    private var mControls: FrameLayout? = null
    private var mPlay: ImageButton? = null
    private var mPrev: ImageButton? = null
    private var mNext: ImageButton? = null
    private var mVolDown: ImageButton? = null
    private var mVolUp: ImageButton? = null
    private var mTitle: TextView? = null
    private var mSubTitle: TextView? = null
    private var mActionDrawer: WearableActionDrawer? = null
    private var mShuffleMenuItem: MenuItem? = null
    private var mRepeatMenuItem: MenuItem? = null
    private var mDeviceMenuItem: MenuItem? = null
    private var mIsPlaying = true
    private var mShuffle: Boolean = false
    private var mRepeat: String? = null
    private var mVolume: Int = 0
    private var mProgressTimestamp: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAmbientEnabled()
        setContentView(R.layout.activity_now_playing)

        mNavigationDrawer = findViewById(R.id.navigation_drawer)
        mActionDrawer = findViewById(R.id.bottom_action_drawer)
        mDrawerLayout = findViewById(R.id.drawer_layout)
        mBackgroundImage = findViewById(R.id.background_image)
        mControls = findViewById(R.id.controls)
        mPlay = findViewById(R.id.button_play)
        mPrev = findViewById(R.id.button_prev)
        mNext = findViewById(R.id.button_next)
        mVolDown = findViewById(R.id.button_vol_down)
        mVolUp = findViewById(R.id.button_vol_up)
        mTitle = findViewById(R.id.title)
        mSubTitle = findViewById(R.id.sub_title)
        mTitle!!.isSelected = true
        mTitle!!.setSingleLine(true)
        mSubTitle!!.isSelected = true
        mSubTitle!!.setSingleLine(true)

        mProgressTimestamp = System.currentTimeMillis()

        var progressbarAnimation = {}
        progressbarAnimation = {
            val interval = 500L

            if (mIsPlaying) ObjectAnimator.ofInt(progress_bar, "progress",
                    (System.currentTimeMillis() - mProgressTimestamp + interval).toInt()).apply {
                duration = interval
                this.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(p0: Animator?) {
                    }

                    override fun onAnimationEnd(p0: Animator?) {
                        progressbarAnimation()
                    }

                    override fun onAnimationCancel(p0: Animator?) {
                    }

                    override fun onAnimationStart(p0: Animator?) {
                    }
                })
                start()
            }
            else Handler().postDelayed(progressbarAnimation, interval)
        }
        progressbarAnimation()

        setDrawers(mDrawerLayout, null, null, 1)

        mNavigationDrawer!!.visibility = GONE

        //Chin workaround
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val chin = displayMetrics.widthPixels - displayMetrics.heightPixels
        if (chin > 0) {
            mControls!!.layoutParams.height = displayMetrics.widthPixels
            mVolDown!!.layoutParams.height -= chin / 2
            mVolUp!!.layoutParams.height -= chin / 2
            mVolDown!!.layoutParams.width -= chin / 2
            mVolUp!!.layoutParams.width -= chin / 2
            (mVolDown!!.layoutParams as FrameLayout.LayoutParams).bottomMargin += chin
            (mVolUp!!.layoutParams as FrameLayout.LayoutParams).bottomMargin += chin
        }

        mPlay!!.setOnClickListener {
            if (mIsPlaying) {
                service.controller!!.pause()
            } else {
                service.controller!!.resume()
            }
        }

        mPrev!!.setOnClickListener { service.controller!!.previous() }
        mNext!!.setOnClickListener { service.controller!!.next() }

        mVolDown!!.setOnClickListener { service.controller!!.volume(Math.max(0, mVolume - 5)) }

        mVolUp!!.setOnClickListener { service.controller!!.volume(Math.min(100, mVolume + 5)) }

        val menu = mActionDrawer!!.menu
        mShuffleMenuItem = menu.add("Shuffle").setIcon(getDrawable(R.drawable.ic_shuffle_black_24dp))
        mShuffleMenuItem!!.setOnMenuItemClickListener {
            service.controller!!.shuffle(!mShuffle)
            false
        }
        mRepeatMenuItem = menu.add("Repeat").setIcon(getDrawable(R.drawable.ic_repeat_black_24dp))
        mRepeatMenuItem!!.setOnMenuItemClickListener {
            when (mRepeat) {
                "off" -> mRepeat = "context"
                "context" -> mRepeat = "track"
                "track" -> mRepeat = "off"
            }
            service.controller!!.repeat(mRepeat)
            false
        }
        mDeviceMenuItem = menu.add("Devices").setIcon(getDrawable(R.drawable.ic_devices_other_black_24dp))
        mDeviceMenuItem!!.setOnMenuItemClickListener {
            startActivity(Intent(this@NowPlayingActivity, DeviceActivity::class.java))
            false
        }
    }

    private fun setLoading(loading: Boolean) {
        mBackgroundImage!!.visibility = if (loading || mAmbient) INVISIBLE else VISIBLE
        if (loading) {
            mTitle!!.text = ""
            mSubTitle!!.text = ""
            mProgressTimestamp = System.currentTimeMillis() + progress_bar.max
        }
    }

    private fun setPlayButton() {
        mPlay!!.setImageDrawable(
                getDrawable(if (mIsPlaying)
                    if (mAmbient)
                        R.drawable.ic_pause_black_burn_in_24dp
                    else
                        R.drawable.ic_pause_black_24dp
                else if (mAmbient)
                    R.drawable.ic_play_arrow_black_burn_in_24dp
                else
                    R.drawable.ic_play_arrow_black_24dp))
        if (mAmbient) {
            mPlay!!.setBackgroundResource(R.drawable.round_primary_ambient_button)
            mPlay!!.imageTintList = ColorStateList.valueOf(Color.WHITE)
            mPlay!!.imageTintMode = PorterDuff.Mode.SRC_ATOP
        } else {
            mPlay!!.setBackgroundResource(R.drawable.round_primary_button)
            mPlay!!.imageTintList = ColorStateList.valueOf(getColor(R.color.primary_icon))
            mPlay!!.imageTintMode = PorterDuff.Mode.SRC_ATOP
        }
    }

    override fun onPlaybackState(currentlyPlaying: CurrentlyPlaying) {
        mIsPlaying = currentlyPlaying.is_playing
        setPlayButton()
        if (currentlyPlaying.item != null) {
            mProgressTimestamp = System.currentTimeMillis() - currentlyPlaying.progress_ms
        }
    }

    override fun onPlaybackShuffle(currentlyPlaying: CurrentlyPlaying) {
        mShuffle = currentlyPlaying.shuffle_state
        mShuffleMenuItem!!.setIcon(if (mShuffle)
            R.drawable.ic_shuffle_black_24dp
        else
            R.drawable.ic_shuffle_disabled_black_24px)
    }

    override fun onPlaybackRepeat(currentlyPlaying: CurrentlyPlaying) {
        mRepeat = currentlyPlaying.repeat_state
        when (mRepeat) {
            "off" -> mRepeatMenuItem!!.setIcon(R.drawable.ic_repeat_disabled_black_24px)
            "context" -> mRepeatMenuItem!!.setIcon(R.drawable.ic_repeat_black_24dp)
            "track" -> mRepeatMenuItem!!.setIcon(R.drawable.ic_repeat_one_black_24dp)
        }
    }

    override fun onPlaybackPrevious(currentlyPlaying: CurrentlyPlaying) {

    }

    override fun onPlaybackNext(currentlyPlaying: CurrentlyPlaying) {

    }

    override fun onPlaybackVolume(currentlyPlaying: CurrentlyPlaying) {
        mVolume = currentlyPlaying.device.volume_percent
    }

    override fun onPlaybackSeek(currentlyPlaying: CurrentlyPlaying) {

    }

    override fun onPlaybackMetaData(currentlyPlaying: CurrentlyPlaying) {
        if(currentlyPlaying.item == null) {
            finish()
            return
        }
        Glide.with(applicationContext)
                .load(largestImageUrl(currentlyPlaying.item.album.images))
                .fitCenter()
                .dontAnimate()
                .into(mBackgroundImage!!)
        mTitle!!.text = currentlyPlaying.item.name
        mSubTitle!!.text = Util.names(currentlyPlaying.item.artists)
        mProgressTimestamp = System.currentTimeMillis() - currentlyPlaying.progress_ms
        progress_bar.max = currentlyPlaying.item.duration_ms
        setLoading(false)
    }

    override fun onPlaybackDevice(currentlyPlaying: CurrentlyPlaying) {
        mDeviceMenuItem!!.title = currentlyPlaying.device.name
        when (currentlyPlaying.device.type) {
            "Native" -> mDeviceMenuItem!!.setIcon(R.drawable.ic_watch_black_24dp)
            "Smartphone" -> mDeviceMenuItem!!.setIcon(R.drawable.ic_smartphone_black_24dp)
            "Tablet" -> mDeviceMenuItem!!.setIcon(R.drawable.ic_tablet_black_24dp)
            "Computer" -> mDeviceMenuItem!!.setIcon(R.drawable.ic_computer_black_24dp)
            else -> mDeviceMenuItem!!.setIcon(R.drawable.ic_computer_black_24dp)
        }
    }

    override fun onPlaybackBuffering() {
        setLoading(true)
    }

    override fun onEnterAmbient(ambientDetails: Bundle?) {
        super.onEnterAmbient(ambientDetails)
        mAmbient = true
        mDrawerLayout!!.setBackgroundColor(Color.BLACK)
        mNavigationDrawer!!.visibility = GONE
        mBackgroundImage!!.visibility = INVISIBLE
        mTitle!!.isSelected = false
        mSubTitle!!.isSelected = false
        progress_bar.visibility = INVISIBLE
        mActionDrawer!!.visibility = GONE
        setPlayButton()
        mNext!!.setImageDrawable(getDrawable(R.drawable.ic_skip_next_black_burn_in_24dp))
        mPrev!!.setImageDrawable(getDrawable(R.drawable.ic_skip_previous_black_burn_in_24dp))
        mVolDown!!.setImageDrawable(getDrawable(R.drawable.ic_volume_down_black_burn_in_24dp))
        mVolUp!!.setImageDrawable(getDrawable(R.drawable.ic_volume_up_black_burn_in_24dp))
        service.controller!!.setInterval(30000)
    }

    override fun onExitAmbient() {
        super.onExitAmbient()
        mAmbient = false
        mDrawerLayout!!.setBackgroundColor(getColor(R.color.background))
        mBackgroundImage!!.visibility = VISIBLE
        mNavigationDrawer!!.visibility = VISIBLE
        mBackgroundImage!!.visibility = VISIBLE
        mTitle!!.isSelected = true
        mSubTitle!!.isSelected = true
        progress_bar.visibility = VISIBLE
        mActionDrawer!!.visibility = VISIBLE
        setPlayButton()
        mNext!!.setImageDrawable(getDrawable(R.drawable.ic_skip_next_black_24dp))
        mPrev!!.setImageDrawable(getDrawable(R.drawable.ic_skip_previous_black_24dp))
        mVolDown!!.setImageDrawable(getDrawable(R.drawable.ic_volume_down_black_24dp))
        mVolUp!!.setImageDrawable(getDrawable(R.drawable.ic_volume_up_black_24dp))
        service.controller!!.setInterval(INTERVAL)
    }
}
