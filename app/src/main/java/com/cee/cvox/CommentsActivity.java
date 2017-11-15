package com.cee.cvox;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.cee.cvox.common.Helper;
import com.cee.cvox.data.UniqueProvider;
import com.cee.cvox.fragment.ProgressBarFragment;
import com.cee.cvox.fragment.RetryFragment;
import com.cee.cvox.model.RSS.RSSComment;
import com.cee.cvox.model.RSS.RSSFeed;
import com.cee.cvox.task.CommentParseTask;

import java.util.ArrayList;
import java.util.List;

public class CommentsActivity extends AppCompatActivity  implements CommentParseTask.OnCommentParseListener {
    private static final String TAG = "___CommentsActivity";
    public static final String COMMENTRSS = "commentrss";

    private ListView mCommentList;
    private CommentListAdapter mAdapter;
    private ProgressBarFragment mProgressBar;

    private String mCommentRss;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        mCommentRss = getIntent().getStringExtra(COMMENTRSS);
        setupContent();
    }

    private void setupContent() {
        mCommentList = (ListView) findViewById(R.id.lvComments);
        mAdapter = new CommentListAdapter(this, R.id.lvComments);
        mCommentList.setAdapter(mAdapter);

        // start loading comments
        CommentParseTask task = new CommentParseTask(this, UniqueProvider.generateTaskId());
        task.addListener(this);
        task.execute(mCommentRss);
    }

    @Override
    public void onCommentPreParse(int taskId) {
        mProgressBar = new ProgressBarFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.activity_comments_progress, mProgressBar).commitAllowingStateLoss();
    }

    @Override
    public void onCommentPostParse(int taskId, List<RSSComment> result) {
        if (mProgressBar != null) {
            getSupportFragmentManager().beginTransaction().remove(mProgressBar).commitAllowingStateLoss();
        }

        if (result != null && result.size() != 0) {
            mAdapter.setList(result);
        }
    }

    @Override
    public void onCommentError(int taskId, String message) {
        if (mProgressBar != null) {
            getSupportFragmentManager().beginTransaction().remove(mProgressBar).commitAllowingStateLoss();
        }

        RetryFragment retryFragment = new RetryFragment();
        retryFragment.setContent(R.mipmap.ic_warning, message, false);
        getSupportFragmentManager().beginTransaction().add(R.id.activity_comments_progress, retryFragment).commitAllowingStateLoss();
    }

    class CommentListAdapter extends ArrayAdapter<RSSComment> {
        private Context context;
        private LayoutInflater inflater;

        private List<RSSComment> mComments = new ArrayList<>();

        /**
         *
         * @param context
         * @param resource
         */
        public CommentListAdapter(@NonNull Context context, int resource) {
            super(context, resource);

            this.context = context;
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }


        @Override
        public int getCount() {
            return this.mComments.size();
        }

        @Override
        public RSSComment getItem(int position) {
            return this.mComments.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;

            if (view == null) {
                view = inflater.inflate(R.layout.comment_item, viewGroup, false);

                viewHolder = new ViewHolder();
                viewHolder.tvCreator = (TextView) view.findViewById(R.id.tvcCreator);
                viewHolder.tvPubDate = (TextView) view.findViewById(R.id.tvcPubDate);
                viewHolder.tvContent = (TextView) view.findViewById(R.id.tvcContent);

                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            RSSComment comment = this.mComments.get(position);
            viewHolder.tvCreator.setText(comment.creator);
            viewHolder.tvPubDate.setText(Helper.milliToString(comment.pubDate, RSSFeed.TIME_FORMAT_PUBDATE_COMPACT));
            viewHolder.tvContent.setText(comment.content);

            return view;
        }

        public void setList(List<RSSComment> list) {
            this.mComments = list;
            notifyDataSetChanged();
        }

    }
    static class ViewHolder {
        TextView tvCreator;
        TextView tvPubDate;
        TextView tvContent;
    }
}
