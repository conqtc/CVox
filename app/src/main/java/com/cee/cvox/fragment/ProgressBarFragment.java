package com.cee.cvox.fragment;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.cee.cvox.R;

/**
 * Created by conqtc on 10/13/17.
 */

public class ProgressBarFragment extends Fragment {
    private static final int PROGRESS_BAR_WIDTH = 100;
    private static final int PROGRESS_BAR_HEIGHT = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ProgressBar progressBar = new ProgressBar(container.getContext());
        progressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(getContext(), R.color.colorAccent), PorterDuff.Mode.SRC_IN);
        if (container instanceof FrameLayout) {
            FrameLayout.LayoutParams layoutParams =
                    new FrameLayout.LayoutParams(PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, Gravity.CENTER);
            progressBar.setLayoutParams(layoutParams);
        }

        return progressBar;
    }
}
