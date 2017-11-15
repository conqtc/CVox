package com.cee.cvox.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cee.cvox.FeedActivity;
import com.cee.cvox.R;
import com.cee.cvox.common.Helper;
import com.cee.cvox.data.BlogProvider;
import com.cee.cvox.model.RSS.RSSFeed;
import com.cee.cvox.model.RSS.RSSItem;
import com.cee.cvox.task.DownloadTask;
import com.cee.cvox.task.RSSParseTask;

/**
 * Created by conqtc on 10/10/17.
 */

public class BlogTabFragment extends BaseTabFragment implements
        DownloadTask.OnDownloadListener,
        RSSParseTask.OnRSSParseListener,
        RetryFragment.OnRetryListener {
    private static final String TAG = "___BlogTab";

    private RecyclerView mRecyclerView;
    private FeedListAdapter mAdapter;
    private ProgressBarFragment mProgressBar;
    private RetryFragment mRetryFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mView == null) {
            mView = inflater.inflate(R.layout.fragment_tab_blog, container, false);

            mRecyclerView = (RecyclerView) mView.findViewById(R.id.feed_list);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(mView.getContext()));

            BlogProvider provider = new BlogProvider(getContext());
            provider.setDownloadListener(this);
            provider.setParseListener(this);

            mAdapter = new FeedListAdapter(provider);
            mRecyclerView.setAdapter(mAdapter);
        }

        return mView;
    }

    @Override
    public void onPreDownload(int taskId) {
        mProgressBar = new ProgressBarFragment();
        getChildFragmentManager().beginTransaction().add(R.id.tab_blog_progress, mProgressBar).commitAllowingStateLoss();
    }

    @Override
    public void onPostDownload(int taskId, DownloadTask.DownloadResult result) {

    }

    @Override
    public void onNetworkError(int taskId, String message) {
        if (mProgressBar != null) {
            getChildFragmentManager().beginTransaction().remove(mProgressBar).commitAllowingStateLoss();
        }

        mRetryFragment = new RetryFragment();
        mRetryFragment.setContent(R.mipmap.ic_warning, getString(R.string.msg_network_error), true);
        getChildFragmentManager().beginTransaction().add(R.id.tab_blog_progress, mRetryFragment).commitAllowingStateLoss();
        mRetryFragment.setRetryListener(this);
    }

    @Override
    public void onPreParse(int taskId) {
    }

    @Override
    public void onPostParse(int taskId, RSSFeed result) {
        if (mProgressBar != null) {
            getChildFragmentManager().beginTransaction().remove(mProgressBar).commitAllowingStateLoss();
        }

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onRetry() {
        // press retry
        // first remove the fragment
        if (mRetryFragment != null) {
            getChildFragmentManager().beginTransaction().remove(mRetryFragment).commitAllowingStateLoss();
        }
        // then try to reload the adapter
        mAdapter.notifyDataSetChanged();
    }

    class FeedListAdapter extends RecyclerView.Adapter<FeedHolder> {
        private BlogProvider mProvider;

        public FeedListAdapter(BlogProvider provider) {
            this.mProvider = provider;
        }

        @Override
        public FeedHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final View itemView = inflater.inflate(R.layout.card_blog_feed, parent, false);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = mRecyclerView.getChildLayoutPosition(v);
                    //Toast.makeText(getContext(), mProvider.getFeed().items.get(position).title, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(getContext(), FeedActivity.class);
                    intent.putExtra(FeedActivity.FEED_POSITION, position);
                    startActivity(intent);
                }
            });

            return new FeedHolder(itemView);
        }

        @Override
        public void onBindViewHolder(FeedHolder holder, int position) {
            RSSItem item = mProvider.getFeed().items.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvSub.setText(Helper.milliToString(item.pubDate, RSSFeed.TIME_FORMAT_PUBDATE_COMPACT));
            holder.tvDescription.setText(item.description);
            holder.tvComments.setText(String.format("%d comment", item.comments) + (item.comments > 1 ? "s" : ""));
        }

        @Override
        public int getItemCount() {
            return mProvider.getFeed().items.size();
        }
    }

    static class FeedHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvSub;
        TextView tvDescription;
        TextView tvComments;

        public FeedHolder(View itemView) {
            super(itemView);
            tvTitle = (TextView) itemView.findViewById(R.id.tvFeedTitle);
            tvSub = (TextView) itemView.findViewById(R.id.tvFeedSub);
            tvDescription = (TextView) itemView.findViewById(R.id.tvFeedDescription);
            tvComments = (TextView) itemView.findViewById(R.id.tvbComments);
        }
    }
}
