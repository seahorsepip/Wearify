package com.seapip.thomas.wearify.Spotify;

import java.util.Arrays;
import java.util.Map;

public class Playlist {
    public boolean collaborative;
    public String description;
    public Map<String, String> external_urls;
    public Followers followers;
    public String href;
    public String id;
    public Image[] images;
    public String name;
    public User owner;
    public String snapshot_id;
    public Paging<PlaylistTrack> tracks;
    public String type;
    public String uri;
}
