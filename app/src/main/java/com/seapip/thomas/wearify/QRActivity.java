package com.seapip.thomas.wearify;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.seapip.thomas.wearify.wearify.Manager;
import com.seapip.thomas.wearify.wearify.Token;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QRActivity extends WearableActivity {

    private String mToken;
    private String mKey;
    private RelativeLayout mLayout;
    private ProgressBar mProgressBar;
    private ImageView mQRCodeView;
    private int mSize;
    private Drawable mLogo;
    private Drawable mLogoBurnIn;
    private Bitmap mQRBitmap;
    private Bitmap mAmbientQRBitmap;
    private boolean mAmbient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAmbientEnabled();
        setContentView(R.layout.activity_qr);
        mLayout = (RelativeLayout) findViewById(R.id.layout);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor("#222222"),
                PorterDuff.Mode.SRC_ATOP);
        mQRCodeView = (ImageView) findViewById(R.id.QRCode);
        Display display = getWindowManager().getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);
        mSize = getResources().getConfiguration().isScreenRound() ? size.x / 3 * 2 : size.x - size.x / 20;
        mLogo = getDrawable(R.drawable.ic_logo);
        mLogo.setBounds(mSize / 2 - mSize / 10, mSize / 2 - mSize / 10,
                mSize / 2 + mSize / 10, mSize / 2 + mSize / 10);
        mLogoBurnIn = getDrawable(R.drawable.ic_logo_burn_in);
        mLogoBurnIn.setBounds(mSize / 2 - mSize / 10, mSize / 2 - mSize / 10,
                mSize / 2 + mSize / 10, mSize / 2 + mSize / 10);
        mQRBitmap = Bitmap.createBitmap(mSize, mSize, Bitmap.Config.ARGB_8888);
        mAmbientQRBitmap = Bitmap.createBitmap(mSize, mSize, Bitmap.Config.ARGB_8888);
        final Handler handler = new Handler();
        new Runnable() {
            @Override
            public void run() {
                final Call<Token> call = Manager.getService().getToken();
                call.enqueue(new Callback<Token>() {
                    @Override
                    public void onResponse(Call<Token> call, Response<Token> response) {
                        if (response.isSuccessful()) {
                            Token token = response.body();
                            if (token != null) {
                                mToken = token.token;
                                mKey = token.key;
                                mProgressBar.setVisibility(View.GONE);
                                generateQRCode();
                                drawQRCode();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Token> call, Throwable t) {

                    }
                });
                handler.postDelayed(this, 600000);
            }
        }.run();
        new Runnable() {
            @Override
            public void run() {
                Call<Token> call = Manager.getService().getToken(mToken, mKey);
                call.enqueue(new Callback<Token>() {
                    @Override
                    public void onResponse(Call<Token> call, Response<Token> response) {
                        if (response.isSuccessful()) {
                            Token token = response.body();
                            if (token != null && token.access_token != null) {
                                handler.removeCallbacksAndMessages(null);
                                Manager.setToken(QRActivity.this, token);
                                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(500);
                                Toast.makeText(getApplicationContext(), "Logged in!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(QRActivity.this, LibraryActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                                finishAffinity();
                                startActivity(intent);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Token> call, Throwable t) {

                    }
                });
                handler.postDelayed(this, 5000);
            }
        }.run();
    }

    private void generateQRCode() {
        if (mToken != null) {
            try {
                Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>(3);
                hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                hints.put(EncodeHintType.MARGIN, 0);
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
                BitMatrix bitMatrix = new QRCodeWriter().encode(
                        "https://wearify.seapip.com/login/" + mToken + "/" + mKey,
                        BarcodeFormat.QR_CODE, mSize, mSize, hints);
                int color = Color.parseColor("#222222");
                for (int x = 0; x < bitMatrix.getWidth(); x++) {
                    for (int y = 0; y < bitMatrix.getHeight(); y++) {
                        boolean bit = bitMatrix.get(x, y);
                        boolean ambientBit = bit;
                        if (bit && !(x < 1 || y < 1
                                || x > bitMatrix.getWidth() - 1 || y > bitMatrix.getHeight() - 1)) {
                            ambientBit = !(bitMatrix.get(x - 1, y) && bitMatrix.get(x + 1, y)
                                    && bitMatrix.get(x, y - 1) && bitMatrix.get(x, y + 1));
                        }
                        mQRBitmap.setPixel(x, y, bit ? color : Color.TRANSPARENT);
                        mAmbientQRBitmap.setPixel(x, y, ambientBit ? Color.WHITE : Color.TRANSPARENT);
                    }
                }
                Paint paint = new Paint();
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                Canvas canvas = new Canvas(mQRBitmap);
                Canvas ambientCanvas = new Canvas(mAmbientQRBitmap);
                canvas.drawCircle(mSize / 2f, mSize / 2f, mSize / 8f, paint);
                ambientCanvas.drawCircle(mSize / 2f, mSize / 2f, mSize / 8f, paint);
                mLogo.draw(canvas);
                mLogoBurnIn.draw(ambientCanvas);

            } catch (WriterException e) {
                e.printStackTrace();
            }
        }
    }

    private void drawQRCode() {
        mLayout.setBackgroundColor(mAmbient ? Color.BLACK : Color.parseColor("#eeeeee"));
        mQRCodeView.setImageBitmap(mAmbient ? mAmbientQRBitmap : mQRBitmap);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        mAmbient = true;
        drawQRCode();
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        mAmbient = false;
        drawQRCode();
    }
}
