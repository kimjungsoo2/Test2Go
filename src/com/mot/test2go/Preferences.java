package com.mot.test2go;

import java.util.Map;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.EditText;

// TODO: Review this class.
public class Preferences extends PreferenceActivity {

	private static final String DELIMINATOR = Character.valueOf((char) 219).toString();
	private static final String SEPARATOR = System.getProperty("line.separator");

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		//ListPreference prefEmail = (ListPreference) findPreference(getString(R.string.pref_sender_email_accounts_key));
		ListPreference prefIntervalTime = (ListPreference) findPreference(getString(R.string.pref_frequency_key));
		Preference prefToEmail = findPreference(getString(R.string.pref_recipient_email_address_key));

		// set listener
		prefIntervalTime.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				setValueForKey(preference.getKey(), (String) newValue);
				updateSummary(preference.getKey());
				return false;
			}

		});

		/*
		prefEmail.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object value) {
				setEntryValue(preference.getKey(), (String) value);
				return false;
			}

		});
		*/

		prefToEmail.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent i = new Intent(Preferences.this, SetupEmailActivity.class);
				startActivity(i);
				return false;
			}

		});

		//populateEmailEntries();

        setupActionBar();
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

	protected void setEntryValue(final String key, final String value) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Verify Password");
		builder.setMessage("Enter password to verify the email account");

		final EditText input = new EditText(this);
		builder.setView(input);

		builder.setPositiveButton("OK", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int response) {

				String emailAccount;
				int index = value.indexOf(DELIMINATOR);

				if (index > -1) {
					emailAccount = value.substring(0, index);
				} else {
					emailAccount = value;
				}

				setValueForKey(key, emailAccount + DELIMINATOR + input.getEditableText().toString());
				//populateEmailEntries(); // update email account entries
				updateSummary(key);

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
	}

	private void setValueForKey(String key, String newValue) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString(key, newValue);
		editor.commit();
	}

	/*
	@SuppressWarnings("deprecation")
	private void populateEmailEntries() {

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		ListPreference prefEmail = (ListPreference) findPreference(getString(R.string.pref_sender_email_accounts_key));
		Account[] accounts = AccountManager.get(this).getAccountsByType("com.google");

		int total = accounts.length;
		String prefValue = sharedPref.getString(getString(R.string.pref_sender_email_accounts_key), null);

		if (total > 0) {
			CharSequence[] entries = new String[total];
			CharSequence[] values = new String[total];

			for (int i = 0; i < total; i++) {
				entries[i] = accounts[i].name;

				if (prefValue != null && prefValue.contains(accounts[i].name)) {
					values[i] = prefValue;
				} else {
					values[i] = accounts[i].name;
				}
			}

			prefEmail.setEntries(entries);
			prefEmail.setEntryValues(values);
		} else {
			prefEmail.setSummary("Please setup email account first to enable this option.");
			prefEmail.setEnabled(false);
		}
	}
	*/

	private void initSummary() {
		Map<String, ?> keys = PreferenceManager.getDefaultSharedPreferences(this).getAll();

		for (Map.Entry<String, ?> entry : keys.entrySet()) {
			Log.d("initSummary", entry.getKey() + ": " + entry.getValue().toString());
			updateSummary(entry.getKey());
		}
	}

	@SuppressWarnings("deprecation")
	private void updateSummary(String key) {

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String value = sharedPref.getString(key, null);
		CharSequence selEntry = null;
		Preference p = findPreference(key);

		if (p != null) {
			if (p instanceof ListPreference) {
				ListPreference listPref = (ListPreference) p;

				int index = listPref.findIndexOfValue(value);

				if (index > -1) {
					listPref.setValueIndex(index);
					selEntry = listPref.getEntry();
				}

				if (selEntry == null) {
					selEntry = listPref.getSummary();
				}

				p.setSummary(selEntry);
			} else {
				if (value.contains(DELIMINATOR)) {
					value = value.replaceAll(DELIMINATOR, SEPARATOR);
				}

				p.setSummary(value);
			}
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		initSummary();
	}

}
