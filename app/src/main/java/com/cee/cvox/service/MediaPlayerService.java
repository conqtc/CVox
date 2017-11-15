package com.cee.cvox.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;
import android.support.v7.graphics.Palette;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.cee.cvox.R;
import com.cee.cvox.model.metadata.AudioInfo;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by conqtc on 11/4/17.
 * ref: https://www.sitepoint.com/a-step-by-step-guide-to-building-an-android-audio-player-app/
 */

public class MediaPlayerService extends Service implements
        AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener {

    private static final String TAG = "___MediaPlayerService";

    private static final int NOTIFICATION_ID = 1808;
    private static final String CHANNEL_ID = "com.cee.cvox.channel.main";
    public static final String NOTIFICATION_CHANNEL_ID = "com.cee.cvox.notification.channel";

    public static final String ACTION_PLAY = "com.cee.cvox.media.player.action.play";
    public static final String ACTION_PAUSE = "com.cee.cvox.media.player.action.pause";
    //public static final String ACTION_REW = "com.cee.cvox.media.player.action.rew";
    //public static final String ACTION_FF = "com.cee.cvox.media.player.action.ff";
    public static final String ACTION_PREVIOUS = "com.cee.cvox.media.player.action.previous";
    public static final String ACTION_NEXT = "com.cee.cvox.media.player.action.next";
    public static final String ACTION_STOP = "com.cee.cvox.media.player.action.stop";


    public enum PlaybackStatus {
        PLAYING,
        PAUSED
    }

    // local binder
    private final IBinder iBinder = new LocalServiceBinder();

    // media player
    private MediaPlayer mMediaPlayer;

    // media session
    private MediaSessionManager mMediaSessionManager;
    private MediaSessionCompat mMediaSession;
    private MediaControllerCompat.TransportControls mTransportControls;

    // audio manager
    private AudioManager mAudioManager;

    //
    private AudioInfo mAudioInfo = new AudioInfo();

    // playback resume position
    private int mResumePosition;

    // phone state handling
    private boolean mOngoingCall = false;
    private PhoneStateListener mPhoneStateListener;
    private TelephonyManager mTelephonyManager;

    // broadcast receivers
    private IntentFilter mBecomingNoisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private BecomingNoisyReceiver mBecomingNoisyReceiver = new BecomingNoisyReceiver();

    //
    private NotificationManager mNotificationManager;
    private NotificationChannel mNotificationChannel;
    //
    private List<OnMediaPlayerServiceListener> mListeners = new ArrayList<>();

    //
    private int mBufferPercent = 0;
    private String mLoadingStatus = "";

    public void addListener(OnMediaPlayerServiceListener listener) {
        mListeners.add(listener);
    }

    public List<OnMediaPlayerServiceListener> getMediaListeners() {
        return this.mListeners;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // call listener
        setupCallListener();

        // handle becoming noisy
        registerReceiver(mBecomingNoisyReceiver, mBecomingNoisyIntentFilter);

        //
        mNotificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mMediaPlayer != null) {
            stopMedia();
            mMediaPlayer.release();
        }

        removeAudioFocus();

        // unregister phone state listener
        if (mPhoneStateListener != null) {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        // remove notification
        removeNotification();

        // unregister broadcast receivers
        unregisterReceiver(mBecomingNoisyReceiver);
    }

    private void setupCallListener() {
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        // incoming, pause media
                        if (mMediaPlayer != null) {
                            pauseMedia();
                            mOngoingCall = true;
                        }
                        break;

                    case TelephonyManager.CALL_STATE_IDLE:
                        // idle, resume media
                        if (mMediaPlayer != null) {
                            if (mOngoingCall) {
                                mOngoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };

        // register call state listening
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (requestAudioFocus() == false) {
            stopSelf();
        }

        AudioInfo audioInfo = null;
        if (intent.hasExtra("audio")) {
            audioInfo = intent.getExtras().getParcelable("audio");
        }

        String action = intent.getAction();

        // not yet established
        if (mMediaSessionManager == null) {
            try {
                initMediaSession();
            } catch (Exception ex) {
                ex.printStackTrace();
                stopSelf();
            }
        }

        // playing or pausing different audio and this is about to start a new audio
        if (action == null && mMediaPlayer != null) {
            stopMedia();
        }

        // new audio
        if (audioInfo != null) {
            mAudioInfo = audioInfo;

            initMediaPlayer();
            updateMetaData();
            setupNotification();

            for (OnMediaPlayerServiceListener listener: mListeners) {
                listener.onMediaStartNew();
            }
        }

        handlePlaybackActions(action);

        return super.onStartCommand(intent, flags, startId);
    }

    private void initMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        }

        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this);
        mMediaPlayer.setOnInfoListener(this);

        // reset to clear previous data (if has)
        mMediaPlayer.reset();

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mMediaPlayer.setDataSource(mAudioInfo.url);
        } catch (Exception ex) {
            stopSelf();
        }

        mLoadingStatus = "Buffering... ";
        mMediaPlayer.prepareAsync();
    }


    private void initMediaSession() throws RemoteException {
        //
        if (mMediaSessionManager != null) { return; }
        mMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);

        //
        mMediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        mTransportControls = mMediaSession.getController().getTransportControls();
        mMediaSession.setActive(true);
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //
        updateMetaData();

        // media session callback
        mMediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();

                resumeMedia();
                for (OnMediaPlayerServiceListener listener: mListeners) {
                    listener.onMediaResume();
                }
            }

            @Override
            public void onPause() {
                super.onPause();

                pauseMedia();
                for (OnMediaPlayerServiceListener listener: mListeners) {
                    listener.onMediaPause();
                }
            }

            @Override
            public void onRewind() {
                super.onRewind();
            }

            @Override
            public void onFastForward() {
                super.onFastForward();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                for (OnMediaPlayerServiceListener listener: mListeners) {
                    listener.onMediaSkipToNext();
                }
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                for (OnMediaPlayerServiceListener listener: mListeners) {
                    listener.onMediaSkipToPrevious();
                }
            }

            @Override
            public void onStop() {
                super.onStop();

                for (OnMediaPlayerServiceListener listener: mListeners) {
                    listener.onMediaStop();
                }

                removeNotification();
                // stop service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    private boolean requestAudioFocus() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return true;
        }

        return false;
    }

    /**
     *
     * @param action
     */
    private void handlePlaybackActions(String action) {
        if (action == null || action.isEmpty()) {
            return;
        }

        if (action.equalsIgnoreCase(ACTION_PLAY)) {
            mTransportControls.play();
        } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
            mTransportControls.pause();
        } else if (action.equalsIgnoreCase(ACTION_PREVIOUS)) {
            mTransportControls.skipToPrevious();
        } else if (action.equalsIgnoreCase(ACTION_NEXT)) {
            mTransportControls.skipToNext();
        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            mTransportControls.stop();
        }
    }


    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            // gain focus
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mMediaPlayer == null) {
                    initMediaPlayer();
                }
                else if (!mMediaPlayer.isPlaying()) {
                    mMediaPlayer.start();
                }

                mMediaPlayer.setVolume(1.0f, 1.0f);

                break;

            // lost it, release media player
            case AudioManager.AUDIOFOCUS_LOSS:
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                mMediaPlayer.release();
                mMediaPlayer = null;

                break;

            // lost for short time, pause but non-release
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                }

                break;

            // lost for short time, keep playing but volume down
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.setVolume(0.1f, 0.1f);
                }

                break;
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        mBufferPercent = percent;
        for (OnMediaPlayerServiceListener listener: mListeners) {
            listener.onMediaBuffering(percent);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopMedia();
        for (OnMediaPlayerServiceListener listener: mListeners) {
            listener.onMediaComplete();
        }
        removeNotification();
        stopSelf();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                break;

            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                break;

            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                break;
        }

        for (OnMediaPlayerServiceListener listener: mListeners) {
            listener.onMediaError(what, extra);
        }

        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        for (OnMediaPlayerServiceListener listener: mListeners) {
            listener.onMediaPrepared();
        }

        mLoadingStatus = "";
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    // media actions
    public void playMedia() {
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
        setupNotification();
    }

    public void stopMedia() {
        if (mMediaPlayer == null) return;

        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
    }

    public void pauseMedia() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mResumePosition = mMediaPlayer.getCurrentPosition();
        }
        setupNotification();
    }

    public void resumeMedia() {
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.seekTo(mResumePosition);
            mMediaPlayer.start();
        }
        setupNotification();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }


    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.abandonAudioFocus(this);
    }


    /**
     *
     */
    public class LocalServiceBinder extends Binder {
        // allow calling public methods
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    // becoming noisy receiver
    private class BecomingNoisyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                pauseMedia();
            }
        }
    }

    private Bitmap loadBitmapFromFile(String guid) {
        Context context = getApplicationContext();
        Bitmap bitmap = null;
        try {
            FileInputStream fis = context.openFileInput(guid);
            bitmap = BitmapFactory.decodeStream(fis);
        } catch (Exception ex) {}

        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_podcast);
        }

        return bitmap;

    }

    private void updateMetaData() {
        Bitmap artwork = loadBitmapFromFile(mAudioInfo.guid);

        mMediaSession.setMetadata(new MediaMetadataCompat.Builder()
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mAudioInfo.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, mAudioInfo.album)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mAudioInfo.title)
            .build());
    }

    private PendingIntent getPlaybackActionIntent(int iAction) {
        Intent playbackIntent = new Intent(this, MediaPlayerService.class);

        switch (iAction) {
            // Play
            case 0:
                playbackIntent.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, iAction, playbackIntent, 0);

            // Pause
            case 1:
                playbackIntent.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, iAction, playbackIntent, 0);

            // Rew
            case 2:
                playbackIntent.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, iAction, playbackIntent, 0);

            // FF
            case 3:
                playbackIntent.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, iAction, playbackIntent, 0);

            default:
                break;
        }

        return null;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        if (mNotificationChannel == null) {
            mNotificationChannel = new NotificationChannel(CHANNEL_ID, "Media Playback", NotificationManager.IMPORTANCE_HIGH);
            mNotificationChannel.setDescription("Media Playback Controls");

            mNotificationChannel.setShowBadge(false);
            mNotificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            mNotificationManager.createNotificationChannel(mNotificationChannel);
        }
    }

    /**
     *  0: Play
     *  1: Pause
     *  2: Rew
     *  3: FF
     */
    private void setupNotification() {

        int notificationAction = android.R.drawable.ic_media_pause;
        PendingIntent playOrPauseIntent = null;

        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            notificationAction = android.R.drawable.ic_media_pause;
            // pause
            playOrPauseIntent = getPlaybackActionIntent(1);
        } else {
            notificationAction = android.R.drawable.ic_media_play;
            // play
            playOrPauseIntent = getPlaybackActionIntent(0);
        }

        Bitmap largeIcon = loadBitmapFromFile(mAudioInfo.guid);
        String text = mLoadingStatus.isEmpty() ? mAudioInfo.album : mLoadingStatus;
        Palette palette = Palette.from(largeIcon).generate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(false)
                .setSmallIcon(R.mipmap.ic_cvox)
                .setLargeIcon(largeIcon)
                .setSubText(mAudioInfo.artist)
                .setContentTitle(mAudioInfo.title)
                .setContentText(text);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder = notificationBuilder.setColor(palette.getDominantColor(Color.BLACK)).setColorized(true);
        }

        if (mLoadingStatus.isEmpty()) {
            notificationBuilder = notificationBuilder.setStyle(
                    new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mMediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2)
                        .setShowCancelButton(true))
                    .addAction(android.R.drawable.ic_media_previous, "prev", getPlaybackActionIntent(2))
                    .addAction(notificationAction, "play/pause", playOrPauseIntent)
                    .addAction(android.R.drawable.ic_media_next, "next", getPlaybackActionIntent(3));
        }

        mNotificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void removeNotification() {
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    public static interface OnMediaPlayerServiceListener {
        public void onMediaStartNew();
        public void onMediaPlay();
        public void onMediaSkipToPrevious();
        public void onMediaSkipToNext();
        public void onMediaPrepared();
        public void onMediaPause();
        public void onMediaResume();
        public void onMediaComplete();
        public void onMediaStop();
        public void onMediaBuffering(int percent);
        public void onMediaError(int what, int extra);
    }

}
