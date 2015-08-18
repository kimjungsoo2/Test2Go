package com.mot.test2go;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

// TODO: Review this class.
public class VScroll extends ScrollView {

	public VScroll(Context context) {
		super(context);
	}

	public VScroll(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public VScroll(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return false;
	}

}
