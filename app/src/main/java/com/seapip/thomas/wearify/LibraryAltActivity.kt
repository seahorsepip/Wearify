package com.seapip.thomas.wearify

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.support.wear.ambient.AmbientMode
import android.support.wear.widget.drawer.WearableDrawerLayout
import android.view.View
import com.seapip.thomas.wearify.R.color.primary_icon
import com.seapip.thomas.wearify.R.drawable.*
import com.seapip.thomas.wearify.R.layout.activity_library
import kotlinx.android.synthetic.main.activity_library.*

class LibraryAltActivity : Activity(), AmbientMode.AmbientCallbackProvider {

    private var mAmbientMode: AmbientMode? = null
    private var mAmbientAttached = false
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
        setContentView(activity_library)

        drawer_layout.setDrawerStateCallback(mDrawerStateCallback)

        val drawablePlaying = getDrawable(ic_logo_waves_animated) as AnimatedVectorDrawable
        icon_drawer_peek.setImageDrawable(drawablePlaying)
        drawablePlaying.start()

        playback_drawer.controller.peekDrawer()
    }

    private fun addAmbientSupport() {
        if (!mAmbientAttached) {
            mAmbientAttached = true

            if (mAmbientMode == null) AmbientMode.attachAmbientSupport(this@LibraryAltActivity)
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
        return object : AmbientMode.AmbientCallback() {
            override fun onExitAmbient() {
                super.onExitAmbient()

                //Background
                album_art_background_image.visibility = View.VISIBLE

                //Progressbar
                progress_bar.visibility = View.VISIBLE

                //Play button
                button_play.apply {
                    background = getDrawable(round_primary_button)
                    setImageDrawable(getDrawable(ic_pause_black_24dp))
                    imageTintList = ColorStateList.valueOf(getColor(primary_icon))
                }
            }

            override fun onEnterAmbient(ambientDetails: Bundle?) {
                super.onEnterAmbient(ambientDetails)
                val burnIn = ambientDetails!!.getBoolean(AmbientMode.EXTRA_BURN_IN_PROTECTION)
                val lowBit = ambientDetails.getBoolean(AmbientMode.EXTRA_LOWBIT_AMBIENT)

                //Background
                if (burnIn || lowBit) album_art_background_image.visibility = View.INVISIBLE

                //Progressbar
                progress_bar.visibility = View.INVISIBLE

                //Play button
                button_play.apply {
                    if (burnIn || lowBit) background = getDrawable(round_primary_ambient_button)
                    if (burnIn) setImageDrawable(getDrawable(ic_pause_black_burn_in_24dp))
                    if (burnIn || lowBit) imageTintList = ColorStateList.valueOf(Color.WHITE)
                }
            }
        }
    }
}