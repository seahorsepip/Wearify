package com.seapip.thomas.wearify.Browse;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.seapip.thomas.wearify.NavigationDrawerAdapter;
import com.seapip.thomas.wearify.R;

public class Activity extends WearableActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setDrawers(WearableDrawerLayout layout,
                           WearableNavigationDrawer navigationDrawer,
                           WearableActionDrawer actionDrawer) {
        // Main Wearable Drawer Layout that wraps all content
        layout.peekDrawer(Gravity.BOTTOM);

        // Top Navigation Drawer
        navigationDrawer.setAdapter(new NavigationDrawerAdapter(this));

        // Bottom Action Drawer
        Menu menu = actionDrawer.getMenu();
        final AnimatedVectorDrawable drawable = (AnimatedVectorDrawable) getDrawable(R.drawable.ic_audio_waves_animated);
        menu.add("Now Playing").setIcon(null);
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final LinearLayout view = (LinearLayout) layoutInflater.inflate(R.layout.action_drawer_view, null);
        actionDrawer.setPeekContent(view);
        final ImageView actionImage = (ImageView) view.findViewById(R.id.action_image);
        drawable.setTint(Color.BLACK);
        drawable.setAlpha(180);
        actionImage.setImageDrawable(drawable);
        drawable.start();
    }

    public void setGradientOverlay(RecyclerView recyclerView, final ImageView imageView) {
        Display display = getWindowManager().getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);
        final Bitmap overlayBitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888);
        final Canvas overlayCanvas = new Canvas(overlayBitmap);
        final int overlayColorStop = Color.parseColor("#141414");
        final Paint gradient = new Paint();
        final Paint solid = new Paint();
        solid.setColor(overlayColorStop);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int offset = recyclerView.computeVerticalScrollOffset();
                if (offset < size.y) {
                    overlayCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    Shader shader = new LinearGradient(0, -offset - size.y * 2, 0, size.y - offset, Color.TRANSPARENT,
                            overlayColorStop, Shader.TileMode.CLAMP);
                    gradient.setShader(shader);
                    overlayCanvas.drawRect(0, -offset, size.x, size.y - offset, gradient);
                    overlayCanvas.drawRect(0, size.y - offset, size.x, size.y, solid);
                } else {
                    overlayCanvas.drawColor(overlayColorStop);
                }
                imageView.setImageBitmap(overlayBitmap);

            }
        });
    }
}
