package com.seapip.thomas.wearify.Spotify;

public class ImageUtil {
    static public String smallestImageUrl(Image[] images) {
        int size = 0;
        for (Image image : images) {
            if (image.width < size || size == 0) {
                size = image.width;
            }
        }
        for (Image image : images) {
            if (image.width == size) {
                return image.url;
            }
        }
        return null;
    }
    static public String largestImageUrl(Image[] images) {
        int size = 0;
        for (Image image : images) {
            if (image.width > size) {
                size = image.width;
            }
        }
        for (Image image : images) {
            if (image.width == size) {
                return image.url;
            }
        }
        return null;
    }
}
