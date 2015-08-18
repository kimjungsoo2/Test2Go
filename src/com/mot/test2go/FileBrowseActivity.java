package com.mot.test2go;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;

public class FileBrowseActivity extends ListActivity {

	private static final String PARENT = "..";

	private String mCurrentLocation;
    private ArrayList<File> mFiles = new ArrayList<File>();
	private CustomArrayAdapter mAdapter;

	private Map<String, String> mfileGroup;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_file_browse_list);

		// Show the Up button in the action bar.
		setupActionBar();

		// Get the instance of TextView for current path
		//mTextViewLocation = (TextView) findViewById(R.id.textView_location);

		// Bind the list of files to custom array adapter
		mAdapter = new CustomArrayAdapter(this, mFiles);
		setListAdapter(mAdapter);

		// Register Context Menu
		registerForContextMenu(getListView());

		// load files
		try {
            File scriptDirectory = new File(Global.SCRIPT_ROOT);
            System.out.println(Global.SCRIPT_ROOT);
            if (!scriptDirectory.isDirectory()) {
                System.out.println("Renaming file.");
                scriptDirectory.renameTo(new File(scriptDirectory.getParentFile(), "apython.bak"));
            }
            if (!scriptDirectory.exists()) {
                System.out.println("Creating directory.");
                if (scriptDirectory.mkdirs()) {

                }
            }
			getDir(new File(Global.SCRIPT_ROOT));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * List the content of the given directory.
     *
	 * @param fs The directory whose content is to be listed.
	 * @throws IOException
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void getDir(File fs) throws IOException {
		mCurrentLocation = fs.getPath();
		File[] files = fs.listFiles();

		// clear list for the adapter
        mFiles.clear();
        Collections.addAll(mFiles, files);

		// add parent folder if it is not a storage root
		if (!mCurrentLocation.equals(Global.SCRIPT_ROOT)) {
            mFiles.add(new File(fs, PARENT));
		}

		// update current location
		//mTextViewLocation.setText("Location: " + mCurrentLocation);
		getActionBar().setTitle(mCurrentLocation.replace(Global.SCRIPT_ROOT, ""));

		// Sort the by type, then name.
        Collections.sort(mFiles, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                // Sort by type.
                if (lhs.isFile() && rhs.isDirectory()) {
                    return 1;
                } else if (lhs.isDirectory() && rhs.isFile()) {
                    return -1;
                }

                // Sort by name if both files are the same type (both are directories or files).
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        });

		// update list view
		mAdapter.notifyDataSetChanged();
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
		getMenuInflater().inflate(R.menu.file_browser, menu);
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

	@Override
	protected void onListItemClick(ListView lv, View v, int position, long id) {
        try {
            File fs = mAdapter.getItem(position).getCanonicalFile();

            // If it is a directory, list the content of the directory.
            if (fs.isDirectory()) {
                getDir(fs);
            }

            // If it is a file, open it.
            if (fs.isFile()) {
                showContent(fs.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	/**
     * Set the result to be returned to calling activity.
	 */
	private void setScriptPath(String path) {
		Intent returnIntent = new Intent(this, MainActivity.class);
		returnIntent.putExtra(MainActivity.SCRIPT_NAME, path);
		setResult(RESULT_OK, returnIntent);
		finish();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        String filePath = (String) ((AdapterContextMenuInfo) menuInfo).targetView.getTag();
        File file = new File(filePath);

        // If it's not a file or it's not readable, then hide the view option.
        boolean hideViewMenu = !file.isFile() || !file.canRead();

        // If the chosen file is not a .py or .testlist file, then hide the play option.
        boolean hideDeleteMenu = filePath.endsWith(PARENT);

        // If it's a file representing parent directory, then hide the delete option.
        boolean hidePlayMenu = !filePath.endsWith(".py") && !filePath.endsWith(".testlist") && !filePath.endsWith("testlist.txt");

        // If all menu items sare going to be hidden, don't show context menu at all.
        if (hideViewMenu && hideDeleteMenu && hidePlayMenu) {
            return;
        }

        // Inflate the context menu.
        getMenuInflater().inflate(R.menu.file_browser_long_press, menu);

        if (hideViewMenu) {
            MenuItem menuItemView = menu.findItem(R.id.menu_view);
            menuItemView.setVisible(false);
        }

        // TODO: Allow running different types of apython test specifiers, such as directories, etc.
        if (hidePlayMenu) {
            MenuItem menuItemView = menu.findItem(R.id.menu_play);
            menuItemView.setVisible(false);
        }

        if (hideDeleteMenu) {
            MenuItem menuItemDelete = menu.findItem(R.id.menu_delete);
            menuItemDelete.setVisible(false);
        }

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        // Get additional information of the item.
        AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        String path = info.targetView.getTag().toString();

        switch (item.getItemId()) {
            case R.id.menu_play:
                setScriptPath(info.targetView.getTag().toString());
                return true;
            case R.id.menu_view:
                showContent(path);
                return true;
            case R.id.menu_delete:

                deleteFilesRecursive(new File(path));

                // Refresh the current files list.
                try {
                    getDir(new File(path).getParentFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
        }

        return super.onContextItemSelected(item);
    }

    /**
     * Delete the specified File.  If it is a directory, then that directory with all of it's content will be deleted.
     *
     * @param fileDelete The File to be deleted.
     */
	private void deleteFilesRecursive(File fileDelete) {
        if (fileDelete.isDirectory()) {
            for (File child : fileDelete.listFiles()) {
                deleteFilesRecursive(child);
            }
        }
        fileDelete.delete();
	}

    /**
     * Show the content of the file at given path.
     *
     * @param path The path of the file whose content is to be shown.
     */
	private void showContent(String path) {
		Intent i = new Intent(this, FileWebViewActivity.class);
		i.putExtra("filePath", path);
		startActivity(i);
	}
}
