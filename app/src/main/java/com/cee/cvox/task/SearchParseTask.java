package com.cee.cvox.task;

import android.content.Context;
import android.os.Process;
import android.text.Html;
import android.util.Xml;

import com.cee.cvox.R;
import com.cee.cvox.common.Helper;
import com.cee.cvox.model.RSS.RSSFeed;
import com.cee.cvox.model.metadata.Book;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by conqtc on 11/5/17.
 */

public class SearchParseTask extends BaseTask<String, Void, List<Book>> {
    private static final String TAG = "___SearchParse";

    public static final String URL_SEARCH_FORMAT = "https://archive.org/advancedsearch.php?q=(%s)+AND+collection:(librivoxaudio)&output=rss";

    private List<OnSearchListener> mListeners = new ArrayList<>();

    /**
     *
     * @param context
     * @param taskId
     */
    public SearchParseTask(Context context, int taskId) {
        super(context, taskId);
    }

    /**
     *
     * @param listener
     */
    public void addListener(OnSearchListener listener) {
        mListeners.add(listener);
    }

    /**
     *
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        for (OnSearchListener listener: mListeners) {
            listener.onPreSearch(mTaskId);
        }
    }

    /**
     *
     * @param params
     * @return
     */
    @Override
    protected List<Book> doInBackground(String... params) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);

        try {
            return parse(params[0]);
        } catch (Exception ex) {
            for (OnSearchListener listener: mListeners) {
                listener.onSearchError(mTaskId, mContext.getString(R.string.msg_network_error));
            }
            return null;
        }
    }

    /**
     *
     * @param query
     * @return
     * @throws Exception
     */
    private List<Book> parse(String query) throws Exception {
        String addr = String.format(URL_SEARCH_FORMAT, query);
        final URL url = new URL(addr);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(DownloadTask.NET_READ_TIMEOUT_MILLIS);
        conn.setConnectTimeout(DownloadTask.NET_CONNECT_TIMEOUT_MILLIS);
        conn.connect();
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            // unable to connect
            for (OnSearchListener listener: mListeners) {
                listener.onSearchError(mTaskId, mContext.getString(R.string.msg_network_error) + " Response code: " + conn.getResponseCode());
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
    protected void onPostExecute(List<Book> result) {
        super.onPostExecute(result);

        for (OnSearchListener listener: mListeners) {
            listener.onPostSearch(mTaskId, result);
        }
    }

    /**
     *
     * @param parser
     * @return
     */
    private List<Book> readFeed(XmlPullParser parser) {
        List<Book> feed = new ArrayList<>();

        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }

                String tag = parser.getName();
                if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM)) {
                    Book item = readItem(parser);
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
    private Book readItem(XmlPullParser parser) throws Exception {
        Book item = new Book();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String tag = parser.getName();
            if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_TITLE)) {
                item.title = readTag(parser, tag, RSSFeed.TYPE_TEXT);
            } else if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_DESCRIPTION)) {
                String text = readTag(parser, tag, RSSFeed.TYPE_TEXT);
                // get rid of img tag and redundant text after ...
                text = text.replaceAll("<img.+?>", "");
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString().trim();
                } else {
                    text = Html.fromHtml(text).toString().trim();
                }
                int dotPos = text.indexOf("...");
                if (dotPos > 0) {
                    text = text.substring(0, dotPos + 3);
                }

                item.description = text;
            } else if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_LINK)) {
                item.link = readTag(parser, tag, RSSFeed.TYPE_TEXT).trim();
                item.guid = Helper.extractGuidFromLink(item.link);
            } else if (tag.equals(RSSFeed.RSS_CHANNEL_ITEM_PUBDATE)) {
                try {
                    String text = readTag(parser, tag, RSSFeed.TYPE_TEXT);
                    Date date = Helper.stringToDate(text, RSSFeed.TIME_FORMAT_PUBDATE_BOOK);
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

    public static interface OnSearchListener {
        public void onPreSearch(int taskId);
        public void onPostSearch(int taskId, List<Book> result);
        public void onSearchError(int taskId, String message);
    }
}
