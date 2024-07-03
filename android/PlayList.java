package com.toelim.tsb;

//we have to split the static resource into several classes since there is 
//65535 size limit for static initializer in a single class
import static com.toelim.tsb.KjStatic.*;
//import static com.toelim.tsb.NivStatic.*;
import static com.toelim.tsb.WebStatic.*;
import static com.toelim.tsb.KidzStatic.*;
import static com.toelim.tsb.SpanishnivStatic.*;

public class PlayList {
	//playlist info
	String mName;
	String mTitle;
	String mLocation;
	String	mInfo;
	int	mTrackListCount;
	int	mTrackCount;
	
	//tracklist info
	String[] mTrackListsTitle;
	String[] mTrackListsLocation;
	String[] mTrackListsInfo;
	int[] mTrackListsStart;
	int[] mTrackListsEnd;
	
	//track info
	String[] mTracksTitle;
	String[] mTracksLocation;
	String[] mTracksInfo;
	
	//used if from raw resources or from web
	TrackList[] mPlayList;
	Track[] mTrackList;
	int	from_res;
	String from_web;
	
	PlayList(String name,
			String title,
			String loc,
			String info,
			int tracklistcount,
			int trackcount,
			String[] trackliststitle,
			String[] tracklistsloc,
			String[] tracklistsinfo,
			int[] tracklistsstart,
			int[] tracklistsend,
			String[] trackstitle,
			String[] tracksloc,
			String[] tracksinfo
			) {
		mName = name;
		mTitle = title;
		mLocation = loc;
		mInfo = info;
		mTrackListCount = tracklistcount;
		mTrackCount = trackcount;
		
		mTrackListsTitle = trackliststitle;
		mTrackListsLocation = tracklistsloc;
		mTrackListsInfo = tracklistsinfo;
		mTrackListsStart = tracklistsstart;
		mTrackListsEnd = tracklistsend;
		mTracksTitle = trackstitle;
		mTracksLocation = tracksloc;
		mTracksInfo = tracksinfo;
	}
	
	PlayList(String title, int from_res) {
		this(title, title, from_res);
	}
	
	PlayList(String title, String info, int from_res) {
		mTitle = title;
		mInfo = info;
		this.from_res = from_res;
	}

	PlayList(String title, String from_web) {
		this(title, title, from_web);
	}
	
	PlayList(String title, String info, String from_web) {
		mTitle = title;
		mInfo = info;
		this.from_web = from_web;
	}
	
	public boolean mIsPaused;
	public long mSavedPosition;
	public int mSavedTrackPosition;
	public int mSavedTrackListPosition;
	
	public static final String SAVED_POSITIONS = "saved.positions";
	public static final String SAVED_LANG = "saved.lang";
	public static final String SAVED_VERSION = "saved.version";
	public static PlayList sCurPlayList;
	//public static TrackList sCurTrackList;
	public static int sCurTrackListPosition;
	//public static Track sCurTrack;
	public static int sCurTrackPosition;
	public static String sCurTrackLoc;
	public static long sCurPosition;

	//not use ENUM for efficiency on mobile devices
	public static final int ENGLISH = 0;
	public static final int SPANISH = 1;
	
	public static final String[] sLangList = {
		"English",
		"Spanish"
	};
	
	public static final int PLAYLIST_KJ = 0;
	public static final int PLAYLIST_NIV = 1;
	public static final int PLAYLIST_WEB = 2;
	public static final int PLAYLIST_KIDZ = 3;

	public static PlayList[][] sPlayLists = {
		{	//english
			/*
			new PlayList("King James Bible", R.raw.kj),
			new PlayList("New International Version", R.raw.niv),
			new PlayList("World English Bible", R.raw.web),
			new PlayList("Kidz Bible", R.raw.kidz)
			*/
			new PlayList("kj", "King James Bible", "http://www.theandroidbible.com/ad", "http://www.theandroidbible.com", 66, 1191, kj_tracklists_title, kj_tracklists_loc, kj_tracklists_info, kj_tracklists_start, kj_tracklists_end, kj_tracks_title, kj_tracks_loc, kj_tracks_info),
			null,
			new PlayList("web", "World English Bible", "http://www.theandroidbible.com/web", "http://www.theandroidbible.com/", 66, 1191, web_tracklists_title, web_tracklists_loc, web_tracklists_info, web_tracklists_start, web_tracklists_end, web_tracks_title, web_tracks_loc, web_tracks_info),
			new PlayList("kidz", "Kidz Bible", "http://www.thestreamingbible.com", "http://www.thestreamingbible.com", 1, 381, kidz_tracklists_title, kidz_tracklists_loc, kidz_tracklists_info, kidz_tracklists_start, kidz_tracklists_end, kidz_tracks_title, kidz_tracks_loc, kidz_tracks_info)		
		},
		{	//spanish
			null,
			//new PlayList("New International Version", R.raw.spanishniv),
			new PlayList("spanishniv", "SpanishNIV Bible", "http://www.thestreamingbible.com/spanNIV", "http://www.thestreamingbible.com", 64, 1167, spanishniv_tracklists_title, spanishniv_tracklists_loc, spanishniv_tracklists_info, spanishniv_tracklists_start, spanishniv_tracklists_end, spanishniv_tracks_title, spanishniv_tracks_loc, spanishniv_tracks_info),
			null,
			null
		}
	};

	public static int sPlayListCount;
	static {
		for(PlayList[] row: sPlayLists)
			for(PlayList pl: row)
				if(pl != null) sPlayListCount++;
	}
	
	public static int sCurLANG = -1;
	public static int sCurPlayListIndex = -1;
	
	public static boolean loaded = true;	//true for builtin, false for from res or from web


}