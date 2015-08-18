package com.mot.test2go;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

// TODO: Review this class.
public class FileViewActivity extends Activity {

//	private ScrollView vScroll;
//	private HorizontalScrollView hScroll;
	private float mx;
	private float my;
	private LinearLayout layoutContainer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_view);

		// Show the Up button in the action bar.
		setupActionBar();

//		vScroll = (ScrollView) findViewById(R.id.vScroll);
//		hScroll = (HorizontalScrollView) findViewById(R.id.hScroll);

		loadFile();
	}

	public void loadFile() {
		layoutContainer = (LinearLayout) findViewById(R.id.LinearLayout_FileContainer);
		String[] lines = getIntent().getStringExtra("content").split("\n");
		TextView textView;

		for (String l : lines) {
			textView = new TextView(this);
			textView.setText(l);
			textView.setTypeface(Typeface.MONOSPACE);
			textView.setTextSize(12);
			textView.setTextColor(getResources().getColor(R.color.xterm_white));
			layoutContainer.addView(textView);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		float curX, curY;

		switch(event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mx = event.getX();
			my = event.getY();
			break;
/*
		case MotionEvent.ACTION_MOVE:
			curX = event.getX();
			curY = event.getY();
			vScroll.scrollBy((int)(mx - curX), (int)(my - curY));
			hScroll.scrollBy((int)(mx - curX), (int)(my - curY));
			mx = curX;
			my = curY;
			break;
		case MotionEvent.ACTION_UP:
			curX = event.getX();
			curY = event.getY();
			vScroll.scrollBy((int)(mx - curX), (int)(my - curY));
			hScroll.scrollBy((int)(mx - curX), (int)(my - curY));
			break;
*/
		}

		return true;
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.file_view, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
