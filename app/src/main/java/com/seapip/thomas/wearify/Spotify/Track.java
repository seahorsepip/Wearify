package com.seapip.thomas.wearify.Spotify;

import java.util.Arrays;
import java.util.Map;

public class Track {
    public Album album;
    public Artist[] artists;
    public String[] available_markets;
    public int disc_number;
    public int duration_ms;
    public boolean explicit;
    public Map<String, String> external_ids;
    public Map<String, String> external_urls;
    public String href;
    public String id;
    public Boolean is_playable;
    public TrackLink linked_from;
    public String name;
    public int popularity;
    public String preview_url;
    public int track_number;
    public String type;
    public String uri;
}
