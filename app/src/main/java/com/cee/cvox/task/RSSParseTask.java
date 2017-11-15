package com.cee.cvox.task;

import android.content.Context;
import android.os.Process;
import android.text.Html;
import android.util.Xml;

import com.cee.cvox.common.Helper;
import com.cee.cvox.model.RSS.RSSFeed;
import com.cee.cvox.model.RSS.RSSItem;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by conqtc on 10/23/17.
 * Ref: BasicSyncAdapter, Android OpenSource Project
 */

public class RSSParseTask extends BaseTask<InputStream, Void, RSSFeed> {
    private static final String TAG = "___RSSParse";

    private List<OnRSSParseListener> mListeners = new ArrayList<>();

    /**
     *
     * @param context
     * @param taskId
     */
    public RSSParseTask(Context context, int taskId) {
        super(context, taskId);
    }

    /**
     *
     * @param listener
     */
    public void addListener(OnRSSParseListener listener) {
        mListeners.add(listener);
    }

    /**
     *
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        for (OnRSSParseListener listener: mListeners) {
            listener.onPreParse(mTaskId);
        }
    }

    /**
     *
     * @param params
     * @return
     */
    @Override
    protected RSSFeed doInBackground(InputStream... params) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);

        try {
            return parse(params[0]);
        } catch (Exception ex) {
            return new RSSFeed();
        }
    }

    /**
     *
     * @param ins
     * @return
     * @throws Exception
     */
    private RSSFeed parse(InputStream ins) throws Exception {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(ins, null);
            parser.nextTag();   // rss
            parser.nextTag();   // channel
            return readFeed(parser);
        } finally {
            ins.close();
        }
    }

    /**
     *
     * @param result
     */
    @Override
    protected void onPostExecute(RSSFeed result) {
        super.onPostExecute(result);

        for (OnRSSParseListener listener: mListeners) {
            listener.onPostParse(mTaskId, result);
        }
    }

    /**
     *
     * @param parser
     * @return
     */
    private RSSFeed readFeed(XmlPullParser parser) {
        RSSFeed feed = new RSSFeed();

        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }

                String tag = parser.getName();
                if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM)) {
                    RSSItem item = readItem(parser);
                    feed.items.add(item);
                } else {
                    skip(parser);
                }
            }
        } catch (Exception ex) {
        }

        return feed;
    }

    /**
     *
     * @param parser
     * @return
     * @throws Exception
     */
    private RSSItem readItem(XmlPullParser parser) throws Exception {
        RSSItem item = new RSSItem();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String tag = parser.getName();
            if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_TITLE)) {
                item.title = readTag(parser, tag, RSSFeed.TYPE_TEXT);
            } else if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_DESCRIPTION)) {
                String text = readTag(parser, tag, RSSFeed.TYPE_TEXT);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    item.description = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString().trim();
                } else {
                    item.description = Html.fromHtml(text).toString().trim();
                }
            } else if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_CONTENT)) {
                String text = readTag(parser, tag, RSSFeed.TYPE_TEXT);
                item.contentEncoded = text;
                /*
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    item.contentEncoded = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString().trim();
                } else {
                    item.contentEncoded = Html.fromHtml(text).toString().trim();
                }
                */
            } else if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_COMMENTRSS)) {
                item.commentRss = readTag(parser, tag, RSSFeed.TYPE_TEXT);
            } else if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_SLASH_COMMENTS)) {
                try {
                    item.comments = Integer.parseInt(readTag(parser, tag, RSSFeed.TYPE_TEXT));
                } catch (Exception ex) {}
            } else if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_PUBDATE)) {
                try {
                    String text = readTag(parser, tag, RSSFeed.TYPE_TEXT);
                    Date date = Helper.stringToDate(text, RSSFeed.TIME_FORMAT_PUBDATE);
                    item.pubDate = date.getTime();
                } catch (Exception ex) {}
            }
            else {
                skip(parser);
            }
        }

        return item;
    }

    /**
     *
     * @param parser
     * @param tag
     * @param type
     * @return
     * @throws Exception
     */
    private String readTag(XmlPullParser parser, String tag, int type) throws Exception {
        switch (type) {
            case RSSFeed.TYPE_TEXT:
                return readTagText(parser, tag);

            default:
                return null;
        }
    }

    /**
     *
     * @param parser
     * @param tag
     * @return
     * @throws Exception
     */
    private String readTagText(XmlPullParser parser, String tag) throws Exception {
        String result = null;

        parser.require(XmlPullParser.START_TAG, null, tag);
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        parser.require(XmlPullParser.END_TAG, null, tag);

        return result;
    }

    /**
     *
     * @param parser
     * @throws Exception
     */
    private void skip(XmlPullParser parser) throws Exception {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }

        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;

                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    /**
     *
     */
    public static interface OnRSSParseListener {
        public void onPreParse(int taskId);
        public void onPostParse(int taskId, RSSFeed result);
    }
}
