package com.seapip.thomas.wearify;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.wearable.view.drawer.WearableNavigationDrawer;

public class NavigationDrawerAdapter extends WearableNavigationDrawer.WearableNavigationDrawerAdapter {
    private Context mContext;

    public NavigationDrawerAdapter(Context context) {
        mContext = context;
    }

    @Override
    public String getItemText(int i) {
        switch (i) {
            case 0:
                return "Test A";
            case 1:
                return "Test B";
            case 2:
                return "Test C";
        }
        return null;
    }

    @Override
    public Drawable getItemDrawable(int i) {
        return mContext.getDrawable(R.drawable.ic_logo);
        //return null;
    }

    @Override
    public void onItemSelected(int i) {

    }

    @Override
    public int getCount() {
        return 3;
    }
}
