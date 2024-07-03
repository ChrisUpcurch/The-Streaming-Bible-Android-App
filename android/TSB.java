package com.toelim.tsb;

import static com.toelim.tsb.MusicUtils.bindToService;
import static com.toelim.tsb.MusicUtils.sService;
import static com.toelim.tsb.MusicUtils.unbindFromService;
import static com.toelim.tsb.PlayList.sCurPlayList;
import static com.toelim.tsb.PlayList.sCurTrackListPosition;
import static com.toelim.tsb.PlayList.sCurTrackPosition;
import static com.toelim.tsb.PlayList.sLangList;
import static com.toelim.tsb.PlayList.sPlayListCount;
import static com.toelim.tsb.PlayList.sPlayLists;
import static com.toelim.tsb.Utils.*;


import java.io.IOException;
import java.io.InputStreamReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.toelim.tsb.R;
import com.toelim.tsb.R.layout;
import com.toelim.tsb.R.string;
import com.toelim.tsb.MusicUtils.ServiceToken;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

public class TSB extends Activity{
	
	
	private int mMode;
	private int mLang;
	private int mPlayListIndex;
	
	static TSB sInstance;
	
    //Called when the activity is first created. 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.splash);

        
        loadstart(0,0);
    }
    
    ServiceToken mToken;
    
    @Override
    public void onStart() {
        super.onStart();
        
        loadstart(0,0);

        if (mToken == null) mToken = bindToService(this, null);
        if (mToken == null) {
        	showToast(this, "Error in streaming music service.", true);
        }
        
        sInstance = this;
    }

    @Override
    public void onStop() {
    	////if (mToken != null) unbindFromService(mToken);
        super.onStop();
    }
    
    @Override
    public void onDestroy() {
    	if (isFinishing() && mToken != null) unbindFromService(mToken);
    	
    	sInstance = null;
    	
        super.onDestroy();
    }
    
    @Override
    public void onLowMemory() {
    	sInstance = null;
    	
        super.onLowMemory();
    }
    
    //for API 1.5 compatibility

    public void loadstart(int lang, int playlist) {
		//save the old positions first if the service is running
		try {
			if(sService != null && sService.isPlaying() && sCurPlayList != null) {
			    sCurPlayList.mSavedTrackListPosition = sCurTrackListPosition;
			    sCurPlayList.mSavedTrackPosition = sCurTrackPosition;
			    long pos = sService.position();
			    sCurPlayList.mSavedPosition = pos < 0 ? 0 : pos;
			    sCurPlayList.mIsPaused = !sService.isPlaying();
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    sCurPlayList = sPlayLists[lang][playlist];        	
	    //Retrieve the playlist from network or raw resource
	    if(sCurPlayList != null) {
		    if(PlayList.loaded) {	//use builtin resources
		    	startplayer(lang, playlist);
		    } else {
			    if (sCurPlayList.mTrackListsTitle == null) {
			    	LoadPlayListTask loadPlayList = new LoadPlayListTask();
			    	loadPlayList.mActivity = this;
			    	loadPlayList.mRes = getResources();
			    	loadPlayList.lang = lang;
			    	loadPlayList.playlist = playlist;
			    	loadPlayList.execute();
			    } else {
			    	startplayer(lang, playlist);
			    }
		    }
	    } else {
	    	showToast(this, "This version is not supported currenly.");
	    }
    }
    
    private void startplayer(int lang, int playlist) {
    	showToast(TSB.this, "Loading Player...", true);
    	Intent i = new Intent(TSB.this, Player.class);
    	Bundle b = new Bundle();
    	b.putInt("playlist", playlist);
    	b.putInt("language", lang);
    	b.putString("from", "button");
    	i.putExtras(b);
    	startActivity(i);
    }
  
    static class ProgressData {
    	String msg;
    	int	percent;
    	
    	ProgressData(String msg, int percent) {
    		this.msg = msg;
    		this.percent = percent;
    	}
    }
    
	class LoadPlayListTask extends AsyncTask<Void, ProgressData, Void> {
		public Activity mActivity;
		public Resources mRes;
		int lang;
		int playlist;

		@Override
		protected void onPreExecute() {
			mActivity.showDialog(DIALOG_PROGRESS_ID);
			mProgressDialog.setTitle("Loading " + sCurPlayList.mTitle + " ...");
			mProgressDialog.setMax(100);
			mProgressDialog.setProgress(0);
			mProgressDialog.setSecondaryProgress(0);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setMessage("Loading...");
		}

		@Override
		protected Void doInBackground(Void... p) {
			Void result = null;
			try {
				getPlayList(sCurPlayList, mRes);
			} catch (NotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return result;
		}

	    protected void onProgressUpdate(ProgressData... progress) {
	    	////this is to hack the issue with the old managed dialog which is fixed with API 8
	    	mProgressData = progress[0];
	    	mActivity.showDialog(DIALOG_PROGRESS_ID);
	    }

		@Override
		protected void onPostExecute(Void result) {
			mActivity.dismissDialog(DIALOG_PROGRESS_ID);
			
			//startplayer(lang, playlist);
		}
		
	    public void getPlayList(PlayList pl, Resources res) throws NotFoundException, XmlPullParserException, IOException {
	        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
	        factory.setNamespaceAware(true);
	        XmlPullParser xpp = factory.newPullParser();
	        xpp.setInput( new InputStreamReader ( res.openRawResource(pl.from_res) ) );

	        int eventType = xpp.getEventType();
	        TrackList tracklist = null;
	        Track track = null;
	        int i = 0;	//track list count
	        int j = 0;	//track count for each track list
	        int start = 0;
	        int trackcount = 0;
	        while (eventType != XmlPullParser.END_DOCUMENT) {
	         if(eventType == XmlPullParser.START_TAG && "playlist".equals(xpp.getName().toLowerCase())) {
	        	 ;
	         } else if(eventType == XmlPullParser.START_TAG && "title".equals(xpp.getName().toLowerCase())) {
	             pl.mTitle = xpp.nextText();
	         } else if(eventType == XmlPullParser.START_TAG && "tracklistcount".equals(xpp.getName().toLowerCase())) {
	             pl.mTrackListCount = Integer.valueOf(xpp.nextText());
	             pl.mPlayList = new TrackList[pl.mTrackListCount];
	         } else if(eventType == XmlPullParser.START_TAG && "trackcount".equals(xpp.getName().toLowerCase())) {
	             pl.mTrackCount = Integer.valueOf(xpp.nextText());
	             pl.mTrackList = new Track[pl.mTrackCount];
	             final int max;
	             if(pl.mTrackListCount > 1)
	             	max = pl.mTrackListCount;
	             else max = pl.mTrackCount;
	             
	             runOnUiThread(new Runnable() {
	            	 public void run() {
	            		 mProgressDialog.setMax(max);
	            	 }
	             });
	         } else if(eventType == XmlPullParser.START_TAG && "info".equals(xpp.getName().toLowerCase())) {
	             pl.mInfo = xpp.nextText();
	         } else if(eventType == XmlPullParser.START_TAG && "location".equals(xpp.getName().toLowerCase())) {
	             pl.mLocation = xpp.nextText();
	         } else if(eventType == XmlPullParser.START_TAG && "tracklist".equals(xpp.getName().toLowerCase())) {
	        	 tracklist = new TrackList();
	        	 pl.mPlayList[i++] = tracklist;
	             j = 0;	//reset track count
	             eventType = xpp.next();
	             start += trackcount;
	             tracklist.mStart = start;
	             while (eventType != XmlPullParser.END_TAG || !"tracklist".equals(xpp.getName().toLowerCase())) {
	            	 if(eventType == XmlPullParser.START_TAG && "title".equals(xpp.getName().toLowerCase())) {
	            		 if(pl.mTrackListCount > 1) {
		            		 tracklist.mTitle = xpp.nextText();
		        			 String msg = tracklist.mTitle;
		        			 publishProgress(new ProgressData(msg, i));
	            		 }
	            	 } else if(eventType == XmlPullParser.START_TAG && "trackcount".equals(xpp.getName().toLowerCase())) {
	                     trackcount = Integer.valueOf(xpp.nextText());
	                     tracklist.mEnd = start + trackcount;
	            	 } else if(eventType == XmlPullParser.START_TAG && "info".equals(xpp.getName().toLowerCase())) {
	                     tracklist.mInfo = xpp.nextText();
	            	 } else if(eventType == XmlPullParser.START_TAG && "location".equals(xpp.getName().toLowerCase())) {
	                     tracklist.mLocation = xpp.nextText();
	                 } else if (eventType == XmlPullParser.START_TAG && "track".equals(xpp.getName().toLowerCase())) {
	                     track = new Track();
	                     pl.mTrackList[start + (j++)] = track;
	                     eventType = xpp.next();
	                     while (eventType != XmlPullParser.END_TAG || !"track".equals(xpp.getName().toLowerCase())) {
	                    	 if(eventType == XmlPullParser.START_TAG && "title".equals(xpp.getName().toLowerCase())) {
	                             track.mTitle = xpp.nextText();
	    	                	 if(pl.mTrackListCount > 1) {
	    		                	 int processPercent = ((int) ((j / (float) trackcount) * pl.mTrackListCount));
	    		                	 String msg = null;
	    		        			 publishProgress(new ProgressData(msg, processPercent));
	    	                	 } else {
	    		                	 String msg = track.mTitle;;
	    		        			 publishProgress(new ProgressData(msg, j));
	    	                	 }
	                         } else if(eventType == XmlPullParser.START_TAG && "location".equals(xpp.getName().toLowerCase())) {
	                             track.mLocation = xpp.nextText();
	                         } else if(eventType == XmlPullParser.START_TAG && "info".equals(xpp.getName().toLowerCase())) {
	                             track.mInfo = xpp.nextText();
	                         }
	                    	 eventType = xpp.next();
	                     }	//inner while
	                 }
	            	 eventType = xpp.next();
	             }
	         }
	         eventType = xpp.next();
	        }
	    }
	}
	
	static final int DIALOG_EXIT_ID = 0;
	static final int DIALOG_PROGRESS_ID = 1;

	@Override
	protected Dialog onCreateDialog(int id) {
	    Dialog dialog;
	    switch(id) {
	    case DIALOG_EXIT_ID:	//TODO: make it a static singleton shared by all activities?
	    						//make sure it meets the activity life cycle so that not to leak memory.
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setMessage("Are you sure you want to close this app which means to kill the music playing? You can close this screen but keep the music playing by pressing BACK hard button.")
	    	       .setCancelable(false)
	    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	        	   Utils.quit(TSB.this);
	    	           }
	    	       })
	    	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	                dialog.cancel();
	    	           }
	    	       });
	    	dialog = builder.create();
	        break;
	    case DIALOG_PROGRESS_ID:
	    	ProgressDialog progressDialog;
	    	progressDialog = new ProgressDialog(TSB.this);
			progressDialog.setMax(200);
			progressDialog.setProgress(0);
			progressDialog.setSecondaryProgress(0);
	    	progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	    	progressDialog.setTitle("Loading Bible version...");
	    	progressDialog.setMessage("Loading...");
	    	progressDialog.setCancelable(false);		//TODO: make it cancelable
	    	dialog = progressDialog;
	        break;
	    default:
	        dialog = null;
	    }
	    return dialog;
	}
	
	private ProgressData mProgressData;
	private ProgressDialog mProgressDialog;
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		
	    switch(id) {
	    case DIALOG_PROGRESS_ID:
	    	mProgressDialog = (ProgressDialog)dialog;
	    	if(mProgressData != null) {
	    		if(mProgressData.msg != null) {
	    			String msg;
	    			if(mProgressData.msg.length() > 20)
	    				msg = mProgressData.msg.substring(0, 20) + "...";
	    			else msg = mProgressData.msg;
	    			
	    			((ProgressDialog)dialog).setMessage(msg);
			    	((ProgressDialog)dialog).setProgress(mProgressData.percent);
	    		} else {
	    			((ProgressDialog)dialog).setSecondaryProgress(mProgressData.percent);
	    		}
	    	}
	        break;
	    default:
	    }
	}
	
    public static final int MENU_ITEM_HOME = Menu.FIRST;
    public static final int MENU_ITEM_QUIT = Menu.FIRST + 1;
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_ITEM_QUIT, 0, R.string.menu_quit).setShortcut('0',
				'q').setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_HOME:
			Intent i = new Intent(this, TSB.class);
			startActivity(i);
			return true;
		case MENU_ITEM_QUIT:
			showDialog(DIALOG_EXIT_ID);
			return true;
		default:
			return false;
		}
	}
}