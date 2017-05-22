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
import android.util.Log;
import android.view.Display;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.seapip.thomas.wearify.Wearify.Token;
import com.seapip.thomas.wearify.Wearify.Manager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends WearableActivity {

    private String mToken;
    private String mKey;
    private ImageView mQRCodeView;
    private int mSize;
    private Drawable mLogo;
    private Drawable mLogoBurnIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAmbientEnabled();
        setContentView(R.layout.activity_login);
        Display display = getWindowManager().getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);
        mSize = getResources().getConfiguration().isScreenRound() ? size.x / 3 * 2 : size.x - size.x / 20;
        mQRCodeView = (ImageView) findViewById(R.id.QRCode);
        mLogo = getDrawable(R.drawable.ic_logo);
        mLogoBurnIn = getDrawable(R.drawable.ic_logo_burn_in);
        Manager.init(getApplicationContext());
        final Handler handler = new Handler();
        final Runnable qrCodeService = new Runnable() {
            @Override
            public void run() {
                final Call<Token> call = Manager.getService().getToken();
                call.enqueue(new Callback<Token>() {
                    @Override
                    public void onResponse(Call<Token> call, Response<Token> response) {
                        if (response.isSuccessful()) {
                            Token token = response.body();
                            Log.d("WEARIFY", token.toString());
                            if (token != null) {
                                mToken = token.token;
                                mKey = token.key;
                                drawQRCode(false);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Token> call, Throwable t) {

                    }
                });
                handler.postDelayed(this, 600000);
            }
        };
        final Runnable loginService = new Runnable() {
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
                                Manager.setToken(token);
                                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                v.vibrate(500);
                                Toast.makeText(getApplicationContext(), "Logged in!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                finish();
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
        };
        Manager.getToken(new com.seapip.thomas.wearify.Wearify.Callback() {
            @Override
            public void onSuccess(Token token) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(500);
                Toast.makeText(getApplicationContext(), "Logged in!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                finish();
                startActivity(intent);
            }

            @Override
            public void onError() {
                qrCodeService.run();
                loginService.run();
            }
        });
    }

    private void drawQRCode(boolean ambient) {
        if(mToken != null) {
            String loginUri = "https://wearify.seapip.com/login/" + mToken + "/" + mKey;
            QRCodeWriter writer = new QRCodeWriter();
            try {
                Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>(3);
                hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                hints.put(EncodeHintType.MARGIN, 0);
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
                BitMatrix bitMatrix = writer.encode(loginUri, BarcodeFormat.QR_CODE,
                        mSize, mSize, hints);
                Bitmap bmp = Bitmap.createBitmap(mSize, mSize, Bitmap.Config.ARGB_8888);
                int color = ambient ? Color.WHITE : Color.parseColor("#999999");
                for (int x = 0; x < bitMatrix.getWidth(); x++) {
                    for (int y = 0; y < bitMatrix.getHeight(); y++) {
                        boolean bit = bitMatrix.get(x, y);
                        if (ambient && bit && !(x < 1 || y < 1
                                || x > bitMatrix.getWidth() - 1 || y > bitMatrix.getHeight() - 1)) {
                            bit = !(bitMatrix.get(x - 1, y) && bitMatrix.get(x + 1, y)
                                    && bitMatrix.get(x, y - 1) && bitMatrix.get(x, y + 1));
                        }
                        bmp.setPixel(x, y, bit ? color : Color.TRANSPARENT);
                    }
                }
                Paint paint = new Paint();
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                Canvas canvas = new Canvas(bmp);
                canvas.drawCircle(mSize / 2, mSize / 2, mSize / 8, paint);
                Drawable logo = ambient ? mLogoBurnIn : mLogo;
                logo.setBounds(mSize / 2 - mSize / 10, mSize / 2 - mSize / 10,
                        mSize / 2 + mSize / 10, mSize / 2 + mSize / 10);
                logo.draw(canvas);
                mQRCodeView.setImageBitmap(bmp);

            } catch (WriterException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        drawQRCode(true);
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        drawQRCode(false);
    }
}
