package com.seapip.thomas.wearify;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.Display;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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
        setContentView(R.layout.activity_main);
        Display display = getWindowManager().getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);
        int inset = (size.x - (int) Math.sqrt(size.x * size.x / 2)) / 2;
        mSize = getResources().getConfiguration().isScreenRound() ? size.x / 3 * 2 : size.x - size.x / 10;
        mQRCodeView = (ImageView) findViewById(R.id.QRCode);
        mLogo = getDrawable(R.drawable.ic_logo);
        mLogoBurnIn = getDrawable(R.drawable.ic_logo_burn_in);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://wearify.seapip.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        WearifyService service = retrofit.create(WearifyService.class);
        Call<Token> call = service.getToken();
        call.enqueue(new Callback<Token>() {
            @Override
            public void onResponse(Call<Token> call, Response<Token> response) {
                if (response.isSuccessful()) {
                    Token token = response.body();
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
    }

    private void drawQRCode(boolean ambient) {
        String loginUri = "https://wearify.seapip.com/login/" + mToken + "/" + mKey;
        QRCodeWriter writer = new QRCodeWriter();
        try {
            Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>(3);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2); /* default = 4 */
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M); /* default = 4 */
            BitMatrix bitMatrix = writer.encode(loginUri, BarcodeFormat.QR_CODE,
                    mSize, mSize, hints);
            Bitmap bmp = Bitmap.createBitmap(mSize, mSize, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < bitMatrix.getWidth(); x++) {
                for (int y = 0; y < bitMatrix.getHeight(); y++) {
                    boolean bit = bitMatrix.get(x, y);
                    if (ambient && bit && !(x < 1 || y < 1
                            || x > bitMatrix.getWidth() - 1 || y > bitMatrix.getHeight() - 1)) {
                        bit = !(bitMatrix.get(x - 1, y) && bitMatrix.get(x + 1, y)
                                && bitMatrix.get(x, y - 1) && bitMatrix.get(x, y + 1));
                    }
                    bmp.setPixel(x, y, bit ? Color.WHITE : Color.TRANSPARENT);
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
