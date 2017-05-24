package com.seapip.thomas.wearify.Spotify;

import java.util.Arrays;

public class Paging<I> {
    public String href;
    public I[] items;
    public int limit;
    public String next;
    public int offset;
    public String previous;
    public int total;
}
