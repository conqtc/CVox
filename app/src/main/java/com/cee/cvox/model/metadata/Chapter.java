package com.cee.cvox.model.metadata;

/**
 * Created by conqtc on 11/5/17.
 */

public class Chapter extends AudioInfo {
    public String name;
    // url -> https://archive.org/download/[book.guid]/[name]
    // source = original

    // title -> e.g "Chapter 01-02"
    public int trackNumber;
    public long trackSize;
    public double trackLength;
    // artist -> artist or [book.creator]
    // album -> [book.title]
}
