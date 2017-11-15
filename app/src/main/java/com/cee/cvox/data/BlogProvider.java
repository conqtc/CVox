package com.cee.cvox.data;

import android.content.Context;
import android.os.AsyncTask;

import com.cee.cvox.R;
import com.cee.cvox.model.RSS.RSSFeed;
import com.cee.cvox.task.DownloadTask;
import com.cee.cvox.task.RSSParseTask;

import java.io.FileInputStream;

/**
 * Created by conqtc on 10/10/17.
 */

public class BlogProvider implements DownloadTask.OnDownloadListener,
        RSSParseTask.OnRSSParseListener {
    private static final String TAG = "___BlogProvider";

    public static final String FILE_BLOG_XML = "blog.xml";
    public static final String URL_LIBRIVOX_FEED = "https://librivox.org/feed/";

    private Context mContext;
    private DownloadTask.OnDownloadListener mDownloadListener;
    private RSSParseTask.OnRSSParseListener mParseListener;
    private static RSSFeed mFeed = new RSSFeed();

    private static boolean taskInProgress = false;

    public BlogProvider(Context context) {
        this.mContext = context;
    }

    public void setDownloadListener(DownloadTask.OnDownloadListener listener) {
        this.mDownloadListener = listener;
    }

    public void setParseListener(RSSParseTask.OnRSSParseListener listener) {
        this.mParseListener = listener;
    }

    public RSSFeed getFeed() {
        if (mFeed.items.size() == 0 && !taskInProgress) {
            startDownload();
        }

        return mFeed;
    }

    private void startDownload() {
        DownloadTask task = new DownloadTask(mContext, UniqueProvider.generateTaskId(), FILE_BLOG_XML);
        task.addListener(this);
        if (mDownloadListener != null) {
            task.addListener(mDownloadListener);
        }
        //task.execute(URL_LIBRIVOX_FEED);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, URL_LIBRIVOX_FEED);
    }

    private void startParsing() {
        // start a new task to parse
        RSSParseTask task = new RSSParseTask(mContext, UniqueProvider.generateTaskId());
        task.addListener(this);
        if (mParseListener != null) {
            task.addListener(mParseListener);
        }

        try {
            FileInputStream file = mContext.openFileInput(FILE_BLOG_XML);
            //task.execute(file);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, file);
        } catch (Exception ex) {
            if (mDownloadListener != null) {
                mDownloadListener.onNetworkError(0, mContext.getResources().getString(R.string.msg_network_error));
            }
        }
    }

    @Override
    public void onPreParse(int taskId) {
    }

    @Override
    public void onPostParse(int taskId, RSSFeed result) {
        taskInProgress = false;
        this.mFeed = result;
    }

    @Override
    public void onPreDownload(int taskId) {
        taskInProgress = true;
    }

    @Override
    public void onPostDownload(int taskId, DownloadTask.DownloadResult result) {
        startParsing();
    }

    @Override
    public void onNetworkError(int taskId, String message) {

    }
}
