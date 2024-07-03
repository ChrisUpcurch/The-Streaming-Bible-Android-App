package com.toelim.tsb;

import java.io.Serializable;

public class SavedPosition implements Serializable {
	static final long serialVersionUID = 1L;
	
	boolean mIsPaused;
	long mSavedPosition;
	int mSavedTrackPosition;
	int mSavedTrackListPosition;
	int mLang;
	int mBibleVersion;
	
	SavedPosition(int tracklist, int track, long time, int lang, int version, boolean paused) {
		mSavedTrackListPosition = tracklist;
		mSavedTrackPosition = track;
		mSavedPosition = time;
		mLang = lang;
		mBibleVersion = version;
		mIsPaused = paused;
	}
}
