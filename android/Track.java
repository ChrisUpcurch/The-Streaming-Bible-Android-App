package com.toelim.tsb;

public class Track {
	String mTitle;
	String mLocation;
	String mInfo;
	
	Track() {
		this(null, null);
	}
	
	Track(String title, String loc) {
		mTitle = title;
		mLocation = loc;
	}
	
	Track(String title, String loc, String info) {
		mTitle = title;
		mLocation = loc;
		mInfo = info;
	}
	
	public String toString() {
		return mTitle;
	}
	
}
