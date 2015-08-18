package com.mot.test2go;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CustomArrayAdapter extends ArrayAdapter<File> {

	private final Context mContext;
    private final ArrayList<File> files;

	public CustomArrayAdapter(Context context, ArrayList<File> mFiles) {
		super(context, R.layout.activity_file_browse_row, mFiles);
        this.files = mFiles;
		this.mContext = context;
	}

    @Override
	public View getView(int position, View convertView, ViewGroup parent) {

		LayoutInflater inflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflator.inflate(R.layout.activity_file_browse_row, parent, false);
		TextView textView = (TextView) rowView.findViewById(R.id.textView_FileView2_row);

        File file = files.get(position);

		textView.setText(file.getName());

		// tag for each item, which will be used to set the path of scripts to run in FileBrowserActivity
		textView.setTag(file.getAbsolutePath());

		// set icon according to file extension
		if (file.isDirectory()) {
			textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.orange_folder_24, 0, 0, 0);
		} else {
			if (file.getName().endsWith(".py")) {
				textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.text_x_python_24, 0, 0, 0);
			} else {
				textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.gnome_text_x_generic_24, 0, 0, 0);
			}
		}

		return rowView;
	}

}
