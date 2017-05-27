package com.seapip.thomas.wearify;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class CircularProgressView extends ImageView {

    private Paint backgroundCirclePaint;
    private Paint foregroundArcPaint;
    private float mProgress;

    public CircularProgressView(Context context) {
        super(context);
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        backgroundCirclePaint = new Paint();
        backgroundCirclePaint.setColor(Color.parseColor("#141414"));
        backgroundCirclePaint.setAntiAlias(true);
        foregroundArcPaint = new Paint();
        foregroundArcPaint.setColor(Color.WHITE);
        foregroundArcPaint.setAntiAlias(true);
        mProgress = 0;
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setProgress(float progress) {
        mProgress = progress;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        canvas.drawCircle(width / 2, height / 2, width / 2, backgroundCirclePaint);
        canvas.drawArc(0, 0, width, height, -90, 3.6f * mProgress, true, foregroundArcPaint);
    }
}
