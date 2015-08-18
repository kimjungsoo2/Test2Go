package com.mot.test2go;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.*;

public class FileWebViewActivity extends Activity {

    private WebView fileReadWebView;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_web_view);

		// Show the Up button in the action bar.
		setupActionBar();

        // Set the title for this activity.
        String filePath = getIntent().getStringExtra("filePath");
        setTitle(filePath);

        // Configure the web view.
        fileReadWebView = (WebView) findViewById(R.id.webView_fileRead);
        fileReadWebView.getSettings().setBuiltInZoomControls(true);
        fileReadWebView.getSettings().setDisplayZoomControls(false);

        if (filePath.toLowerCase().endsWith(".html") || filePath.toLowerCase().endsWith(".xhtml")) {
            // The file to be opened is an HTML file that WebView can load without any pre-processing, so just load it as is.
            fileReadWebView.getSettings().setJavaScriptEnabled(false);
            fileReadWebView.loadUrl("file://" + new File(filePath).getAbsoluteFile());
        } else {
            // Enabled javascript so google-code-prettify library to highlight code syntax can be run.
            fileReadWebView.getSettings().setJavaScriptEnabled(true);

            // Copy the google-code-prettify files to application's files directory, in case it's needed.
            File GOOGLE_PRETTIFY_DIRECTORY = new File(getFilesDir(), "google-code-prettify");
            if (!GOOGLE_PRETTIFY_DIRECTORY.exists()) {
                GOOGLE_PRETTIFY_DIRECTORY.mkdirs();
                Utility.copyAssetFiles(getAssets(), "google-code-prettify", GOOGLE_PRETTIFY_DIRECTORY.getAbsolutePath());
            }

            // Retrieve the file content and encode it to HTTP friendly format.
            String fileContent;
            try {
                fileContent = readFile(filePath);
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String encodedFileContent = Html.escapeHtml(fileContent);

            // Encase the encoded file content in a bare minimum HTML with <pre> that google-code-prettify can work with.
            String html = String.format("<html>" +
                    "    <head>" +
                    "        <link href=\"prettify.css\" type=\"text/css\" rel=\"stylesheet\" />" +
                    "        <script type=\"text/javascript\" src=\"prettify.js\"></script>" +
                    "    </head>" +
                    "    <body style=\"font-size:10;\" onload=\"prettyPrint()\">" +
                    "        <pre class=\"prettyprint\">%s</pre>" +
                    "    </body>" +
                    "</html>",
                    encodedFileContent
            );
            String baseURL = "file://" + GOOGLE_PRETTIFY_DIRECTORY.getAbsolutePath() + "/";
            fileReadWebView.loadDataWithBaseURL(baseURL, html, "text/html", "utf-8", null);
        }
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
	public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Just press back button when the up key is pressed.
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
	}

    /**
     * Read and return the content of specified file.
     *
     * @param path The path of the file to be read.
     * @return The content of specified file.
     */
    private String readFile(String path) throws Exception {
        File f = new File(path);

        long fileSize = f.length();
        if (fileSize > 1048576) {
            throw new Exception(String.format("Cannot open file \"%s\" because it's too big.", path));
        }
        byte[] bytes = new byte[(int) fileSize];

        if (f.isFile()) {
            try {
                FileInputStream fis = new FileInputStream(f);
                fis.read(bytes);
                fis.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new String(bytes);
    }
}
