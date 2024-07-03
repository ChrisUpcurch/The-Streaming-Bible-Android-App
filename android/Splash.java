package com.toelim.tsb;

import static com.toelim.tsb.PlayList.*;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.TextView;

public class Splash extends Activity {
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if(PlayList.loaded) {
			startActivity(new Intent(Splash.this, TSB.class));
			finish();
        } else {
	        setContentView(R.layout.splash);
	        setTitle("The Streaming Bible");
	
	        //// preload all playlists to alleviate the player slow startup issue
	    	LoadPlayListTask loadPlayList = new LoadPlayListTask();
	    	loadPlayList.mActivity = this;
	    	loadPlayList.mRes = getResources();
	    	loadPlayList.execute();
        }
    }
    
    private static class ProgressData {
    	String msg;
    	int	percent;
    	
    	ProgressData(String msg, int percent) {
    		this.msg = msg;
    		this.percent = percent;
    	}
    }
    
	private class LoadPlayListTask extends AsyncTask<Void, ProgressData, Void> {
		public Activity mActivity;
		public Resources mRes;

		@Override
		protected void onPreExecute() {
			mActivity.showDialog(DIALOG_PROGRESS_ID);
		}

		@Override
		protected Void doInBackground(Void... p) {
			Void result = null;
			try {
				int progress = 0;
				for(int lang = 0; lang < sPlayLists.length; lang++)
					for(PlayList pl: sPlayLists[lang]) {
						if(pl != null) {
							int processPercent = ((int) ((progress / (float) sPlayListCount) * 100));
							String msg = pl.mTitle + " - " + sLangList[lang];
							////Log.d("TSB/loadPlayListTask/doInBackground: " + pl.mTitle + ":" + sLangList[lang]);
							publishProgress(new ProgressData(msg, processPercent));
							//pl.getPlayList(mRes);
							progress++;
						}
				}
			} catch (NotFoundException e) {
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
			PlayList.loaded = true;
			startActivity(new Intent(Splash.this, TSB.class));
			finish();
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
	    	builder.setMessage("Are you sure you want to close this app including background service?")
	    	       .setCancelable(false)
	    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	        	   Application app = (Application)Splash.this.getApplication();
	    	        	   app.onTerminate();
	    	        	   Process.killProcess(Process.myPid());
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
	    	progressDialog = new ProgressDialog(Splash.this);
			progressDialog.setMax(100);
			progressDialog.setProgress(0);
	    	progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	    	progressDialog.setTitle("Loading Bible versions...");
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
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		
	    switch(id) {
	    case DIALOG_PROGRESS_ID:
	    	if(mProgressData != null) {
		    	((ProgressDialog)dialog).setMessage(mProgressData.msg);
		    	((ProgressDialog)dialog).setProgress(mProgressData.percent);
	    	}
	        break;
	    default:
	    }
	}
}