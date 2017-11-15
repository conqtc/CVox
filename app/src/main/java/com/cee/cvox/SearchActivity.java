package com.cee.cvox;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.cee.cvox.common.Helper;
import com.cee.cvox.data.CoverProvider;
import com.cee.cvox.data.UniqueProvider;
import com.cee.cvox.fragment.ProgressBarFragment;
import com.cee.cvox.fragment.RetryFragment;
import com.cee.cvox.model.RSS.RSSFeed;
import com.cee.cvox.model.metadata.Book;
import com.cee.cvox.task.SearchParseTask;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements
        SearchParseTask.OnSearchListener, AdapterView.OnItemClickListener {
    private static final String TAG = "___SearchActivity";

    private ListView mBookList;
    private BookListAdapter mAdapter;
    private ProgressBarFragment mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setupContent();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (query == null || query.isEmpty()) { return; }

            SearchParseTask task = new SearchParseTask(this, UniqueProvider.generateTaskId());
            task.addListener(this);
            //task.execute(query);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, query);
        }
    }

    private void setupContent() {
        mBookList = (ListView) findViewById(R.id.lvsBooks);
        mAdapter = new BookListAdapter(this, R.id.lvsBooks);
        mBookList.setAdapter(mAdapter);
        mBookList.setOnItemClickListener(this);
    }

    @Override
    public void onPreSearch(int taskId) {
        mProgressBar = new ProgressBarFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.activity_search_progress, mProgressBar).commitAllowingStateLoss();
    }

    @Override
    public void onPostSearch(int taskId, List<Book> result) {
        if (mProgressBar != null) {
            getSupportFragmentManager().beginTransaction().remove(mProgressBar).commitAllowingStateLoss();
        }

        if (result == null) {
            // error, let onSearchError handles
        } else if (result.size() == 0) {
            RetryFragment retryFragment = new RetryFragment();
            retryFragment.setContent(R.mipmap.ic_search_accent, getString(R.string.msg_search_result_empty), false);
            getSupportFragmentManager().beginTransaction().add(R.id.activity_search_progress, retryFragment).commitAllowingStateLoss();
        } else {
            mAdapter.setList(result);
        }
    }


    @Override
    public void onSearchError(int taskId, String message) {
        if (mProgressBar != null) {
            getSupportFragmentManager().beginTransaction().remove(mProgressBar).commitAllowingStateLoss();
        }

        RetryFragment retryFragment = new RetryFragment();
        retryFragment.setContent(R.mipmap.ic_warning, message, false);
        getSupportFragmentManager().beginTransaction().add(R.id.activity_search_progress, retryFragment).commitAllowingStateLoss();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Book book = mAdapter.getItem(position);

        Intent intent = new Intent(this, BookDetailActivity.class);
        intent.putExtra("book", book);
        startActivity(intent);
    }

    class BookListAdapter extends ArrayAdapter<Book> {
        private Context mContext;
        private LayoutInflater inflater;

        private CoverProvider mCoverProvider = new CoverProvider(getContext());
        private List<Book> mBooks = new ArrayList<>();

        /**
         *
         * @param context
         * @param resource
         */
        public BookListAdapter(@NonNull Context context, int resource) {
            super(context, resource);

            this.mContext = context;
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }


        @Override
        public int getCount() {
            return this.mBooks.size();
        }

        @Override
        public Book getItem(int position) {
            return this.mBooks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            ViewHolder holder;

            if (view == null) {
                view = inflater.inflate(R.layout.search_item, viewGroup, false);

                holder = new ViewHolder();
                holder.ivCover = (ImageView) view.findViewById(R.id.ivsCover);
                holder.tvTitle = (TextView) view.findViewById(R.id.tvsTitle);
                holder.tvPubdate = (TextView) view.findViewById(R.id.tvsPubdate);
                holder.tvDescription = (TextView) view.findViewById(R.id.tvsDescription);

                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            Book book = this.mBooks.get(position);

            Bitmap bitmap = mCoverProvider.getCoverBitmap(book, holder.ivCover);
            holder.ivCover.setImageBitmap(bitmap);

            holder.tvTitle.setText(book.title);
            holder.tvPubdate.setText(String.format("%s", Helper.milliToString(book.pubDate, RSSFeed.TIME_FORMAT_PUBDATE_COMPACT)));
            holder.tvDescription.setText(book.description);

            return view;
        }

        public void setList(List<Book> list) {
            this.mBooks = list;
            notifyDataSetChanged();
        }
    }

    static class ViewHolder {
        ImageView ivCover;
        TextView tvTitle;
        TextView tvPubdate;
        TextView tvDescription;
    }

}
