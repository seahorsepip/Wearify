package com.seapip.thomas.wearify.spotify.objects;

public class CursorPaging<I> {
    public String href;
    public I[] items;
    public int limit;
    public String next;
    public Cursor cursors;
    public int total;
}
