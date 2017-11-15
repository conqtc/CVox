package com.cee.cvox.task;

import android.content.Context;
import android.os.Process;
import android.text.Html;
import android.util.Xml;

import com.cee.cvox.R;
import com.cee.cvox.common.Helper;
import com.cee.cvox.model.RSS.RSSComment;
import com.cee.cvox.model.RSS.RSSFeed;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by conqtc on 10/23/17.
 * Ref: BasicSyncAdapter, Android OpenSource Project
 */

public class CommentParseTask extends BaseTask<String, Void, List<RSSComment>> {
    private static final String TAG = "___CommentParse";

    private List<OnCommentParseListener> mListeners = new ArrayList<>();

    /**
     *
     * @param context
     * @param taskId
     */
    public CommentParseTask(Context context, int taskId) {
        super(context, taskId);
    }

    /**
     *
     * @param listener
     */
    public void addListener(OnCommentParseListener listener) {
        mListeners.add(listener);
    }

    /**
     *
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        for (OnCommentParseListener listener: mListeners) {
            listener.onCommentPreParse(mTaskId);
        }
    }

    /**
     *
     * @param params
     * @return
     */
    @Override
    protected List<RSSComment> doInBackground(String... params) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);

        try {
            return parse(params[0]);
        } catch (Exception ex) {
            for (OnCommentParseListener listener: mListeners) {
                listener.onCommentError(mTaskId, mContext.getString(R.string.msg_network_error));
            }
            return null;
        }
    }

    /**
     *
     * @param addr
     * @return
     * @throws Exception
     */
    private List<RSSComment> parse(String addr) throws Exception {
        final URL url = new URL(addr);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(DownloadTask.NET_READ_TIMEOUT_MILLIS);
        conn.setConnectTimeout(DownloadTask.NET_CONNECT_TIMEOUT_MILLIS);

        conn.connect();
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            // error
            for (OnCommentParseListener listener: mListeners) {
                listener.onCommentError(mTaskId, mContext.getString(R.string.msg_network_error) + " Response code: " + conn.getResponseCode());
            }
            return null;
        }

        InputStream ins = conn.getInputStream();
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
    protected void onPostExecute(List<RSSComment> result) {
        super.onPostExecute(result);

        for (OnCommentParseListener listener: mListeners) {
            listener.onCommentPostParse(mTaskId, result);
        }
    }

    /**
     *
     * @param parser
     * @return
     */
    private List<RSSComment> readFeed(XmlPullParser parser) {
        List<RSSComment> comments = new ArrayList<>();

        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }

                String tag = parser.getName();
                if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM)) {
                    RSSComment comment = readComment(parser);
                    comments.add(comment);
                } else {
                    skip(parser);
                }
            }
        } catch (Exception ex) {
        }

        return comments;
    }

    /**
     *
     * @param parser
     * @return
     * @throws Exception
     */
    private RSSComment readComment(XmlPullParser parser) throws Exception {
        RSSComment item = new RSSComment();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String tag = parser.getName();
            if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_CREATOR)) {
                item.creator = readTag(parser, tag, RSSFeed.TYPE_TEXT);
            } else if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_CONTENT)) {
                String text = readTag(parser, tag, RSSFeed.TYPE_TEXT);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    item.content = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString().trim();
                } else {
                    item.content = Html.fromHtml(text).toString().trim();
                }
            } else if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_PUBDATE)) {
                try {
                    String text = readTag(parser, tag, RSSFeed.TYPE_TEXT);
                    Date date = Helper.stringToDate(text, RSSFeed.TIME_FORMAT_PUBDATE);
                    item.pubDate = date.getTime();
                } catch (Exception ex) {}
            }
            else { skip(parser); }
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
    public static interface OnCommentParseListener {
        public void onCommentPreParse(int taskId);
        public void onCommentPostParse(int taskId, List<RSSComment> result);
        public void onCommentError(int taskId, String message);
    }
}
