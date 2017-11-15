package com.cee.cvox.model.metadata;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by conqtc on 11/5/17.
 */

public class Book implements Parcelable {
    public String title;
    public String description;
    public String link;
    public String guid;
    public long pubDate;

    // json data
    public String creator;
    public String runtime;
    public int totalTracks;
    public List<Chapter> chapters = new ArrayList<>();

    public Book() {}

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * We only care to pass a raw book with limited info to BookDetailProvider and corresponding activity
     * BookDetailProvider will take care of filling in the detail info
     * @param dest
     * @param flags
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.title);
        dest.writeString(this.guid);
        dest.writeLong(this.pubDate);
    }

    public static final Parcelable.Creator<Book> CREATOR = new Parcelable.Creator<Book>() {

        @Override
        public Book createFromParcel(Parcel source) {
            Book book = new Book();

            book.title = source.readString();
            book.guid = source.readString();
            book.pubDate = source.readLong();

            return book;
        }

        @Override
        public Book[] newArray(int size) {
            return new Book[size];
        }
    };

}
