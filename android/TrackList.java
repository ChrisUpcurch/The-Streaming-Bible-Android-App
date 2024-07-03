package com.toelim.tsb;

import java.util.ArrayList;

public class TrackList {
	String	mTitle;
	String	mLocation;
	String	mInfo;
	int	mStart;
	int mEnd;
	
	TrackList() {
		mTitle = null;
		mInfo = null;
		mLocation = null;
		mStart = 0;
		mEnd = 0;
	}
	
	TrackList(String title, String loc, int start, int end) {
		mTitle = title;
		mLocation = loc;
		mInfo = null;
		mStart = start;
		mEnd = end;
	}

	TrackList(String title, String loc, String info, int start, int end) {
		mTitle = title;
		mLocation = loc;
		mInfo = info;
		mStart = start;
		mEnd = end;
	}

	public String toString() {
		return mTitle;
	}
}
