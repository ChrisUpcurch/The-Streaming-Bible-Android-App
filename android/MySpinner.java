package com.toelim.tsb;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.widget.Spinner;

public class MySpinner extends Spinner {
	public MySpinner(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

    public MySpinner(Context context, AttributeSet attrs)  {
    	super(context, attrs);
    }

    public MySpinner(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle);
    }
    
	public boolean fromuser = false;
	public boolean fromnotification = false;
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		super.onClick(dialog, which);
		
		fromuser = true;
	}
}
