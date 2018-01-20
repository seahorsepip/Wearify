package com.seapip.thomas.wearify

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK
import android.os.PowerManager.WakeLock
import android.os.Vibrator
import android.support.wearable.activity.WearableActivity
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.seapip.thomas.wearify.wearify.Manager
import com.seapip.thomas.wearify.wearify.Token
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class QRActivity : WearableActivity() {

    private var mToken: String? = null
    private var mKey: String? = null
    private var mLayout: RelativeLayout? = null
    private var mProgressBar: ProgressBar? = null
    private var mQRCodeView: ImageView? = null
    private var mSize: Int = 0
    private var mLogo: Drawable? = null
    private var mQRBitmap: Bitmap? = null
    private var mAmbientQRBitmap: Bitmap? = null
    private val mAmbient: Boolean = false
    private val wakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(SCREEN_BRIGHT_WAKE_LOCK, this.localClassName)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr)

        mLayout = findViewById(R.id.layout)
        mProgressBar = findViewById(R.id.progress_bar)
        mProgressBar!!.indeterminateDrawable.setColorFilter(Color.parseColor("#222222"),
                PorterDuff.Mode.SRC_ATOP)
        mQRCodeView = findViewById(R.id.QRCode)

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        mSize = if (resources.configuration.isScreenRound) size.x / 3 * 2 else size.x - size.x / 20
        mLogo = getDrawable(R.mipmap.ic_launcher)
        mLogo!!.setBounds(mSize / 2 - mSize / 9, mSize / 2 - mSize / 9,
                mSize / 2 + mSize / 9, mSize / 2 + mSize / 9)
        mQRBitmap = Bitmap.createBitmap(mSize, mSize, Bitmap.Config.ARGB_8888)
        mAmbientQRBitmap = Bitmap.createBitmap(mSize, mSize, Bitmap.Config.ARGB_8888)
        val handler = Handler()
        object : Runnable {
            override fun run() {
                val call = Manager.getService().token
                call.enqueue(object : Callback<Token> {
                    override fun onResponse(call: Call<Token>, response: Response<Token>) {
                        if (response.isSuccessful) {
                            val token = response.body()
                            if (token != null) {
                                mToken = token.token
                                mKey = token.key
                                mProgressBar!!.visibility = View.GONE
                                generateQRCode()
                                drawQRCode()
                            }
                        }
                    }

                    override fun onFailure(call: Call<Token>, t: Throwable) {

                    }
                })
                handler.postDelayed(this, 600000)
            }
        }.run()
        object : Runnable {
            override fun run() {
                val call = Manager.getService().getToken(mToken, mKey)
                call.enqueue(object : Callback<Token> {
                    override fun onResponse(call: Call<Token>, response: Response<Token>) {
                        if (response.isSuccessful) {
                            val token = response.body()
                            if (token != null && token.access_token != null) {
                                handler.removeCallbacksAndMessages(null)
                                Manager.setToken(this@QRActivity, token)
                                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(500)
                                Toast.makeText(applicationContext, "Logged in!", Toast.LENGTH_LONG).show()
                                val intent = Intent(this@QRActivity, LibraryActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
                                finishAffinity()
                                startActivity(intent)
                            }
                        }
                    }

                    override fun onFailure(call: Call<Token>, t: Throwable) {

                    }
                })
                handler.postDelayed(this, 5000)
            }
        }.run()
    }

    private fun generateQRCode() {
        if (mToken != null) {
            try {
                val hints = HashMap<EncodeHintType, Any>(3)
                hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
                hints[EncodeHintType.MARGIN] = 0
                hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
                val bitMatrix = QRCodeWriter().encode(
                        "https://wearify.seapip.com/login/$mToken/$mKey",
                        BarcodeFormat.QR_CODE, mSize, mSize, hints)
                val color = Color.parseColor("#222222")
                for (x in 0 until bitMatrix.width) {
                    for (y in 0 until bitMatrix.height) {
                        val bit = bitMatrix.get(x, y)
                        mQRBitmap!!.setPixel(x, y, if (bit) color else Color.TRANSPARENT)
                    }
                }
                val paint = Paint()
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                paint.isAntiAlias = true
                val canvas = Canvas(mQRBitmap!!)
                canvas.drawCircle(mSize / 2f, mSize / 2f, mSize / 8f, paint)
                mLogo!!.draw(canvas)

            } catch (e: WriterException) {
                e.printStackTrace()
            }

        }
    }

    private fun drawQRCode() {
        mLayout!!.setBackgroundColor(if (mAmbient) Color.BLACK else Color.parseColor("#eeeeee"))
        mQRCodeView!!.setImageBitmap(if (mAmbient) mAmbientQRBitmap else mQRBitmap)
    }

    override fun onPause() {
        super.onPause()
        with(wakeLock) {
            if (isHeld) release()
        }
    }

    override fun onResume() {
        super.onResume()
        with(wakeLock) {
            if (!isHeld) acquire(30000)
        }
    }
}
