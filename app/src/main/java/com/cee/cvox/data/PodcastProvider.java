package com.cee.cvox.data;

import android.content.Context;
import android.os.AsyncTask;

import com.cee.cvox.R;
import com.cee.cvox.model.RSS.RSSPodcast;
import com.cee.cvox.task.DownloadTask;
import com.cee.cvox.task.PodcastParseTask;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by conqtc on 11/4/17.
 */

public class PodcastProvider implements DownloadTask.OnDownloadListener, PodcastParseTask.OnPodcastParseListener {
    private static final String TAG = "___PodcastProvider";

    public static final String FILE_PODCAST_XML = "podcast.xml";
    public static final String URL_LIBRIVOX_PODCAST = "https://librivox.org/podcast.xml";

    private Context mContext;
    private DownloadTask.OnDownloadListener mDownloadListener;
    private PodcastParseTask.OnPodcastParseListener mParseListener;
    private static List<RSSPodcast> sPodcasts = new ArrayList<>();

    private static boolean taskInProgress = false;

    public PodcastProvider(Context context) {
        this.mContext = context;
    }

    public void setDownloadListener(DownloadTask.OnDownloadListener listener) {
        this.mDownloadListener = listener;
    }

    public void setParseListener(PodcastParseTask.OnPodcastParseListener listener) {
        this.mParseListener = listener;
    }

    public List<RSSPodcast> getPodcasts() {
        if (sPodcasts.size() == 0 && !taskInProgress) {
            startDownload();
        }

        return sPodcasts;
    }

    private void startDownload() {
        DownloadTask task = new DownloadTask(mContext, UniqueProvider.generateTaskId(), FILE_PODCAST_XML);
        task.addListener(this);
        if (mDownloadListener != null) {
            task.addListener(mDownloadListener);
        }
        //task.execute(URL_LIBRIVOX_PODCAST);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, URL_LIBRIVOX_PODCAST);
    }

    private void startParsing() {
        // start a new task to parse
        PodcastParseTask task = new PodcastParseTask(mContext, UniqueProvider.generateTaskId());
        task.addListener(this);
        if (mParseListener != null) {
            task.addListener(mParseListener);
        }

        try {
            FileInputStream file = mContext.openFileInput(FILE_PODCAST_XML);
            //task.execute(file);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, file);
        } catch (Exception ex) {
            if (mDownloadListener != null) {
                mDownloadListener.onNetworkError(0, mContext.getResources().getString(R.string.msg_network_error));
            }
        }
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

    @Override
    public void onPodcastPreParse(int taskId) {

    }

    @Override
    public void onPodcastPostParse(int taskId, List<RSSPodcast> result) {
        taskInProgress = false;
        sPodcasts = result;
    }
}
