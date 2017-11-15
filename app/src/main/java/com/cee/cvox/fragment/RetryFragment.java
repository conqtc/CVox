package com.cee.cvox.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cee.cvox.R;

/**
 * Created by conqtc on 11/6/17.
 */

public class RetryFragment extends Fragment {

    private int mIconId;
    private String mMessage;
    private boolean bIncludeRetry = false;

    private OnRetryListener mListener;

    public void setRetryListener(OnRetryListener listener) {
        this.mListener = listener;
    }

    public void setContent(int iconResourceId, String message, boolean includeRetryButton) {
        this.mIconId = iconResourceId;
        this.mMessage = message;
        this.bIncludeRetry = includeRetryButton;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_retry, container, false);

        view.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        ImageView iv = (ImageView) view.findViewById(R.id.ivrIcon);
        TextView tv = (TextView) view.findViewById(R.id.tvrMessage);
        Button bt = (Button) view.findViewById(R.id.btrRetry);

        iv.setImageResource(mIconId);
        tv.setText(mMessage);

        if (!bIncludeRetry) {
            bt.setVisibility(View.INVISIBLE);
        }

        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onRetry();
                }
            }
        });

        return view;
    }

    public static interface OnRetryListener {
        public void onRetry();
    }

}
