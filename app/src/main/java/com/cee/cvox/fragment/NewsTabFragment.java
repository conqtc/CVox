package com.cee.cvox.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cee.cvox.BookDetailActivity;
import com.cee.cvox.R;
import com.cee.cvox.common.Helper;
import com.cee.cvox.data.BookProvider;
import com.cee.cvox.data.CoverProvider;
import com.cee.cvox.model.RSS.RSSFeed;
import com.cee.cvox.model.metadata.Book;
import com.cee.cvox.task.BooksParseTask;
import com.cee.cvox.task.DownloadTask;

import java.util.List;

/**
 * Created by conqtc on 10/10/17.
 */

public class NewsTabFragment extends BaseTabFragment implements
        DownloadTask.OnDownloadListener,
        BooksParseTask.OnBooksParseListener {
    private static final String TAG = "___NewsTab";

    private RecyclerView mRecyclerView;
    private BookListAdapter mAdapter;
    private ProgressBarFragment mProgressBar;
    private RetryFragment mRetryFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mView == null) {
            mView = inflater.inflate(R.layout.fragment_tab_news, container, false);

            mRecyclerView = (RecyclerView) mView.findViewById(R.id.book_list);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(mView.getContext()));

            BookProvider provider = new BookProvider(getContext());
            provider.setDownloadListener(this);
            provider.setParseListener(this);

            mAdapter = new BookListAdapter(provider);
            mRecyclerView.setAdapter(mAdapter);
        }

        return mView;
    }

    @Override
    public void onPreDownload(int taskId) {
        mProgressBar = new ProgressBarFragment();
        getChildFragmentManager().beginTransaction().add(R.id.tab_news_progress, mProgressBar).commitAllowingStateLoss();
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
        getChildFragmentManager().beginTransaction().add(R.id.tab_news_progress, mRetryFragment).commitAllowingStateLoss();
        //mRetryFragment.setRetryListener(this);
    }

    @Override
    public void onBooksPreParse(int taskId) {

    }

    @Override
    public void onBooksPostParse(int taskId, List<Book> result) {
        if (mProgressBar != null) {
            getChildFragmentManager().beginTransaction().remove(mProgressBar).commitAllowingStateLoss();
        }

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    class BookListAdapter extends RecyclerView.Adapter<BookHolder> {
        private BookProvider mProvider;
        private CoverProvider mCoverProvider = new CoverProvider(getContext());

        public BookListAdapter(BookProvider provider) {
            this.mProvider = provider;

        }

        @Override
        public BookHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final View itemView = inflater.inflate(R.layout.card_news_book, parent, false);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = mRecyclerView.getChildLayoutPosition(v);
                    Book book = mProvider.getBooks().get(position);

                    Intent intent = new Intent(getContext(), BookDetailActivity.class);
                    intent.putExtra("book", book);
                    startActivity(intent);
                }
            });

            return new BookHolder(itemView);
        }

        @Override
        public void onBindViewHolder(BookHolder holder, int position) {
            Book item = mProvider.getBooks().get(position);

            Bitmap bitmap = mCoverProvider.getCoverBitmap(item, holder.ivCover);
            holder.ivCover.setImageBitmap(bitmap);

            holder.tvTitle.setText(item.title);
            holder.tvPubdate.setText(Helper.milliToString(item.pubDate, RSSFeed.TIME_FORMAT_PUBDATE_COMPACT));
            holder.tvDescription.setText(item.description);
        }

        @Override
        public int getItemCount() {
            return mProvider.getBooks().size();
        }
    }

    static class BookHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle;
        TextView tvPubdate;
        TextView tvDescription;

        public BookHolder(View itemView) {
            super(itemView);
            ivCover = (ImageView) itemView.findViewById(R.id.ivnCover);
            tvTitle = (TextView) itemView.findViewById(R.id.tvnTitle);
            tvPubdate = (TextView) itemView.findViewById(R.id.tvnPubdate);
            tvDescription = (TextView) itemView.findViewById(R.id.tvnDescription);
        }
    }
}
