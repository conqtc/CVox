package com.cee.cvox.model.metadata;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by conqtc on 11/5/17.
 */

public class AudioInfo implements Parcelable {
    public static final int AI_IDLE = 0;
    public static final int AI_PLAYING = 1;
    public static final int AI_PAUSE = 2;

    public String url;
    public String title;
    public String artist;
    public String album;
    public String duration;
    public int playbackState;
    public String guid;     // for album art

    public AudioInfo() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.url);
        dest.writeString(this.title);
        dest.writeString(this.artist);
        dest.writeString(this.album);
        dest.writeInt(this.playbackState);
        dest.writeString(guid);
    }

    public static final Parcelable.Creator<AudioInfo> CREATOR = new Parcelable.Creator<AudioInfo>() {

        @Override
        public AudioInfo createFromParcel(Parcel source) {
            AudioInfo audioInfo = new AudioInfo();

            audioInfo.url = source.readString();
            audioInfo.title = source.readString();
            audioInfo.artist = source.readString();
            audioInfo.album = source.readString();
            audioInfo.playbackState = source.readInt();
            audioInfo.guid = source.readString();

            return audioInfo;
        }

        @Override
        public AudioInfo[] newArray(int size) {
            return new AudioInfo[size];
        }
    };
}
