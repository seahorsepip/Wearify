package com.seapip.thomas.wearify.browse;

import android.graphics.drawable.Drawable;

public class Category extends Item {
    public Category(String title, Drawable image, OnClick onClick) {
        this.title = title;
        this.image = image;
        this.onClick = onClick;
    }
}
