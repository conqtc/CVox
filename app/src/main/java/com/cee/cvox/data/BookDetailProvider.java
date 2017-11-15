package com.cee.cvox.data;

import android.content.Context;
import android.os.AsyncTask;

import com.cee.cvox.R;
import com.cee.cvox.model.metadata.Book;
import com.cee.cvox.task.DownloadTask;
import com.cee.cvox.task.JsonParseTask;

import java.io.FileInputStream;
import java.util.HashMap;

/**
 * Created by conqtc on 11/6/17.
 */

public class BookDetailProvider implements
    DownloadTask.OnDownloadListener,
    JsonParseTask.OnJsonParseListener {
    private static final String TAG = "___BookDetailProvider";
    public static final String URL_JSON_METADATA_PREFIX = "https://archive.org/metadata/";

    private Context mContext;
    private DownloadTask.OnDownloadListener mDownloadListener;
    private JsonParseTask.OnJsonParseListener mParseListener;

    private Book mBookStart;

    private static HashMap<String, Book> sBookMap = new HashMap<>();
    private static boolean taskInProgress = false;

    public BookDetailProvider(Context context) {
        this.mContext = context;
    }

    public void setDownloadListener(DownloadTask.OnDownloadListener listener) {
        this.mDownloadListener = listener;
    }

    public void setParseListener(JsonParseTask.OnJsonParseListener listener) {
        this.mParseListener = listener;
    }

    public Book getBookDetail(Book book) {
        if (sBookMap.containsKey(book.guid)) {
            Book bookDetail = sBookMap.get(book.guid);
            return bookDetail;
        } else {
            mBookStart = book;
            startDownload(book);

            return null;
        }
    }

    private void startDownload(Book book) {
        DownloadTask task = new DownloadTask(mContext, UniqueProvider.generateTaskId(), book.guid + ".json");
        task.addListener(this);
        if (mDownloadListener != null) {
            task.addListener(mDownloadListener);
        }
        //task.execute(URL_JSON_METADATA_PREFIX + book.guid);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, URL_JSON_METADATA_PREFIX + book.guid);

    }

    private void startParsing() {
        // start a new task to parse
        JsonParseTask task = new JsonParseTask(mContext, UniqueProvider.generateTaskId(), mBookStart);
        task.addListener(this);
        if (mParseListener != null) {
            task.addListener(mParseListener);
        }

        try {
            FileInputStream file = mContext.openFileInput(mBookStart.guid + ".json");
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
    public void onPreJsonParse(int taskId) {

    }

    @Override
    public void onPostJsonParse(int taskId, Book result) {
        taskInProgress = false;
        sBookMap.put(result.guid, result);
    }

    @Override
    public void onJsonParseError(int taskId, String message) {

    }
}
