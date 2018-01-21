package com.seapip.thomas.wearify.browse;

import android.graphics.drawable.Drawable;

public class ActionButton extends Item {
    public ActionButton() {
    }

    public ActionButton(Drawable icon) {
        this.icon = icon;
    }

    public ActionButton(Drawable icon, String text) {
        this.icon = icon;
        this.text = text;
    }

    public Drawable icon;
    public int iconColor;
    public int backgroundColor;
    public String text;
}
