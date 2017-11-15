package com.cee.cvox.task;

import android.content.Context;
import android.os.Process;
import android.util.Xml;

import com.cee.cvox.common.Helper;
import com.cee.cvox.model.RSS.RSSFeed;
import com.cee.cvox.model.RSS.RSSPodcast;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by conqtc on 11/4/17.
 */

public class PodcastParseTask extends BaseTask<InputStream, Void, List<RSSPodcast>> {
    private static final String TAG = "___PodcastParse";

    private List<OnPodcastParseListener> mListeners = new ArrayList<>();

    /**
     *
     * @param context
     * @param taskId
     */
    public PodcastParseTask(Context context, int taskId) {
        super(context, taskId);
    }

    /**
     *
     * @param listener
     */
    public void addListener(OnPodcastParseListener listener) {
        mListeners.add(listener);
    }

    /**
     *
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        for (OnPodcastParseListener listener: mListeners) {
            listener.onPodcastPreParse(mTaskId);
        }
    }

    /**
     *
     * @param params
     * @return
     */
    @Override
    protected List<RSSPodcast> doInBackground(InputStream... params) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);

        try {
            return parse(params[0]);
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    /**
     *
     * @param ins
     * @return
     * @throws Exception
     */
    private List<RSSPodcast> parse(InputStream ins) throws Exception {
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
    protected void onPostExecute(List<RSSPodcast> result) {
        super.onPostExecute(result);

        for (OnPodcastParseListener listener: mListeners) {
            listener.onPodcastPostParse(mTaskId, result);
        }
    }

    /**
     *
     * @param parser
     * @return
     */
    private List<RSSPodcast> readFeed(XmlPullParser parser) {
        List<RSSPodcast> feed = new ArrayList<>();

        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }

                String tag = parser.getName();
                if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM)) {
                    RSSPodcast item = readItem(parser);
                    feed.add(item);
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
    private RSSPodcast readItem(XmlPullParser parser) throws Exception {
        RSSPodcast item = new RSSPodcast();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String tag = parser.getName();
            if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_TITLE)) {
                item.title = readTag(parser, tag, RSSFeed.TYPE_TEXT);
            } else if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_LINK)) {
                item.url = readTag(parser, tag, RSSFeed.TYPE_TEXT);
            } else if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_SUBTITLE)) {
                item.album = readTag(parser, tag, RSSFeed.TYPE_TEXT);
            } else if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_DURATION)) {
                item.duration = readTag(parser, tag, RSSFeed.TYPE_TEXT);
            } else if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_AUTHOR)) {
                item.artist = readTag(parser, tag, RSSFeed.TYPE_TEXT);
            } else if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_PUBDATE)) {
                try {
                    String text = readTag(parser, tag, RSSFeed.TYPE_TEXT);
                    Date date = Helper.stringToDate(text, RSSFeed.TIME_FORMAT_PUBDATE_PODCAST);
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
    public static interface OnPodcastParseListener {
        public void onPodcastPreParse(int taskId);
        public void onPodcastPostParse(int taskId, List<RSSPodcast> result);
    }
}
