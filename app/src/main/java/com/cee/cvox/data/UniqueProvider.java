package com.cee.cvox.data;

/**
 * Created by conqtc on 10/23/17.
 */

public class UniqueProvider {
    private static int sTaskId = 0;

    public static int generateTaskId() {
        sTaskId++;
        return sTaskId;
    }
}
