package com.seapip.thomas.wearify

import android.app.Fragment
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.support.wear.ambient.AmbientMode.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.seapip.thomas.wearify.R.color.primary_icon
import com.seapip.thomas.wearify.R.drawable.*
import com.seapip.thomas.wearify.R.layout.now_playing_view
import kotlinx.android.synthetic.main.now_playing_view.*

class NowPlayingFragment : Fragment(), AmbientCallbackProvider {

    private val mAmbientCallback = object : AmbientCallback() {
        override fun onExitAmbient() {
            album_art_background_image.visibility = View.VISIBLE

            progress_bar.visibility = View.VISIBLE

            context.apply {
                button_play.also {
                    it.background = getDrawable(round_primary_button)
                    it.setImageDrawable(getDrawable(ic_pause_black_24dp))
                    it.imageTintList = ColorStateList.valueOf(getColor(primary_icon))
                }
                button_prev.setImageDrawable(getDrawable(ic_skip_previous_black_24dp))
                button_next.setImageDrawable(getDrawable(ic_skip_next_black_24dp))
                button_vol_up.setImageDrawable(getDrawable(ic_volume_up_black_24dp))
                button_vol_down.setImageDrawable(getDrawable(ic_volume_down_black_24dp))
            }
        }

        override fun onEnterAmbient(ambientDetails: Bundle?) {
            val burnIn = ambientDetails!!.getBoolean(EXTRA_BURN_IN_PROTECTION)
            val lowBit = ambientDetails.getBoolean(EXTRA_LOWBIT_AMBIENT)

            if (burnIn || lowBit) album_art_background_image.visibility = View.INVISIBLE

            progress_bar.visibility = View.INVISIBLE

            context.apply {
                button_play.also {
                    if (burnIn || lowBit) {
                        it.background = getDrawable(round_primary_ambient_button)
                        it.imageTintList = ColorStateList.valueOf(Color.WHITE)
                    }
                    if (burnIn) it.setImageDrawable(getDrawable(ic_pause_black_burn_in_24dp))
                }
                if (burnIn) {
                    button_prev.setImageDrawable(getDrawable(ic_skip_previous_black_burn_in_24dp))
                    button_next.setImageDrawable(getDrawable(ic_skip_next_black_burn_in_24dp))
                    button_vol_up.setImageDrawable(getDrawable(ic_volume_up_black_burn_in_24dp))
                    button_vol_down.setImageDrawable(getDrawable(ic_volume_down_black_burn_in_24dp))
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater!!.inflate(now_playing_view, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_play.setOnClickListener {
            Toast.makeText(context, "Button clicked!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getAmbientCallback(): AmbientCallback {
        return mAmbientCallback
    }
}