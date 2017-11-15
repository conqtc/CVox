package com.cee.cvox.task;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;

/**
 * Created by conqtc on 10/23/17.
 */

public class BaseTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
    protected Context mContext;
    protected PowerManager.WakeLock mWakeLock;

    protected int mTaskId = 0;

    public BaseTask(Context context) {
        this.mContext = context;
    }

    public BaseTask(Context context, int taskId) {
        this(context);
        this.mTaskId = taskId;
    }

    protected void setTaskId(int taskId) {
        this.mTaskId = taskId;
    }

    protected  int getTaskId() {
        return mTaskId;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        // take CPU lock to prevent CPU from going off if the user presses the power button during download
        PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        mWakeLock.acquire();
    }

    /**
     * TO BE OVERRIDE
     * @param params
     * @return
     */
    @Override
    protected Result doInBackground(Params... params) {
        return null;
    }

    @Override
    protected void onPostExecute(Result result) {
        mWakeLock.release();
    }


}
