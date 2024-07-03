package com.toelim.tsb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Process;
import android.widget.Toast;

public class Utils {
	public static Toast sToast;
	
    public static void showToast(Context context, int resid) {
        if (sToast == null) {
            sToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
        }
        sToast.setText(resid);
        sToast.show();
    }

    public static void showToast(Context context, String msg) {
    	if(msg != null) {
	        if (sToast == null) {
	            sToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
	        }
	        sToast.setText(msg);
	        sToast.show();
    	}
    }

    public static void showToast(Context context, int resid, boolean longperiod) {
        if (sToast == null) {
        	if(longperiod)
        		sToast = Toast.makeText(context, "", Toast.LENGTH_LONG);
        	else
        		sToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
        }
        sToast.setText(resid);
        sToast.show();
    }

    public static void showToast(Context context, String msg, boolean longperiod) {
    	if(msg != null) {
        	if(longperiod)
        		sToast = Toast.makeText(context, "", Toast.LENGTH_LONG);
        	else
        		sToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
	        sToast.setText(msg);
	        sToast.show();
    	}
    }
    
	public static Map getMap(Bundle b) {
		Map<String, String> map = new HashMap<String, String>();
		for(String key : b.keySet()) map.put(key, b.getString(key));
		return map;
	}

	public static Bundle getBundle(Map<String, String> m) {
		Bundle b = new Bundle();
		for(Entry<String, String> entry : m.entrySet()) b.putString(entry.getKey(), entry.getValue());
		return b;
	}

    protected static Object unserialize(byte[] bytes) {
    	if (bytes == null) return null;
    	
    	ObjectInput is;
    	Object o = null;
		try {
			is = new ObjectInputStream(new ByteArrayInputStream(bytes));
	    	o = is.readObject();
	    	is.close();
		} catch (StreamCorruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	return o;
    }
    
    protected static byte[] serialize(Object o) {
    	if(o == null) return null;
    	
    	ObjectOutput oos;
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(o);
			oos.flush();
			oos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return baos.toByteArray();
    }
    
    protected static void quit(Context cxt) {
    	showToast(cxt, "Quitting...", true);
    	
    	Application app = null;
    	if(cxt instanceof Activity)
    		app = ((Activity)cxt).getApplication();
    	else if(cxt instanceof Application)
    		app = ((Application)cxt);
    	if(app != null) app.onTerminate();
    	
    	//Process.killProcess(Process.myPid());
    }
}