package com.seapip.thomas.wearify

import android.app.Activity
import android.graphics.Rect
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.support.wear.ambient.AmbientMode
import android.support.wear.widget.drawer.WearableDrawerLayout
import android.util.Log
import com.seapip.thomas.wearify.R.drawable.ic_audio_waves_animated
import com.seapip.thomas.wearify.R.layout.main_view
import kotlinx.android.synthetic.main.main_view.*
import java.lang.Math.sqrt

class MainActivity : Activity(), AmbientMode.AmbientCallbackProvider {

    private var mAmbientMode: AmbientMode? = null
    private var mAmbientAttached = false
    private val mAmbientCallback = object : AmbientMode.AmbientCallback() {
        override fun onExitAmbient() {
            super.onExitAmbient()
            (drawer_content as NowPlayingFragment).ambientCallback.onExitAmbient()
        }

        override fun onEnterAmbient(ambientDetails: Bundle?) {
            super.onEnterAmbient(ambientDetails)
            (drawer_content as NowPlayingFragment).ambientCallback.onEnterAmbient(ambientDetails)
        }
    }
    private val mDrawerStateCallback = object : WearableDrawerLayout.DrawerStateCallback() {
        override fun onDrawerStateChanged(layout: WearableDrawerLayout?, newState: Int) {
            super.onDrawerStateChanged(layout, newState)
            if (newState == 0) {
                if ((playback_drawer.isClosed || playback_drawer.isPeeking)) removeAmbientSupport()
                else if (playback_drawer.isOpened) addAmbientSupport()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(main_view)

        drawer_layout.setDrawerStateCallback(mDrawerStateCallback)

        val drawablePlaying = getDrawable(ic_audio_waves_animated) as AnimatedVectorDrawable
        icon_drawer_peek.setImageDrawable(drawablePlaying)
        drawablePlaying.start()

        playback_drawer.controller.peekDrawer()


        track_title.isSelected = true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val rect = Rect()
            peek_view.getGlobalVisibleRect(rect)
            val width = 2.0 * sqrt(Math.pow(rect.right / 2.0, 2.0)
                    - Math.pow((rect.bottom / 2.0 - peek_view.height), 2.0))
            peek_view.layoutParams.width = width.toInt()
            peek_view.requestLayout()
            Log.e("WEARIFY", width.toString())
        }
    }

    private fun addAmbientSupport() {
        if (!mAmbientAttached) {
            mAmbientAttached = true

            if (mAmbientMode == null) AmbientMode.attachAmbientSupport(this@MainActivity)
            else fragmentManager.beginTransaction().add(mAmbientMode, AmbientMode.FRAGMENT_TAG).commit()
        }
    }

    private fun removeAmbientSupport() {
        if (mAmbientAttached) {
            mAmbientAttached = false

            if (mAmbientMode == null) mAmbientMode = fragmentManager.findFragmentByTag(AmbientMode.FRAGMENT_TAG) as AmbientMode?
            if (mAmbientMode != null) fragmentManager.beginTransaction().remove(mAmbientMode).commit()
        }
    }

    override fun getAmbientCallback(): AmbientMode.AmbientCallback {
        return mAmbientCallback
    }
}