package com.seapip.thomas.wearify;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

public class RoundImageButtonView extends ImageView {

    private Paint mBackgroundPaint;
    private Paint mTouchPaint;
    private Paint mBorderPaint;
    private int mTint;
    private int mClickAlpha;

    public RoundImageButtonView(Context context) {
        super(context);
    }

    public RoundImageButtonView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray defaultTypedArray = context.obtainStyledAttributes(attrs,
                new int[]{android.R.attr.background});
        int backgroundColor = defaultTypedArray.getColor(0, Color.TRANSPARENT);
        defaultTypedArray.recycle();
        setBackground(new ColorDrawable(Color.TRANSPARENT));
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(backgroundColor);
        mBackgroundPaint.setAntiAlias(true);
        mTouchPaint = new Paint();
        mTouchPaint.setColor(Color.TRANSPARENT);
        mTouchPaint.setAntiAlias(true);
        mBorderPaint = new Paint();
        mBorderPaint.setColor(Color.TRANSPARENT);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(4);
        mBorderPaint.setAntiAlias(true);
        if(getImageTintList() != null) {
            mTint = getImageTintList().getDefaultColor();
        }
    }

    public RoundImageButtonView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RoundImageButtonView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setTint(int tint) {
        mTint = tint;
        invalidate();
    }

    public void setBorder(int border) {
        mBorderPaint.setColor(border);
        invalidate();
    }

    public void setClickAlpha(int alpha) {
        mClickAlpha = alpha;
    }

    @Override
    public void setBackgroundColor(int color) {
        super.setBackgroundColor(color);
        setBackground(new ColorDrawable(Color.TRANSPARENT));
        mBackgroundPaint.setColor(color);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        canvas.drawCircle(width / 2f, height / 2f, width / 2f, mBackgroundPaint);
        canvas.drawCircle(width / 2f, height / 2f, width / 2f - 4f, mBorderPaint);
        canvas.drawCircle(width / 2f, height / 2f, width / 2f, mTouchPaint);
        Drawable drawable = getDrawable();
        if (drawable != null) {
            int size = (int) Math.round(width / 10 * 6.2f);
            drawable.setTint(mTint);
            drawable.setAlpha(Color.alpha(mTint));
            drawable.setBounds(width / 2 - size / 2, height / 2 - size / 2,
                    width / 2 + size / 2, height / 2 + size / 2);
            drawable.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(isClickable()) {
                    mTouchPaint.setColor(Color.argb(mClickAlpha, Color.red(mTint), Color.green(mTint), Color.blue(mTint)));
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchPaint.setColor(Color.TRANSPARENT);
                invalidate();
                break;
        }
        return super.onTouchEvent(event);
    }
}
