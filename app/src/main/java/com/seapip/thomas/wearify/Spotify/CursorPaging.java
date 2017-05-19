package com.seapip.thomas.wearify.Spotify;

import java.util.ArrayList;

public class CursorPaging<I> {
    public String href;
    public I[] items;
    public int limit;
    public String next;
    public Cursor cursors;
    public int total;
}
