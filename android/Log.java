/* 
 * Copyright (C) 2010 WaveConn
 * Author BQ
 */

package com.toelim.tsb;

/**
 * package-level logging flag
 */

public class Log {
    public final static String LOGTAG = "TSB";

    public static final boolean LOGD = false;

    public static void v(String logMe) {
        if(LOGD) android.util.Log.v(LOGTAG, logMe);
    }
    
    public static void d(String logMe) {
        if(LOGD) android.util.Log.d(LOGTAG, logMe);
    }

    public static void i(String logMe) {
        android.util.Log.i(LOGTAG, logMe);
    }
    
    public static void w(String logMe) {
        android.util.Log.w(LOGTAG, logMe);
    }
    
    public static void e(String logMe) {
        android.util.Log.e(LOGTAG, logMe);
    }

    public static void e(String logMe, Exception ex) {
        android.util.Log.e(LOGTAG, logMe, ex);
    }
}
