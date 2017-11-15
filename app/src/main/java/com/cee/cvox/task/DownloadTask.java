package com.cee.cvox.task;

import android.content.Context;
import android.os.Process;

import com.cee.cvox.R;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by conqtc on 10/19/17.
 */

public class DownloadTask extends BaseTask<String, Integer, DownloadTask.DownloadResult> {
    private static final String TAG = "___Download";

    public static final int NET_CONNECT_TIMEOUT_MILLIS = 15000;  // 15 seconds
    public static final int NET_READ_TIMEOUT_MILLIS = 10000;  // 10 seconds
    private static final int READ_BUFFER_SIZE = 1024;  // 10Kb

    private String mOutputFileName;
    private List<OnDownloadListener> mListeners = new ArrayList<>();

    /**
     *
     * @param context
     * @param taskId
     * @param outputFileName
     */
    public DownloadTask(Context context, int taskId, String outputFileName) {
        super(context, taskId);
        this.mOutputFileName = outputFileName;
    }

    /**
     *
     * @param listener
     */
    public void addListener(OnDownloadListener listener) {
        mListeners.add(listener);
    }

    /**
     *
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        for (OnDownloadListener listener: mListeners) {
            if (mListeners != null) {
                listener.onPreDownload(mTaskId);
            }
        }
    }

    /**
     *
     * @param params
     * @return
     */
    @Override
    protected DownloadResult doInBackground(String... params) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE);

        try {
            final URL url = new URL(params[0]);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(NET_READ_TIMEOUT_MILLIS /* milliseconds */);
            conn.setConnectTimeout(NET_CONNECT_TIMEOUT_MILLIS /* milliseconds */);

            if (isCancelled()) {
                return new DownloadResult(false, "Cancelled", mOutputFileName);
            }

            conn.connect();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return new DownloadResult(false, "[Response Code] " + conn.getResponseCode() + " [Response Message]" + conn.getResponseMessage(), mOutputFileName);
            }

            if (isCancelled()) {
                return new DownloadResult(false, "Cancelled", mOutputFileName);
            }

            InputStream inputStream = conn.getInputStream();
            FileOutputStream fileOutputStream = mContext.openFileOutput(mOutputFileName, Context.MODE_PRIVATE);

            byte data[] = new byte[READ_BUFFER_SIZE];
            int count;
            while ((count = inputStream.read(data)) != -1) {
                fileOutputStream.write(data, 0, count);
                if (isCancelled()) { break; }
            }

            fileOutputStream.close();
            inputStream.close();
        } catch (Exception ex) {
            for (OnDownloadListener listener: mListeners) {
                if (listener != null) {
                    listener.onNetworkError(mTaskId, mContext.getString(R.string.msg_network_error));
                }
            }
            return new DownloadResult(false, "[Exception] " + ex, mOutputFileName);
        }

        if (isCancelled()) {
            // remove incompleted downloaded file
            try {
                mContext.deleteFile(mOutputFileName);
            } catch (Exception ex) {}
            return new DownloadResult(false, "Cancelled", mOutputFileName);
        } else {
            return new DownloadResult(true, "[OK] File downloaded as " + mOutputFileName, mOutputFileName);
        }
    }

    /**
     *
     * @param result
     */
    @Override
    protected void onPostExecute(DownloadResult result) {
        super.onPostExecute(result);

        for (OnDownloadListener listener: mListeners) {
            if (listener != null) {
                listener.onPostDownload(mTaskId, result);
            }
        }
    }

    @Override
    protected void onCancelled(DownloadResult result) {
        super.onCancelled(result);
        for (OnDownloadListener listener: mListeners) {
            if (listener != null) {
                listener.onPostDownload(mTaskId, result);
            }
        }
    }

    /**
     *
     */
    public static class DownloadResult {
        public DownloadResult(boolean b, String s, String o) {
            this.successful = b;
            this.message = s;
            this.output = o;
        }

        public boolean successful;
        public String output;
        public String message;
    }

    /**
     *
     */
    public static interface OnDownloadListener {
        public void onPreDownload(int taskId);
        public void onPostDownload(int taskId, DownloadResult result);
        public void onNetworkError(int taskId, String message);
    }
}
