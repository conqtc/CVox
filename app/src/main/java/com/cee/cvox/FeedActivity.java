package com.cee.cvox;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;

import com.cee.cvox.common.Helper;
import com.cee.cvox.data.BlogProvider;
import com.cee.cvox.model.RSS.RSSFeed;
import com.cee.cvox.model.RSS.RSSItem;

public class FeedActivity extends AppCompatActivity {
    private static final String TAG = "___FeedActivity";

    public static final String FEED_POSITION = "feed_position";

    private int mPosition;
    private BlogProvider mProvider;
    private RSSItem mFeedItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPosition = getIntent().getIntExtra(FEED_POSITION, -1);
        setupContent();
    }

    private void setupContent() {
        mProvider = new BlogProvider(this);
        mFeedItem = mProvider.getFeed().items.get(mPosition);

        TextView tv = (TextView) findViewById(R.id.tvfFeedTitle);
        tv.setText(mFeedItem.title);
        tv = (TextView) findViewById(R.id.tvfFeedSub);
        tv.setText(Helper.milliToString(mFeedItem.pubDate, RSSFeed.TIME_FORMAT_PUBDATE_COMPACT));

        WebView webView = (WebView) findViewById(R.id.wvFeedContent);
        final String mimeType = "text/html";
        final String encoding = "UTF-8";
        webView.loadData(mFeedItem.contentEncoded, mimeType, encoding);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_share, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_share) {
            Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, mFeedItem.title);
            intent.putExtra(Intent.EXTRA_TEXT, mFeedItem.contentEncoded);

            startActivity(Intent.createChooser(intent, "Share this feed with"));

            return true;
        } else if (id == R.id.action_comment) {

            if (mFeedItem.comments == 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Message from CVox");
                builder.setMessage("This feed has no comment!");
                builder.setPositiveButton("OK", null);
                builder.show();
            } else {
                Intent intent = new Intent(this, CommentsActivity.class);
                intent.putExtra(CommentsActivity.COMMENTRSS, mFeedItem.commentRss);
                startActivity(intent);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}

