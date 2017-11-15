package com.cee.cvox.data;

import android.content.Context;
import android.os.AsyncTask;

import com.cee.cvox.R;
import com.cee.cvox.model.metadata.Book;
import com.cee.cvox.task.BooksParseTask;
import com.cee.cvox.task.DownloadTask;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by conqtc on 11/5/17.
 */

public class BookProvider implements
        DownloadTask.OnDownloadListener,
        BooksParseTask.OnBooksParseListener {
    private static final String TAG = "___BookProvider";

    public static final String FILE_BOOKS_XML = "books.xml";
    public static final String URL_ARCHIVE_LIBRIVOX = "http://archive.org/services/collection-rss.php?collection=librivoxaudio";

    private Context mContext;
    private DownloadTask.OnDownloadListener mDownloadListener;
    private BooksParseTask.OnBooksParseListener mParseListener;
    private static List<Book> sBooks = new ArrayList<>();

    private static boolean taskInProgress = false;

    public BookProvider(Context context) {
        this.mContext = context;
    }

    public void setDownloadListener(DownloadTask.OnDownloadListener listener) {
        this.mDownloadListener = listener;
    }

    public void setParseListener(BooksParseTask.OnBooksParseListener listener) {
        this.mParseListener = listener;
    }

    public List<Book> getBooks() {
        if (sBooks.size() == 0 && !taskInProgress) {
            startDownload();
        }

        return sBooks;
    }

    private void startDownload() {
        DownloadTask task = new DownloadTask(mContext, UniqueProvider.generateTaskId(), FILE_BOOKS_XML);
        task.addListener(this);
        if (mDownloadListener != null) {
            task.addListener(mDownloadListener);
        }
        //task.execute(URL_ARCHIVE_LIBRIVOX);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, URL_ARCHIVE_LIBRIVOX);
    }

    private void startParsing() {
        // start a new task to parse
        BooksParseTask task = new BooksParseTask(mContext, UniqueProvider.generateTaskId());
        task.addListener(this);
        if (mParseListener != null) {
            task.addListener(mParseListener);
        }

        try {
            FileInputStream file = mContext.openFileInput(FILE_BOOKS_XML);
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
    public void onBooksPreParse(int taskId) {
    }

    @Override
    public void onBooksPostParse(int taskId, List<Book> result) {
        taskInProgress = false;
        sBooks = result;
    }
}
