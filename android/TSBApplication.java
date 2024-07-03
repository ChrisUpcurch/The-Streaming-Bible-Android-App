package com.toelim.tsb;

import static com.toelim.tsb.PlayList.*;
import static com.toelim.tsb.MusicUtils.*;

import java.io.Serializable;
import java.util.ArrayList;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.RemoteException;

public class TSBApplication extends Application {
	ArrayList<Activity>	mActiveActivities;	//XXX: memory leak?
											//TODO: or use ActivityManager.RunningTaskInfo?
	
	static SharedPreferences preferences;
	static final String PREFERENCES_DB = "mini_db";
	
    @Override
    public void onCreate() {
    	super.onCreate();
    	
    	preferences = getSharedPreferences(
				PREFERENCES_DB,
				Activity.MODE_PRIVATE);
    	
    	restorePositions(preferences);
    	
    	mActiveActivities = new ArrayList<Activity>();
    }

    @Override
    public void onTerminate() {
    	if(Log.LOGD) Log.v("SRJobApplication onTerminate gets called");
    	
    	savePositions(preferences);
		
    	/*
    	ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    	List<RunningTaskInfo> rtis = am.getRunningTasks(1);
    	RunningTaskInfo rti = rtis.get(0);
    	* it seems lack of list of active activities in RunningTaskInfo API in 1.5
    	*/

    	if(Player.sCurrentPlayer != null) Player.sCurrentPlayer.finish();
    	if(TSB.sInstance != null) TSB.sInstance.finish();
    	if(MediaPlaybackService.sInstance != null) MediaPlaybackService.sInstance.stopSelf();
    	
    	for (Activity a: mActiveActivities)
    		if (a != null) a.finish();
    	mActiveActivities.clear();
    	
    	super.onTerminate();
    }
    
    void restorePositions(SharedPreferences preferences)
    {
		String s = preferences.getString(PlayList.SAVED_POSITIONS, null);
		if (s != null) {
			ArrayList<SavedPosition> savedPositions
				= (ArrayList<SavedPosition>)Utils.unserialize(Base64.decode(s));
			if (savedPositions != null)
				for(SavedPosition sp : savedPositions) {
					int i = sp.mLang;
					int j = sp.mBibleVersion;
					sPlayLists[i][j].mSavedTrackListPosition = sp.mSavedTrackListPosition;
					sPlayLists[i][j].mSavedTrackPosition = sp.mSavedTrackPosition;
					sPlayLists[i][j].mSavedPosition = sp.mSavedPosition < 0 ? 0 : sp.mSavedPosition;
					sPlayLists[i][j].mIsPaused = sp.mIsPaused;
					if(Log.LOGD) Log.d("mLang=" + sp.mLang);
					if(Log.LOGD) Log.d("mBibleVersion=" + sp.mBibleVersion);
					if(Log.LOGD) Log.d("mSavedTrackListPosition=" + sp.mSavedTrackListPosition);
					if(Log.LOGD) Log.d("mSavedTrackPosition=" + sp.mSavedTrackPosition);
					if(Log.LOGD) Log.d("mSavedPosition=" + sp.mSavedPosition);
					if(Log.LOGD) Log.d("mIsPaused=" + sp.mIsPaused);
				}
		}
    }
	
    void savePositions(SharedPreferences preferences)
    {
		ArrayList<SavedPosition> savedPositions = new ArrayList<SavedPosition>();
		for(int i = 0; i < sPlayLists.length; i++)
			for(int j = 0; j < sPlayLists[i].length; j++) {
				try {
					if(sService != null && sService.isPlaying() && sCurPlayList != null && sPlayLists[i][j] == sCurPlayList) {
						sPlayLists[i][j].mSavedTrackListPosition = sCurTrackListPosition;
						sPlayLists[i][j].mSavedTrackPosition = sCurTrackPosition;
						long pos = sService.position();
						sPlayLists[i][j].mSavedPosition = pos < 0 ? 0 : pos;
						sPlayLists[i][j].mIsPaused = !sService.isPlaying();
					}
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(sPlayLists[i][j] != null) {
					savedPositions.add(new SavedPosition(
							sPlayLists[i][j].mSavedTrackListPosition,
							sPlayLists[i][j].mSavedTrackPosition,
							sPlayLists[i][j].mSavedPosition,
							i, j, sPlayLists[i][j].mIsPaused));
				}
			}
		
		if(savedPositions != null && savedPositions.size() != 0) {
			Editor e = preferences.edit();
			e.putString(PlayList.SAVED_POSITIONS,
						Base64.encodeToString(Utils.serialize(savedPositions), false));
			if(sCurLANG != -1 && sCurPlayListIndex != -1) {
				e.putInt(PlayList.SAVED_LANG, sCurLANG);
				e.putInt(PlayList.SAVED_VERSION, sCurPlayListIndex);
			}
			e.commit();
		}
	}
    
    public void addActiveActivity(Activity a) {
    	mActiveActivities.add(a);
    }
    
    public void removeActiveActivity(Activity a) {
    	mActiveActivities.remove(a);
    }
}
