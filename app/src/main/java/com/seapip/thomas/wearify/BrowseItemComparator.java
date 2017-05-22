package com.seapip.thomas.wearify;

import java.util.Comparator;

public class BrowseItemComparator implements Comparator<BrowseItem> {
    @Override
    public int compare(BrowseItem o1, BrowseItem o2) {
        return o2.played_at.compareTo(o1.played_at);
    }
}
