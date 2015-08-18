package com.mot.test2go;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

// TODO: Review this class.
public class FileReadActivity extends ListActivity {

	private HorizontalScrollView hScroll;
	private float mx;
	private float my;
	private ScrollView vScroll;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_read_list);
		// Show the Up button in the action bar.
		setupActionBar();

		hScroll = (HorizontalScrollView) findViewById(R.id.hScroll);
		vScroll = (ScrollView) findViewById(R.id.vScroll);

		loadFile();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		float curX, curY;

		switch(event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mx = event.getX();
			my = event.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			curX = event.getX();
			curY = event.getY();
			hScroll.scrollBy((int)(mx - curX), (int)(my - curY));
			vScroll.scrollBy((int)(mx - curX), (int)(my - curY));
			mx = curX;
			my = curY;
			break;
		case MotionEvent.ACTION_UP:
			curX = event.getX();
			curY = event.getY();
			hScroll.scrollBy((int)(mx - curX), (int)(my - curY));
			vScroll.scrollBy((int)(mx - curX), (int)(my - curY));
			break;
		}

		return true;
	}

	protected void loadFile() {
		String[] lines = getIntent().getStringExtra("content").split("\n");
		ArrayAdapter<String> adapter = new ArrayAdapter<String> (this, R.layout.activity_file_read_row, R.id.textView_file_read_row, lines);
		setListAdapter(adapter);
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
		getMenuInflater().inflate(R.menu.file_read, menu);
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
