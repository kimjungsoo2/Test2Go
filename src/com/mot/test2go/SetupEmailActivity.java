package com.mot.test2go;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.text.InputType;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

public class SetupEmailActivity extends ListActivity implements OnItemClickListener {

	private static final String DELIMINATOR = Character.valueOf((char) 219).toString();

	ArrayAdapter<String> mAdapter;
	ArrayList<String> emailList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_email);
        // Show the Up button in the action bar.
        setupActionBar();

        // set on item click listener
        getListView().setOnItemClickListener(this);

        String value = getPrefValue(getString(R.string.pref_recipient_email_address_key)).trim();

        if (value.length() > 0) {
            String[] values = value.split(DELIMINATOR);

            Collections.addAll(emailList, values);
        }

        // bind the email ArrayList to the list view
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, emailList);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        setListAdapter(mAdapter);

    }

    private String getPrefValue(String key) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString(key, "");
	}

	private void putPrefValue(String key, String value) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(key, value);
		editor.commit();
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (getListView().getCheckedItemCount() > 0) {
            menu.findItem(R.id.item_delete).setVisible(true);
            menu.findItem(R.id.item_add).setVisible(false);
        } else {
            menu.findItem(R.id.item_delete).setVisible(false);
            menu.findItem(R.id.item_add).setVisible(true);
        }
//        getActionBar().
        return true;
//        return super.onPrepareOptionsMenu(menu);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.setup_email_list, menu);
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
            case R.id.item_delete:

                // get checked items
                String[] checkedItems = getCheckedItems(getListView().getCheckedItemPositions());
                for (String checkedItem : checkedItems) {
                    mAdapter.remove(checkedItem);
                    emailList.remove(checkedItem);
                }
                getListView().clearChoices();
                invalidateOptionsMenu();

                // save the email list to preference and update list view
                updateList(emailList);

                return true;
            case R.id.item_add:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Enter Email Address");
                builder.setMessage("Enter recipient's email address");

                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                builder.setView(input);

                builder.setPositiveButton("OK", new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int response) {
                        String emailAddress = input.getEditableText().toString().trim();

                        // Do a quick validity check on the specified email address and double check to see if it's already in the list
                        // or not.
                        String msg = null;
                        if (emailAddress.length() < 7 || !emailAddress.contains("@")) {
                            msg = "The email address \"" + emailAddress + "\" is invalid.";
                        } else if (emailList.contains(emailAddress)) {
                            msg = "The email address \"" + emailAddress + "\" is in the list already.";
                        }
                        if (msg != null) {
                            Toast.makeText(
                                    getBaseContext(),
                                    msg,
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        // Add newly specified email address.
                        emailList.add(input.getEditableText().toString());

                        // Save the email list to preference and update list view
                        updateList(emailList);
                    }

                });

                builder.setNegativeButton("Cancel", new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }

                });

                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
        }

        // TODO: Test2go with uiautomator
        // Support running uiautomator test.
        // Read the uiautomator test result.
        // No need for dependency.

        return super.onOptionsItemSelected(item);
	}

	private String[] getCheckedItems(SparseBooleanArray sbArray) {
		String[] array = new String[sbArray.size()];

		for (int i = 0; i < sbArray.size(); i++) {
			// get the position of checked item through bounded key
			int position = sbArray.keyAt(i);

			// add to array if the item is checked
			boolean checked = sbArray.get(position);

			if (checked) {
				array[i] = mAdapter.getItem(position);
			}
		}

		return array;
	}

	private void updateList(ArrayList<String> list) {

		StringBuilder items = new StringBuilder();

		// Save the list to preference key.
		for (Iterator<?> i = list.iterator(); i.hasNext();) {
			items.append(i.next());

			if (i.hasNext()) {
				items.append(DELIMINATOR);
			}
		}

		putPrefValue(getString(R.string.pref_recipient_email_address_key), items.toString());

		// Update listView.
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
        invalidateOptionsMenu();
	}

}
