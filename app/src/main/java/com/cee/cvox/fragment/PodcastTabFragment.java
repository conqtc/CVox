package com.cee.cvox.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.cee.cvox.R;
import com.cee.cvox.common.Helper;
import com.cee.cvox.data.PodcastProvider;
import com.cee.cvox.model.RSS.RSSFeed;
import com.cee.cvox.model.RSS.RSSPodcast;
import com.cee.cvox.model.metadata.AudioInfo;
import com.cee.cvox.service.MediaPlayerService;
import com.cee.cvox.task.DownloadTask;
import com.cee.cvox.task.PodcastParseTask;

import java.util.List;

/**
 * Created by conqtc on 10/10/17.
 */

public class PodcastTabFragment extends BaseTabFragment implements
        DownloadTask.OnDownloadListener,
        PodcastParseTask.OnPodcastParseListener,
        AdapterView.OnItemClickListener,
        MediaPlayerService.OnMediaPlayerServiceListener {
    private static final String TAG = "___PodcastTab";

    private ListView mPodcastListView;
    private PodcastListAdapter mAdapter;
    private PodcastProvider mProvider;
    private ProgressBarFragment mProgressBar;
    private RetryFragment mRetryFragment;

    // edia player service
    private MediaPlayerService mPlayer;
    private boolean bServiceBound = false;

    //
    private RSSPodcast mActivePodcast;
    private RSSPodcast mPendingActive;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mView == null) {
            mView = inflater.inflate(R.layout.fragment_tab_podcast, container, false);

            mProvider = new PodcastProvider(getContext());
            mProvider.setDownloadListener(this);
            mProvider.setParseListener(this);

            mPodcastListView = (ListView) mView.findViewById(R.id.lvPodcasts);
            mAdapter = new PodcastListAdapter(getContext(), R.id.lvPodcasts, mProvider);
            mPodcastListView.setAdapter(mAdapter);

            mPodcastListView.setOnItemClickListener(this);
        }

        return mView;
    }

    @Override
    public void onPreDownload(int taskId) {
        mProgressBar = new ProgressBarFragment();
        getChildFragmentManager().beginTransaction().add(R.id.tab_podcast_progress, mProgressBar).commitAllowingStateLoss();
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
        getChildFragmentManager().beginTransaction().add(R.id.tab_podcast_progress, mRetryFragment).commitAllowingStateLoss();
        //mRetryFragment.setRetryListner(this);
    }

    @Override
    public void onPodcastPreParse(int taskId) {

    }

    @Override
    public void onPodcastPostParse(int taskId, List<RSSPodcast> result) {
        if (mProgressBar != null) {
            getChildFragmentManager().beginTransaction().remove(mProgressBar).commitAllowingStateLoss();
        }

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }

    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalServiceBinder binder = (MediaPlayerService.LocalServiceBinder) service;
            mPlayer = binder.getService();
            bServiceBound = true;
            mPlayer.addListener(PodcastTabFragment.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bServiceBound = false;
        }
    };


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        RSSPodcast podcast = mProvider.getPodcasts().get(position);

        switch (podcast.playbackState) {
            case AudioInfo.AI_IDLE:
                Intent intent = new Intent(getActivity(), MediaPlayerService.class);
                AudioInfo audioInfo = (AudioInfo) podcast;
                intent.putExtra("audio", audioInfo);
                getActivity().startService(intent);

                if (!bServiceBound) {
                    getActivity().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
                }

                if (mPendingActive != null) {
                    mPendingActive.playbackState = AudioInfo.AI_IDLE;
                }
                mPendingActive = podcast;
                ((AudioInfo) podcast).playbackState = AudioInfo.AI_PLAYING;
                break;

            case AudioInfo.AI_PLAYING:
                if (mPlayer != null && mActivePodcast != null) {
                    mPlayer.pauseMedia();
                    ((AudioInfo) podcast).playbackState = AudioInfo.AI_PAUSE;
                }
                break;

            case AudioInfo.AI_PAUSE:
                if (mPlayer != null) {
                    mPlayer.resumeMedia();
                    ((AudioInfo) podcast).playbackState = AudioInfo.AI_PLAYING;
                }
                break;

        }

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onMediaStartNew() {
        if (mActivePodcast != null) {
            mActivePodcast.playbackState = AudioInfo.AI_IDLE;
            mActivePodcast = null;
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onMediaPlay() {

    }

    @Override
    public void onMediaSkipToPrevious() {
        if (mActivePodcast != null) {
            int position = mAdapter.mProvider.getPodcasts().indexOf(mActivePodcast);

            if (position > 0) {
                onItemClick(null, null, position - 1, 0);
            }
        }
    }

    @Override
    public void onMediaSkipToNext() {
        if (mActivePodcast != null) {
            int position = mAdapter.mProvider.getPodcasts().indexOf(mActivePodcast);

            if (position >= 0 && position < mAdapter.getCount() - 1) {
                onItemClick(null, null, position + 1, 0);
            }
        }
    }

    @Override
    public void onMediaPrepared() {
        if (mPendingActive != null) {
            mActivePodcast = mPendingActive;
            mPendingActive = null;
        }
    }

    @Override
    public void onMediaPause() {
        if (mActivePodcast != null) {
            mActivePodcast.playbackState = AudioInfo.AI_PAUSE;
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onMediaResume() {
        if (mActivePodcast != null) {
            mActivePodcast.playbackState = AudioInfo.AI_PLAYING;
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onMediaComplete() {
        if (mActivePodcast != null) {
            mActivePodcast.playbackState = AudioInfo.AI_IDLE;
            mActivePodcast = null;
            mPendingActive = null;
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onMediaStop() {
        if (mActivePodcast != null) {
            mActivePodcast.playbackState = AudioInfo.AI_IDLE;
            mActivePodcast = null;
            mPendingActive = null;
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onMediaBuffering(int percent) {

    }

    @Override
    public void onMediaError(int what, int extra) {
        if (mActivePodcast != null) {
            mActivePodcast.playbackState = AudioInfo.AI_IDLE;
            mActivePodcast = null;
            mPendingActive = null;
            mAdapter.notifyDataSetChanged();
        }
    }

    class PodcastListAdapter extends ArrayAdapter<RSSPodcast> {
        private Context context;
        private LayoutInflater inflater;

        private PodcastProvider mProvider;

        /**
         *
         * @param context
         * @param resource
         */
        public PodcastListAdapter(@NonNull Context context, int resource, PodcastProvider provider) {
            super(context, resource);

            this.context = context;
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.mProvider = provider;
        }

        @Override
        public int getCount() {
            return mProvider.getPodcasts().size();
        }

        @Override
        public RSSPodcast getItem(int position) {
            return mProvider.getPodcasts().get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;

            if (view == null) {
                view = inflater.inflate(R.layout.podcast_item, viewGroup, false);

                viewHolder = new ViewHolder();
                viewHolder.tvTitle = (TextView) view.findViewById(R.id.tvpTitle);
                viewHolder.tvPubDate = (TextView) view.findViewById(R.id.tvpPubDate);
                viewHolder.tvDuration = (TextView) view.findViewById(R.id.tvpDuration);
                viewHolder.tvSubtitle = (TextView) view.findViewById(R.id.tvpSubtitle);
                viewHolder.ivPlayPause = (ImageView) view.findViewById(R.id.ivpPlayPause);

                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            RSSPodcast podcast = this.mProvider.getPodcasts().get(position);
            viewHolder.tvTitle.setText(podcast.title);
            viewHolder.tvPubDate.setText(Helper.milliToString(podcast.pubDate, RSSFeed.TIME_FORMAT_PUBDATE_COMPACT));
            viewHolder.tvDuration.setText(podcast.duration);
            viewHolder.tvSubtitle.setText(podcast.album + " by " + podcast.artist);

            switch (podcast.playbackState) {
                case AudioInfo.AI_IDLE:
                    viewHolder.ivPlayPause.setImageResource(R.mipmap.ic_podcast);
                    break;

                case AudioInfo.AI_PLAYING:
                    viewHolder.ivPlayPause.setImageResource(R.mipmap.ic_pause);
                    break;

                case AudioInfo.AI_PAUSE:
                    viewHolder.ivPlayPause.setImageResource(R.mipmap.ic_play);
                    break;

            }

            return view;
        }

    }

    static class ViewHolder {
        TextView tvTitle;
        TextView tvPubDate;
        TextView tvSubtitle;
        TextView tvDuration;
        ImageView ivPlayPause;
    }

}
