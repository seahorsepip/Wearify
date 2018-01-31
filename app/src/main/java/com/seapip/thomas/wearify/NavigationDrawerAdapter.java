package com.seapip.thomas.wearify;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.wearable.view.drawer.WearableNavigationDrawer;

public class NavigationDrawerAdapter extends WearableNavigationDrawer.WearableNavigationDrawerAdapter {

    private int mPosition;
    private Context mContext;

    public NavigationDrawerAdapter(int position, Context context) {
        mPosition = position;
        mContext = context;
    }

    @Override
    public String getItemText(int i) {
        switch (i) {
            case 0:
                return "Library";
            case 1:
                return "Settings";
        }
        return null;
    }

    @Override
    public Drawable getItemDrawable(int i) {
        int id;
        switch (i) {
            default:
            case 0:
                id = R.drawable.ic_library_music_black_24dp;
                break;
            case 1:
                id = R.drawable.ic_settings_black_24dp;
                break;
        }
        Drawable drawable = mContext.getDrawable(id);
        drawable.setTint(Color.WHITE);
        return drawable;
    }

    @Override
    public void onItemSelected(int i) {
        Intent intent;
        switch (i) {
            case 0:
                intent = new Intent(mContext, LibraryActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                break;
            case 1:
            default:
                intent = new Intent(mContext, SettingsActivity.class);
                break;
        }
        if (mPosition != i) mContext.startActivity(intent);

    }

    @Override
    public int getCount() {
        return 2;
    }
}
