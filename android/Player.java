package com.toelim.tsb;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;


import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.toelim.tsb.R;
import com.toelim.tsb.IMediaPlaybackService;
import com.toelim.tsb.IMediaPlaybackService.Stub;
import com.toelim.tsb.R.id;
import com.toelim.tsb.R.layout;
import com.toelim.tsb.R.string;
import com.toelim.tsb.Log;
import com.toelim.tsb.MediaPlaybackService;
import com.toelim.tsb.RepeatingImageButton;
import com.toelim.tsb.TSB;
import com.toelim.tsb.TSB.LoadPlayListTask;

import static com.toelim.tsb.MusicUtils.*;
import static com.toelim.tsb.PlayList.*;
import static com.toelim.tsb.Utils.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class Player extends Activity implements View.OnLongClickListener {
	private MySpinner mChapterSpinner;
	private MySpinner mSpinner;
	private TextView mPlayList;
	private TextView mTrackList;
	private TextView mTrackName;
	private ImageButton mPlayButton;
	private ImageButton mPrevButton;
	private ImageButton mNextButton;
	private RepeatingImageButton mFFButton;
	private RepeatingImageButton mRewButton;
	private SeekBar mProgress;
	private TextView mCurrentTime;
	private TextView mStatus;
	private TextView mTotalTime;
	private ConnectivityManager mConnMngr;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player);
        
       // TSB tsb = new TSB();
      //  tsb.loadstart(0,0);
        
        setTitle("The Streaming Bible");
        
        mConnMngr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        mChapterSpinner = (MySpinner)findViewById(R.id.chapter_spinner);
        mSpinner = (MySpinner)findViewById(R.id.verse_spinner);
        mPlayList = (TextView)findViewById(R.id.playlist);
        mTrackList = (TextView)findViewById(R.id.tracklist);
        mTrackName = (TextView)findViewById(R.id.track_name);
        mPlayButton = (ImageButton)findViewById(R.id.play_button);
        mPrevButton = (ImageButton)findViewById(R.id.previous_button);
        mNextButton = (ImageButton)findViewById(R.id.next_button);
        mFFButton = (RepeatingImageButton)findViewById(R.id.ff_button);
        mRewButton = (RepeatingImageButton)findViewById(R.id.rew_button);
        mProgress = (SeekBar)findViewById(android.R.id.progress);
        mCurrentTime = (TextView)findViewById(R.id.currenttime);
        mStatus = (TextView)findViewById(R.id.statustext);
        mTotalTime = (TextView)findViewById(R.id.totaltime);
        
        //for API 1.5 compatibility
        //if ("cupcake".equalsIgnoreCase(android.os.Build.VERSION.SDK)) {
        	mPlayButton.setOnClickListener(handler);
        	mPrevButton.setOnClickListener(handler);
        	mNextButton.setOnClickListener(handler);
        	mFFButton.setOnClickListener(handler);
        	mRewButton.setOnClickListener(handler);
        //}
        
        mFFButton.setRepeatListener(mFfwdListener, 260);
        mRewButton.setRepeatListener(mRewListener, 260);
        
        if (mProgress instanceof SeekBar) {
            SeekBar seeker = (SeekBar) mProgress;
            seeker.setOnSeekBarChangeListener(mSeekListener);
        }
        mProgress.setMax(1000);
        
        mChapterSpinner.setOnItemSelectedListener(mChapterSpinnerListner);
        mSpinner.setOnItemSelectedListener(mSpinnerListner);
        
        Intent i = getIntent();
        Bundle b = i.getExtras();
        String startfrom = b.getString("from");
		if(Log.LOGD) Log.d("Player/onCreate");
		if(Log.LOGD) Log.d("sCurLANG="+sCurLANG);
		if(Log.LOGD) Log.d("sCurPlayListIndex="+sCurPlayListIndex);
		if(Log.LOGD) Log.d("startfrom="+startfrom);
        if("button".equalsIgnoreCase(startfrom)) {
        	int curPlayListIndex = b.getInt("playlist");
        	int curLANG = b.getInt("language");
        	try {
        		
				if(sService != null && sService.isPlaying()
						&& sCurPlayListIndex == curPlayListIndex
						&& sCurLANG == curLANG) {
					// act as if from notification
					long time = sService.position();
					long duration = sService.duration();
					if(duration > 0) mProgress.setProgress((int)(1000 * time/duration));
					mCurrentTime.setText(makeTimeString(this, time/1000));
					if(duration > 0) mTotalTime.setText(makeTimeString(this, duration/1000));
					
					mChapterSpinner.fromuser = false;
					mSpinner.fromuser = false;
					mChapterSpinner.fromnotification = true;
					mSpinner.fromnotification = true;
					mStatus.setText("");
				} else {	//play the last saved track
			        TSBApplication app = (TSBApplication)getApplication();
			        app.restorePositions(app.preferences);
			        
				    sCurPlayListIndex = b.getInt("playlist");
				    sCurLANG = b.getInt("language");
				    //playlist is already loaded at this time
				    //getPlayList(sCurLANG, sCurPlayListIndex);
				    sCurTrackListPosition = sCurPlayList.mSavedTrackListPosition;
				    sCurTrackPosition = sCurPlayList.mSavedTrackPosition;
				    sCurTrackLoc = sCurPlayList.mLocation + "/" + sCurPlayList.mTrackListsLocation[sCurTrackListPosition] + "/" + sCurPlayList.mTracksLocation[sCurTrackPosition];
				    sCurPosition = sCurPlayList.mSavedPosition;
				    
					mChapterSpinner.fromuser = false;
					mSpinner.fromuser = false;
					mChapterSpinner.fromnotification = false;
					mSpinner.fromnotification = false;
					
					mStatus.setText("Initializing...");
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        } else {
        	long time;
        	long duration;
        	if("notification".equalsIgnoreCase(startfrom) && sService != null) {
        		try {
        			time = sService.position();
					duration = sService.duration();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					time = b.getLong("time");
		        	duration = b.getLong("duration");
				}
        	} else {
	        	time = b.getLong("time");
	        	duration = b.getLong("duration");
        	}
        	if(Log.LOGD) Log.d("Player/onCreate: time=" + time);
        	if(Log.LOGD) Log.d("Player/onCreate: duration=" + duration);
        	if(duration > 0) mProgress.setProgress((int)(1000 * time/duration));
        	mCurrentTime.setText(makeTimeString(this, time/1000));
        	if(duration > 0) mTotalTime.setText(makeTimeString(this, duration/1000));
        	
        	mChapterSpinner.fromuser = false;
        	mSpinner.fromuser = false;
        	mChapterSpinner.fromnotification = true;
        	mSpinner.fromnotification = true;
        	mStatus.setText("");
        	
        }
                
        if (sCurLANG == ENGLISH && sCurPlayListIndex == PLAYLIST_KIDZ) {
        	mChapterSpinner.setVisibility(View.GONE);
        	mTrackList.setVisibility(View.GONE);
        }
        
        String[] playList = sCurPlayList.mTrackListsTitle;
        ArrayAdapter<?> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, playList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mChapterSpinner.setAdapter(adapter);
  
    	//sCurTrackList = sCurPlayList.mPlayList[sCurTrackListPosition];
    	mChapterSpinner.setSelection(sCurTrackListPosition);
    	
        String[] trackList = getTrackList(sCurPlayList, sCurTrackListPosition);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, trackList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        
    	//sCurTrack = sCurPlayList.mTrackList[sCurTrackList.mStart + sCurTrackPosition];
    	mPlayList.setText(sCurPlayList.mTitle);
    	mTrackList.setText(getTrackListTitle());
    	mTrackName.setText(sCurPlayList.mTracksTitle[sCurTrackPosition]);
    	mSpinner.setSelection(sCurTrackPosition - sCurPlayList.mTrackListsStart[sCurTrackListPosition]);
    	
    }

    private String[] getTrackList(PlayList pl, int tracklistposition) {
    	int start = pl.mTrackListsStart[tracklistposition];
    	int end = pl.mTrackListsEnd[tracklistposition];
    	String[] trackList = new String[end - start];
    	for (int i = start; i < end; i++) {
    		trackList[i - start] = pl.mTracksTitle[i];
    	}
    	
    	return trackList;
    }
    
    private String getTrackListTitle() {
    	String title = sCurPlayList.mTrackListsTitle[sCurTrackListPosition];
    	return title != null ? title : sCurPlayList.mTitle;
    }

    @Override
    public void onStart() {
        super.onStart();
        paused = false;

        if(sService != null) mService = sService;
        else {
	        mToken = bindToService(this, osc);
	        if (mToken == null) {
	            // something went wrong
	            mHandler.sendEmptyMessage(QUIT);
	        }
        }
        
        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.PLAYBACK_COMPLETE);
        registerReceiver(mStatusListener, new IntentFilter(f));
        
//        try {
//			if(mService != null && mService.isPlaying()) {
			    updateTrackInfo();
			    long next = refreshNow();
			    queueNextRefresh(next);
//			}
//		} catch (RemoteException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
    	setPauseButtonImage();
    	
    	sCurrentPlayer = this;
    }

    private boolean mOneShot = false;
	static Player sCurrentPlayer;	//callback handler for registered listeners in the service
	
    @Override
    public void onStop() {
    	try {
			if(sService != null && !sService.isPlaying()) {
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
		
        paused = true;
        if (mService != null && mOneShot && getChangingConfigurations() == 0) {
            try {
                mService.stop();
            } catch (RemoteException ex) {
            }
        }
        mHandler.removeMessages(REFRESH);
        unregisterReceiver(mStatusListener);
        if(mToken != null)
        	unbindFromService(mToken);
        mService = null;
        
        super.onStop();
    }
    
    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);

    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {	
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onRestoreInstanceState(Bundle State) {
    	mChapterSpinner.fromuser = false;
    	mSpinner.fromuser = false;
    	mChapterSpinner.fromnotification = true;
    	mSpinner.fromnotification = true;
    	
        super.onRestoreInstanceState(State);
    }
    
    @Override
    public void onPause() {	
        super.onPause();
        
        mHandler.removeMessages(REFRESH);
        
        //make sure the positions are saved in case of system kill
        //in normal case, it is done through onTerminate()
        TSBApplication app = (TSBApplication)getApplication();
        app.savePositions(app.preferences);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
    	mChapterSpinner.fromuser = false;
    	mSpinner.fromuser = false;
    	mChapterSpinner.fromnotification = true;
    	mSpinner.fromnotification = true;
        // Checks the orientation of the screen
        //if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        //    Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        //} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
        //    Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        //}
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateTrackInfo();
        setPauseButtonImage();
    }
    
    
    @Override
    public void onLowMemory()
    {
    	sCurrentPlayer = null;
    	
        super.onLowMemory();
    }
    
    @Override
    public void onDestroy()
    {
    	sCurrentPlayer = null;
    	
        super.onDestroy();
    }
    

    private AdapterView.OnItemSelectedListener mChapterSpinnerListner = new AdapterView.OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			if(mChapterSpinner.fromuser) {
				mChapterSpinner.fromuser = false;
				if(sCurTrackListPosition != position) {
					sCurTrackListPosition = position;
					//sCurTrackList = sCurPlayList.mPlayList[sCurTrackListPosition];
			    	mTrackList.setText(getTrackListTitle());
			    	
			        String[] trackList = getTrackList(sCurPlayList, sCurTrackListPosition);
			        ArrayAdapter<?> adapter = new ArrayAdapter<String>(Player.this, android.R.layout.simple_spinner_item, trackList);
			        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			        mSpinner.setAdapter(adapter);
			        
					//sCurTrackPosition = 0;
					//sCurTrack = sCurPlayList.mTrackList[sCurTrackList.mStart + sCurTrackPosition];
			        sCurTrackPosition = sCurPlayList.mTrackListsStart[sCurTrackListPosition];
			        sCurTrackLoc = sCurPlayList.mLocation + "/" + sCurPlayList.mTrackListsLocation[sCurTrackListPosition] + "/" + sCurPlayList.mTracksLocation[sCurTrackPosition];
			        sCurPosition = 0;
			        //sCurPlayList.mIsPaused = true;
			        mSpinner.fromuser = false;
					mSpinner.fromnotification = false;
					mSpinner.setSelection(sCurTrackPosition - sCurPlayList.mTrackListsStart[sCurTrackListPosition]);
				}
			} else {
				if(mChapterSpinner.fromnotification) {
					mChapterSpinner.fromnotification = false;
				}
				sCurTrackListPosition = position;
				//sCurTrackList = sCurPlayList.mPlayList[sCurTrackListPosition];
			}
		}
		
		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// TODO Auto-generated method stub

		}
    	
    };
    
   
    
    private AdapterView.OnItemSelectedListener mSpinnerListner = new AdapterView.OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			if(mSpinner.fromuser) {
				mSpinner.fromuser = false;
				if(sCurTrackPosition - sCurPlayList.mTrackListsStart[sCurTrackListPosition] != position) {
					sCurTrackPosition = position + sCurPlayList.mTrackListsStart[sCurTrackListPosition];
					sCurTrackLoc = sCurPlayList.mLocation + "/" + sCurPlayList.mTrackListsLocation[sCurTrackListPosition] + "/" + sCurPlayList.mTracksLocation[sCurTrackPosition];
					//sCurTrack = sCurPlayList.mTrackList[sCurTrackList.mStart + sCurTrackPosition];
					sCurPosition = 0;
					//sCurPlayList.mIsPaused = true;
					if(sService != null) {
						startPlayback();
					}
				}
			} else {
				sCurTrackPosition = sCurPlayList.mTrackListsStart[sCurTrackListPosition] + position;
				sCurTrackLoc = sCurPlayList.mLocation + "/" + sCurPlayList.mTrackListsLocation[sCurTrackListPosition] + "/" + sCurPlayList.mTracksLocation[sCurTrackPosition];
				//sCurTrackPosition = position;
				//sCurTrack = sCurPlayList.mTrackList[sCurTrackList.mStart + sCurTrackPosition];
				if(mSpinner.fromnotification) {
					mSpinner.fromnotification = false;
			        updateTrackInfo();
			        setPauseButtonImage();
				} else {	// from Button
					if(sService != null) {
						startPlayback();
					}
				}
			}
		}
		
		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// TODO Auto-generated method stub

		}
    	
    };
    
    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            mLastSeekEventTime = 0;
            mFromTouch = true;
        }
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser || (mService == null)) return;
            
            long now = SystemClock.elapsedRealtime();
            if ((now - mLastSeekEventTime) > 250) {
                mLastSeekEventTime = now;
                mPosOverride = mDuration * progress / 1000;

                try {
					mService.seek(mPosOverride);	//in milliseconds
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
                /*
                new Thread(new Runnable() {
            		public void run() {
            			try {
							mService.seek(mPosOverride);	//in milliseconds
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
            		}
            	}).start();*/

                // trackball event, allow progress updates
                if (!mFromTouch) {
                    refreshNow();
                    mPosOverride = -1;
                }
            }
        }
        public void onStopTrackingTouch(SeekBar bar) {
            mPosOverride = -1;
            mFromTouch = false;
        }
    };

    /* old play
	private void play() {
		mTrackName.setText(mTrack.mTitle);
		if(mMP == null) mMP = new MediaPlayer();
		
		try {
			mMP.setDataSource(mTrack.mLocation);
			mMP.prepare();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		mMP.start();
	}
*/
    
    //unused. use repeatlistener
    public boolean  onLongClick(View v) {
        switch (v.getId()) {
        case R.id.ff_button:
        	updatePosition(10000);	//10 secs
            break;        	
        case R.id.rew_button:
        	updatePosition(-10000);		//10 secs
        	break;
        }
        
    	return false;
    }

    private RepeatingImageButton.RepeatListener mRewListener =
        new RepeatingImageButton.RepeatListener() {
        public void onRepeat(View v, long howlong, int repcnt) {
            scanBackward(repcnt, howlong);
        }
    };
    
    private RepeatingImageButton.RepeatListener mFfwdListener =
        new RepeatingImageButton.RepeatListener() {
        public void onRepeat(View v, long howlong, int repcnt) {
            scanForward(repcnt, howlong);
        }
    };

    private void scanBackward(int repcnt, long delta) {
        if(mService == null) return;
        try {
            if(repcnt == 0) {
                mStartSeekPos = mService.position();
                mLastSeekEventTime = 0;
                //mSeeking = false;
            } else {
                //mSeeking = true;
                if (delta < 5000) {
                    // seek at 10x speed for the first 5 seconds
                    delta = delta * 10; 
                } else {
                    // seek at 40x after that
                    delta = 50000 + (delta - 5000) * 40;
                }
                long newpos = mStartSeekPos - delta;
                if (newpos < 0) {
                    // move to previous track
                    mService.prev();
                    long duration = mService.duration();
                    mStartSeekPos += duration;
                    newpos += duration;
                }
                if (((delta - mLastSeekEventTime) > 250) || repcnt < 0){
        			if(newpos >= 0 && newpos * 100 <= mBufferingPercent * mService.duration())
        				mService.seek(newpos);
        			mLastSeekEventTime = delta;
        			
        			/*
                	final long pos = newpos;
                	final long final_delta = delta;
                	new Thread(new Runnable() {
                		public void run() {
                			try {
                				mService.seek(pos);
                                mLastSeekEventTime = final_delta;
    						} catch (RemoteException e) {
    							// TODO Auto-generated catch block
    							e.printStackTrace();
    						}
                		}
                	}).start();
                    */
                }
                if (repcnt >= 0) {
                    mPosOverride = newpos;
                } else {
                    mPosOverride = -1;
                }
                refreshNow();
            }
        } catch (RemoteException ex) {
        }
    }

    private void scanForward(int repcnt, long delta) {
        if(mService == null) return;
        
        try {
            if(repcnt == 0) {
                mStartSeekPos = mService.position();
                mLastSeekEventTime = 0;
                //mSeeking = false;
            } else {
                //mSeeking = true;
                if (delta < 5000) {
                    // seek at 10x speed for the first 5 seconds
                    delta = delta * 10; 
                } else {
                    // seek at 40x after that
                    delta = 50000 + (delta - 5000) * 40;
                }
                long newpos = mStartSeekPos + delta;
                long duration = mService.duration();
                if (newpos >= duration) {
                    // move to next track
                    mService.next();
                    mStartSeekPos -= duration; // is OK to go negative
                    newpos -= duration;
                }
                if (((delta - mLastSeekEventTime) > 250) || repcnt < 0){
        			if(newpos >= 0 && newpos * 100 <= mBufferingPercent * mService.duration())
        				mService.seek(newpos);
        			mLastSeekEventTime = delta;
        			
        			/*
                	final long pos = newpos;
                	final long final_delta = delta;
                	new Thread(new Runnable() {
                		public void run() {
                			try {
                				mService.seek(pos);
                                mLastSeekEventTime = final_delta;
    						} catch (RemoteException e) {
    							// TODO Auto-generated catch block
    							e.printStackTrace();
    						}
                		}
                	}).start();
                	*/
                }
                if (repcnt >= 0) {
                    mPosOverride = newpos;
                } else {
                    mPosOverride = -1;
                }
                refreshNow();
            }
        } catch (RemoteException ex) {
        }
    }

    public void myClickHandler(View v) {
        switch (v.getId()) {
        case R.id.ff_button:
			updatePosition(1000);	//1 sec
            break;        	
        case R.id.rew_button:
			updatePosition(-1000);	//1 sec
            break;
        case R.id.previous_button:
        	prev();
            break;
        case R.id.next_button:
        	next();
            break;
        case R.id.play_button:
        	doPauseResume();
            break;
        }
    }

    //for API 1.5 compatibility
    View.OnClickListener handler = new View.OnClickListener() {
        public void onClick(View v) {
        	myClickHandler(v);
        }
    };

    private void updatePosition(final long delta) {
		long newpos;
		try {
			newpos = mService.position() + delta;
			if(newpos >= 0 && newpos * 100 <= mBufferingPercent * mService.duration())
				mService.seek(newpos);

			refreshNow();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
    private void prev() {
		//if (sCurTrackPosition > 0) {
    	if (sCurTrackPosition > sCurPlayList.mTrackListsStart[sCurTrackListPosition]) {
			sCurTrackPosition --;
			sCurTrackLoc = sCurPlayList.mLocation + "/" + sCurPlayList.mTrackListsLocation[sCurTrackListPosition] + "/" + sCurPlayList.mTracksLocation[sCurTrackPosition];
			//sCurTrack = sCurPlayList.mTrackList[sCurTrackList.mStart + sCurTrackPosition];
			mSpinner.setSelection(sCurTrackPosition - sCurPlayList.mTrackListsStart[sCurTrackListPosition]);
			//mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
			sCurPosition = 0;
		} else {
			if (sCurTrackListPosition > 0) {
				sCurTrackListPosition --;
				//sCurTrackList = sCurPlayList.mPlayList[sCurTrackListPosition];
				sCurTrackPosition --;
				sCurTrackLoc = sCurPlayList.mLocation + "/" + sCurPlayList.mTrackListsLocation[sCurTrackListPosition] + "/" + sCurPlayList.mTracksLocation[sCurTrackPosition];
				//sCurTrack = sCurPlayList.mTrackList[sCurTrackList.mStart + sCurTrackPosition];
				mChapterSpinner.setSelection(sCurTrackListPosition);
				
		        String[] trackList = getTrackList(sCurPlayList, sCurTrackListPosition);
		        ArrayAdapter<?> adapter = new ArrayAdapter<String>(Player.this, android.R.layout.simple_spinner_item, trackList);
		        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		        mSpinner.setAdapter(adapter);
				mSpinner.setSelection(sCurTrackPosition - sCurPlayList.mTrackListsStart[sCurTrackListPosition]);
				//mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
				sCurPosition = 0;
			} else {
				mTrackName.setText("You are at the beginning of this playlist.");
			}
		}
    }
	
    private void next() {
    	if (sCurTrackPosition < sCurPlayList.mTrackListsEnd[sCurTrackListPosition] - 1) {
    		sCurTrackPosition ++;
    		sCurTrackLoc = sCurPlayList.mLocation + "/" + sCurPlayList.mTrackListsLocation[sCurTrackListPosition] + "/" + sCurPlayList.mTracksLocation[sCurTrackPosition];
    		//sCurTrack = sCurPlayList.mTrackList[sCurTrackList.mStart + sCurTrackPosition];
    		mSpinner.setSelection(sCurTrackPosition - sCurPlayList.mTrackListsStart[sCurTrackListPosition]);
    		//mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
    		sCurPosition = 0;
    	} else {
        	if (sCurTrackListPosition < sCurPlayList.mTrackListsTitle.length - 1) {
        		sCurTrackListPosition ++;
        		//sCurTrackList = sCurPlayList.mPlayList[sCurTrackListPosition];
        		sCurTrackPosition = sCurPlayList.mTrackListsStart[sCurTrackListPosition];
        		sCurTrackLoc = sCurPlayList.mLocation + "/" + sCurPlayList.mTrackListsLocation[sCurTrackListPosition] + "/" + sCurPlayList.mTracksLocation[sCurTrackPosition];
        		//sCurTrack = sCurPlayList.mTrackList[sCurTrackList.mStart + sCurTrackPosition];
        		mChapterSpinner.setSelection(sCurTrackListPosition);
        		
        		String[] trackList = getTrackList(sCurPlayList, sCurTrackListPosition);
		        ArrayAdapter<?> adapter = new ArrayAdapter<String>(Player.this, android.R.layout.simple_spinner_item, trackList);
		        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		        mSpinner.setAdapter(adapter);
        		mSpinner.setSelection(sCurTrackPosition - sCurPlayList.mTrackListsStart[sCurTrackListPosition]);
        		//mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
        		sCurPosition = 0;
        	} else {
        		mTrackName.setText("You have reached the end of this playlist.");
        	}
    	}
    }
    
    private ServiceConnection osc = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
	        mService = IMediaPlaybackService.Stub.asInterface(obj);
            /* we are not using this play-rightaway feature
            startPlayback();
            try {
                // Assume something is playing when the service says it is,
                // but also if the audio ID is valid but the service is paused.
                if (mService.getAudioId() >= 0 || mService.isPlaying() ||
                        mService.getPath() != null) {
                    // something is playing now, we're done

                    setPauseButtonImage();
                    return;
                }
            } catch (RemoteException ex) {
            }
            // Service is dead or not playing anything. If we got here as part
            // of a "play this file" Intent, exit. Otherwise go to the Music
            // app start screen.
            if (getIntent().getData() == null) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setClass(KidzPlayer.this, TSB.class);
                startActivity(intent);
            }
            finish();
            */
        }
        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };

    private long mStartSeekPos = 0;
    private long mLastSeekEventTime;
    private long mPosOverride = -1;
    private boolean mFromTouch = false;
    private long mDuration;
    
    private IMediaPlaybackService mService = null;
    private Toast mToast;
    private ServiceToken mToken;
    
    private boolean paused;
    
    private static final int REFRESH = 1;
    private static final int QUIT = 2;
    
    private void queueNextRefresh(long delay) {
        if (!paused) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }

    private long refreshNow() {
        if(mService == null)
            return 500;
        try {
            long pos = mPosOverride <= 0 ? mService.position() : mPosOverride;
            if(Log.LOGD) Log.d("Player/refreshNow: pos=" + pos);
            
            long remaining = 1000 - (pos % 1000);
            if ((pos >= 0) && (mDuration > 0)) {
                mCurrentTime.setText(makeTimeString(this, pos / 1000));
                
                if (mService.isPlaying()) {
                    mCurrentTime.setVisibility(View.VISIBLE);
                } else {
                    // blink the counter
                    int vis = mCurrentTime.getVisibility();
                    mCurrentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
                    remaining = 500;
                }

                mProgress.setProgress((int) (1000 * pos / mDuration));
            } else {
                mCurrentTime.setText("--:--");
                mProgress.setProgress(1000);
            }
            // return the number of milliseconds until the next full second, so
            // the counter can be updated at just the right time
            return remaining;
        } catch (RemoteException ex) {
        }
        return 500;
    }
    
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH:
                    long next = refreshNow();
                    queueNextRefresh(next);
                    break;
                    
                case QUIT:
                    // This can be moved back to onCreate once the bug that prevents
                    // Dialogs from being started from onCreate/onResume is fixed.
                    new AlertDialog.Builder(Player.this)
                            .setTitle(R.string.service_start_error_title)
                            .setMessage(R.string.service_start_error_msg)
                            .setPositiveButton(R.string.service_start_error_button,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            finish();
                                        }
                                    })
                            .setCancelable(false)
                            .show();
                    break;

                default:
                    break;
            }
        }
    };

    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MediaPlaybackService.META_CHANGED)) {
                // redraw the artist/title info and
                // set new max for progress bar
                updateTrackInfo();
                setPauseButtonImage();
                queueNextRefresh(1);
            } else if (action.equals(MediaPlaybackService.PLAYBACK_COMPLETE)) {
                next();
            } else if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
                setPauseButtonImage();
            }
        }
    };

    private void updateTrackInfo() {
        if (mService == null) {
            return;
        }
        try {
            String path = mService.getPath();
            ////if (path == null) {
                ////finish();
            	////showToast(this, "Please tap Play button or choose a track to play.", true);
                ////return;
            ////}
            /*
            long songid = mService.getAudioId(); 
            if (songid < 0 && path.toLowerCase().startsWith("http://")) {
                // Once we can get album art and meta data from MediaPlayer, we
                // can show that info again when streaming.
                mTrackName.setText(path);
            } else {
                mTrackName.setText(mService.getTrackName());
            }*/
            mTrackList.setText(getTrackListTitle());
            mTrackName.setText(sCurPlayList.mTracksTitle[sCurTrackPosition]);
            if(path != null && mService.isInitialized()) {
	            mDuration = mService.duration();
	            if(mDuration > 0)
	            	mTotalTime.setText(makeTimeString(this, mDuration / 1000));
            }
            //mCurrentTime.setText(makeTimeString(this, mService.position() / 1000));
        } catch (RemoteException ex) {
        	showToast(this, "Something wrong with the music background service. You may restart the app.");
            ////finish();
        }
    }
    
    private void setPauseButtonImage() {
        try {
            if (mService != null && mService.isPlaying()) {
                mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
                //mStatus.setText("Playing...");
            } else {
                mPlayButton.setImageResource(android.R.drawable.ic_media_play);
                //mStatus.setText("Paused");
            }
        } catch (RemoteException ex) {
        }
    }
    
    private void startPlayback() {
    	enableUI();
    	if(!startPlaybackInternal()) {
    		mStatus.setText("");
    		mPlayButton.setImageResource(android.R.drawable.ic_media_play);
    	}
    }
    
    private boolean startPlaybackInternal() {
        if(sService == null) return false;
        
        try {
        	if(Log.LOGD) Log.d("Player/startPlayback: " + sCurPlayList.mLocation);
			if(sCurPlayList.mLocation.startsWith("http://")
					&& !hasConnection(this, mConnMngr)
					) {
				showToast(this, "This track needs Internet connection. Currently either there is no network connectivity or there is no permission to access network.", true);
			} else {
				try {
					if(sCurPlayList.mTracksLocation[sCurTrackPosition].equalsIgnoreCase("info.php")) {
						//TODO:	special case for info.php at the end of kidz playlist
						return false;
					}
						disableUI();
						mIsPrepared = false;
						mBufferingPercent = 0;
	            		mStatus.setText("Preparing...");
	            		mCurrentTime.setText("--:--");
	            		mProgress.setProgress(1000);
	            		mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
		                sService.stop();
		                //String loc = sCurPlayList.mLocation + "/" + sCurPlayList.mTrackListsLocation[sCurTrackListPosition] + "/" + sCurPlayList.mTracksLocation[sCurTrackPosition];
		                if(Log.LOGD) Log.d("Player/startPlayback: " + sCurTrackLoc);
		                sService.openFileAsync(Uri.encode(sCurTrackLoc, ":/"));
				        ////updateTrackInfo();
		                //sService.openFile(Uri.encode(sCurTrackLoc, ":/"), false); //true: oneShot; false: playlist 
		                //sService.play();
					    //disableUI();
					    //updateTrackInfo();
					    //long next = refreshNow();
					    //queueNextRefresh(next);
		                //mStatus.setText("Preparing...");
				        return true;
		            } catch (Exception ex) {
		                Log.d("Player: " + "couldn't start playback: " + ex);
		            }
			}
        } catch (Exception ex) {
            Log.d("Player: " + "couldn't start playback: " + ex);
            //TODO: why it has no ACCESS_NETWORK_STATE permission even it is set in Manifest
            showToast(this, "This track needs Internet connection. Currently either there is no network connectivity or there is no permission to access network.", true);
        }

        return false;
    }
    
    private void disableUI() {
	    mPlayButton.setEnabled(false);
		//mPrevButton.setEnabled(false);
		//mNextButton.setEnabled(false);
		mFFButton.setEnabled(false);
		mRewButton.setEnabled(false);
		mProgress.setEnabled(false);
		//mSpinner.setEnabled(false);
		//mChapterSpinner.setEnabled(false);
    }
	
    private void enableUI() {
	    mPlayButton.setEnabled(true);
		//mPrevButton.setEnabled(true);
		//mNextButton.setEnabled(true);
		mFFButton.setEnabled(true);
		mRewButton.setEnabled(true);
		mProgress.setEnabled(true);
		//mSpinner.setEnabled(true);
		//mChapterSpinner.setEnabled(true);
    }
    
    private void doPauseResume() {
        try {
        	//String loc = sCurPlayList.mLocation + "/" + sCurPlayList.mTrackListsLocation[sCurTrackListPosition] + "/" + sCurPlayList.mTracksLocation[sCurTrackPosition];
            if(mService != null) {
                if(mService.getPath() == null
                		|| (mService.getPath() != null && !mService.getPath().equals(Uri.encode(sCurTrackLoc, ":/")))
                		|| !mService.isInitialized()) {
    				startPlayback();
                } else {
	                if (mService.isPlaying()) {
	                    mService.pause();
	                    
	                    //save the current positions
					    sCurPlayList.mSavedTrackListPosition = sCurTrackListPosition;
					    sCurPlayList.mSavedTrackPosition = sCurTrackPosition;
					    long pos = sService.position();
					    sCurPlayList.mSavedPosition = pos < 0 ? 0 : pos;
					    sCurPlayList.mIsPaused = true;
	                } else {
	                    mService.play();
	                    sCurPlayList.mIsPaused = false;
	                }
	                refreshNow();
	                setPauseButtonImage();
                }
            }
        } catch (RemoteException ex) {
        }
    }
    
    private boolean mIsPrepared = false;
    
	void onPreparedListener(final MediaPlayer mp) {
//		runOnUiThread(new Runnable() {			//local service running on main thread
//			public void run() {
                try {
					if(!mService.isInitialized()) {
						//String loc = sCurPlayList.mLocation + "/" + sCurPlayList.mTrackListsLocation[sCurTrackListPosition] + "/" + sCurPlayList.mTracksLocation[sCurTrackPosition];
						showToast(Player.this, "couldn't open " + sCurTrackLoc + ", please retry or choose another track", true);
						mCurrentTime.setText("-:-");
					} else {
						mIsPrepared = true;
						mStatus.setText("");
						//if(!sCurPlayList.mIsPaused)
							mService.play();
						
						mPlayButton.setEnabled(true);
						mFFButton.setEnabled(true);
						mRewButton.setEnabled(true);
						
						try{
							updateTrackInfo();
						    long next = refreshNow();
						    queueNextRefresh(next);
						} catch (Exception e){}
						
						/*if(mp.isPlaying()) {
						    updateTrackInfo();
						    long next = refreshNow();
						    queueNextRefresh(next);
						}
					    
					    /*
						mStatus.setText("Seeking...");
						mDuration = mService.duration();
						mTotalTime.setText(makeTimeString(Player.this, mDuration / 1000));
						mProgress.setProgress((int) (1000 * sCurPosition / mDuration));
						if(Log.LOGD) Log.d("sCurPosition=" + sCurPosition);
						sCurPosition = mService.seek(sCurPosition);
						if(Log.LOGD) Log.d("sCurPosition=" + sCurPosition);
						*/
					}
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				setPauseButtonImage();
//			}
//		});
		
	}
	
	private static final float safe_delta = 0.95f;
	private int mBufferingPercent;
	
	void updateBufferingProgress(final MediaPlayer mp, final int percent) {
//		runOnUiThread(new Runnable() {
//			public void run() {
				if(percent < 0 || percent > 100) return;
				
				mBufferingPercent = percent;
				mProgress.setSecondaryProgress((int)(percent * mProgress.getMax()/100));

				if (percent == 100) {
					enableUI();		//enable SeekBar
				}
				/* i don't know why the code below causes bufferingUpdate to be less frequent
				 * my effort to prevent playing with insufficient buffering becomes futile.
				try {
					if(mIsPrepared) {
						if (mService.position() * 100 >= percent * safe_delta * mService.duration()) {
								mStatus.setText("Buffering...");
								mService.pause();
								queueNextRefresh(refreshNow());
						}
						if (percent == 100 || mService.position() * 100 < percent * safe_delta * mService.duration()) {
								mStatus.setText("");
								mService.play();
							    queueNextRefresh(refreshNow());
						}
	
					}
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
//			}
//		});
		
	}
	
	//unused now
	void seekComplete(MediaPlayer mp) {
//		runOnUiThread(new Runnable() {
//			public void run() {
			    try {
					mStatus.setText("");
					if(!sCurPlayList.mIsPaused)
						mService.play();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			    disableUI();
			    updateTrackInfo();
			    long next = refreshNow();
			    queueNextRefresh(next);
//			}
//		});
		
	}
	
	void errorListener(final MediaPlayer mp, final int what, final int extra, final String msg) {
//		runOnUiThread(new Runnable() {
//			public void run() {
		
		//setDataSource
                switch (what) {
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                	showToast(Player.this, "Somehow the serverhas lost connection.  Please choose another track.");
                    break;
                case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                	showToast(Player.this, "Not valid for progressive playback. Please choose another track.");
                	break;
                case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                default:
                	if(msg != null)
                		showToast(Player.this, msg);
                	else showToast(Player.this, "Some error with this track. Please choose another track.");
                	break;
                }
//			}
//		});
	}
	
	void infoListener(final MediaPlayer mp, final int what, final int extra, final String msg) {
//		runOnUiThread(new Runnable() {
//			public void run() {
                switch (what) {
                case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                	showToast(Player.this, "Bad interleaving.");
                	break;
                ////case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:	//added since API 5
                case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                	showToast(Player.this, "Not seekable.");
                	break;
                case MediaPlayer.MEDIA_INFO_UNKNOWN:
                case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                default:
                	if(msg != null)
                		showToast(Player.this, msg);
                	break;
                }
//			}
//		});
	}
	
    private void showSimpleAlertDialog(Context context, String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(msg)
		       .setCancelable(false)
		       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
    }
    
    public static final int MENU_ITEM_HOME = Menu.FIRST;
    public static final int MENU_ITEM_QUIT = Menu.FIRST + 1;
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_ITEM_HOME, 0, "About").setShortcut('1','h');
		menu.add(0, MENU_ITEM_QUIT, 0, R.string.menu_quit).setShortcut('0',
				'q').setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ITEM_HOME:
			showDialog(DIALOG_ABOUT);
			return true;
		case MENU_ITEM_QUIT:
			showDialog(DIALOG_EXIT_ID);
			return true;
		default:
			return false;
		}
	}
	
	static final int DIALOG_EXIT_ID = 0;
	static final int DIALOG_PROGRESS_ID = 1;
	static final int DIALOG_ABOUT = 2;

	@Override
	protected Dialog onCreateDialog(int id) {
	    Dialog dialog;
	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    switch(id) {
	    case DIALOG_EXIT_ID:	//TODO: make it a static singleton shared by all activities?
	    						//make sure it meets the activity life cycle so that not to leak memory.
	    	
	    	builder.setMessage("Are you sure you want to close this app which means to kill the music playing? You can close this screen but keep the music playing by pressing BACK hard button.")
	    	       .setCancelable(false)
	    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	        	   Utils.quit(Player.this);
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
	    	progressDialog = new ProgressDialog(Player.this);
			progressDialog.setMax(100);
			progressDialog.setProgress(0);
	    	progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	    	progressDialog.setTitle("Preparing the track...");
	    	progressDialog.setMessage(sCurPlayList.mTracksTitle[sCurTrackPosition]);
	    	progressDialog.setCancelable(false);		//TODO: make it cancelable
	    	dialog = progressDialog;
	        break;
	    case DIALOG_ABOUT:
	    	//AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setMessage("(C)Copyright TSB 2011\n\nDesigned and Developed by Chris Upchurch.")
	    	       .setCancelable(true)
	    	       .setPositiveButton("Done", new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	        	   dialog.cancel();
	    	           }
	    	       });	    	       
	    	dialog = builder.create();
	        break;
	    default:
	        dialog = null;
	    }
	    return dialog;
	}
	
	
	
}


