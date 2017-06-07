package com.seapip.thomas.wearify.Spotify.Objects;

import java.util.Map;

public class Album {
    public String album_type;
    public Artist[] artists;
    public String[] available_markets;
    public Copyright[] copyrights;
    public Map<String, String> external_id;
    public Map<String, String> external_urls;
    public String[] genres;
    public String href;
    public String id;
    public Image[] images;
    public String label;
    public String name;
    public int popularity;
    public String release_date;
    public String release_date_precision;
    public Paging<Track> tracks;
    public String type;
    public String uri;
}
