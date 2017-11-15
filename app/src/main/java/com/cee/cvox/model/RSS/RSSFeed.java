package com.cee.cvox.model.RSS;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by conqtc on 10/13/17.
 */

public class RSSFeed {
    public static final String TIME_FORMAT_PUBDATE = "EEE, d MMM yyyy HH:mm:ss";
    public static final String TIME_FORMAT_PUBDATE_PODCAST = "d MMM yyyy HH:mm:ss";
    public static final String TIME_FORMAT_PUBDATE_BOOK = "EEE, d MMM yyyy HH:mm:ss";
    public static final String TIME_FORMAT_PUBDATE_COMPACT = "EEE, d MMM yyyy HH:mm";

    public static final int TYPE_TEXT = 0;
    public static final int TYPE_CDATA = 1;
    public static final int TYPE_TIME = 2;
    public static final int TYPE_INT = 4;

    public static final String RSS = "rss";
    public static final String RSS_CHANNEL = "channel";
    public static final String RSS_CHANNEL_ITEM = "item";
    public static final String RSS_CHANNEL_ITEM_TITLE = "title";
    public static final String RSS_CHANNEL_ITEM_LINK = "link";
    public static final String RSS_CHANNEL_ITEM_PUBDATE = "pubDate";
    public static final String RSS_CHANNEL_ITEM_GUID = "guid";
    public static final String RSS_CHANNEL_ITEM_DESCRIPTION = "description";
    public static final String RSS_CHANNEL_ITEM_CONTENT = "content:encoded";
    public static final String RSS_CHANNEL_ITEM_COMMENTRSS = "wfw:commentRss";
    public static final String RSS_CHANNEL_ITEM_COMMENTS = "comments";
    public static final String RSS_CHANNEL_ITEM_SLASH_COMMENTS = "slash:comments";

    // Comments
    public static final String RSS_CHANNEL_ITEM_CREATOR = "dc:creator";

    // Podcast
    public static final String RSS_CHANNEL_ITEM_SUBTITLE = "itunes:subtitle";
    public static final String RSS_CHANNEL_ITEM_DURATION = "itunes:duration";
    public static final String RSS_CHANNEL_ITEM_AUTHOR = "itunes:author";

    public List<RSSItem> items = new ArrayList<>();
}
