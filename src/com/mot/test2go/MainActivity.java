package com.mot.test2go;

import java.io.*;
import java.nio.channels.AsynchronousCloseException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class MainActivity extends Activity {

    private static final String TAG = "Test2Go-" + MainActivity.class.getName();

	private String mScriptPath;

	private static final int REQUEST_CODE = 1;
	public static final String SCRIPT_NAME = "script_to_run";
	private static final int COLOR_FILTER = 0xFF999999;
	//private static final String PYTHON_LOCATION = "/system/bin/python";

	private TextView textViewInstruction;
    private boolean installingPythonAndApython = false;
    private ProgressDialog progressDialog;

    @Override
    protected void onResume() {
        super.onResume();

        // Check if Test2Go is invoked with intent to start remote testing.
        String remoteTestPackageURL = getIntent().getStringExtra("remoteTestPackageURL");
        if (remoteTestPackageURL != null) {
            startTestingFromPackage(remoteTestPackageURL);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Check if Test2Go is invoked with intent to start remote testing.
        String remoteTestPackageURL = intent.getStringExtra("remoteTestPackageURL");
        if (remoteTestPackageURL != null) {
            startTestingFromPackage(remoteTestPackageURL);
        }
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Check if this is first run.  If it is, then send the device information to server.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean firstRun = prefs.getBoolean("firstRun", true);
        if (firstRun) {
            System.out.println("First run");
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("firstRun", false);
            editor.apply();

            DeviceInformationReporter deviceInformationReporter = new DeviceInformationReporter();
            deviceInformationReporter.onReceive(this, null);
        } else {
            System.out.println("Not first run");
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);

        textViewInstruction = (TextView) findViewById(R.id.textView_instruction);
		setButtonOnClickListener();
	}

	private void setButtonOnClickListener() {

		ImageView button = (ImageView) findViewById(R.id.imageView_start);
		button.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ImageView button = (ImageView) v;
                float x = event.getX();
                float y = event.getY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        button.setColorFilter(COLOR_FILTER, PorterDuff.Mode.MULTIPLY);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (0 <= x && x <= button.getWidth() && 0 <= y && y <= button.getHeight()) {
                            button.setColorFilter(COLOR_FILTER, PorterDuff.Mode.MULTIPLY);
                        } else {
                            button.clearColorFilter();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        button.clearColorFilter();
                        if (0 <= x && x <= button.getWidth() && 0 <= y && y <= button.getHeight()) {
                            startScript(mScriptPath, null);
                        }
                        break;
                }
                return true;
            }
        });
	}

	/*
	 * start a service to monitor the python process to indicate when testing is complete
	 */
	protected void startMonitorService(String script, File reportDirectory, File logFile, String recipientEmail, String url) {
		Intent service = new Intent(MainActivity.this, MonitorService.class);
		service.putExtra(MonitorService.SCRIPT_NAME, script);
        service.putExtra(MonitorService.REPORT_DIRECTORY, reportDirectory);
		service.putExtra(MonitorService.LOG_FILE, logFile);
		//service.putExtra(MonitorService.EMAIL_SENDER, senderEmail);
		service.putExtra(MonitorService.EMAIL_RECIPIENT, recipientEmail);
        if (url != null) {
            service.putExtra(MonitorService.TEST_URL, url);
        }
		startService(service);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		/*
		// Enable or disable Install menu item according to the existence of python interpreter
		if (runCommandThroughSh("busybox which python").equals(PYTHON_LOCATION)) {
			menu.getItem(0).setEnabled(false);
		} else {
			menu.getItem(0).setEnabled(true);
		}*/

		return true;
	}

    @Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_item_setting:
			Intent iSetting = new Intent(this, Preferences.class);
			startActivity(iSetting);
			return true;
		case R.id.menu_item_browse:
			Intent iBrowse = new Intent(this, FileBrowseActivity.class);
			startActivityForResult(iBrowse, REQUEST_CODE);
			return true;
//		case R.id.menu_item_test:
//            // TODO: Remove this menu.
//            startTestingFromPackage("https://sites.google.com/site/mymttest/tests.zip");
//            return true;
		case R.id.menu_item_install:
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(final Void... params) {
                    installPythonAndApython();
                    return null;
                }
            }.execute();
            return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

    private String checkSU2CommandStatus() {
        File su2File = new File("/system/xbin/su2");
        if (!su2File.exists()) {
            // Check if su2 is available or not.
            return "Cannot find su2 in /system/xbin. Make sure you copy it there.";
        }
        if (!su2File.canExecute()) {
            // Check if su2 is executable or not.
            return "/system/xbin/su2 is not executable. Make it executable with the following command:\n" +
                    "chmod 6755 /system/xbin/su2";
        }
        return null;
    }

    private void publishInstallationProgress(final ProgressDialog dialog, final String msg) {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        dialog.setMessage(msg);
                    }
                }
        );
    }

    private boolean needsToReinstallPythonAndApython() {
        // TODO: Handle the case where update is required.
        return !new File(Global.SCRIPT_ROOT).exists() || !new File(Global.PYTHON_ROOT).exists();
    }

    private boolean installPythonAndApython() {
        // Check if it is necessary to install python and apython or not.
        if (!needsToReinstallPythonAndApython()) {
            return true;
        }

        String su2CheckMessage = checkSU2CommandStatus();
        if (su2CheckMessage != null) {
            Toast.makeText(this, su2CheckMessage, Toast.LENGTH_LONG).show();
            return false;
        }

        progressDialog.setTitle("Installing python and apython");
        progressDialog.setMessage("Installation is in progress...");
        showProgressDialog();

        try {
            // However, if this is done, then we must make sure that the code can only be downloaded by Test2Go.
            // Alternatively, we can ask user to download the files manually from internal Motorola network,
            // then install them manually.

            // Download python.zip and extract its content.
            publishInstallationProgress(progressDialog, "Downloading python.zip...");
            downloadFile(Global.PYTHON_SERVER_ADDRESS, "python.zip");
            publishInstallationProgress(progressDialog, "Extracting python.zip...");
            /* NOTE: python.zip is being extracted to external storage first before moved to /data because writing to /data requires root
             permission. */
            extractPackage("python.zip", Global.EXTERNAL_STORAGE_PATH);
            System.out.println("Extracting python.zip");

            // Download apython.zip and extract its content.
            publishInstallationProgress(progressDialog, "Downloading apython.zip...");
            downloadFile(Global.APYTHON_SERVER_ADDRESS, "apython.zip");
            publishInstallationProgress(progressDialog, "Extracting apython.zip...");
            extractPackage("apython.zip", Global.EXTERNAL_STORAGE_PATH);
            System.out.println("Extracting apython.zip");

            // Copy python.sh to Global.EXTERNAL_STORAGE_PATH.  This file will be copied to /system/bin/python later with
            // su2 command because that operation requires elevated privilege.
            Utility.copyAssetFiles(getAssets(), "python.sh", Global.EXTERNAL_STORAGE_PATH);

            // Remount the system folder.
            final ArrayList<String> commands = new ArrayList<String>();
            commands.add("mount -o remount,rw /system");
            commands.add("cd " + Global.EXTERNAL_STORAGE_PATH);
            commands.add("rm -r " + Global.PYTHON_ROOT);
            commands.add("cp -r python " + Global.PYTHON_ROOT);
            commands.add("rm -r python");
            commands.add("cp python.sh /system/bin/python");
            commands.add("rm python.sh");
            commands.add("chmod 777 " + Global.PYTHON_ROOT + "/bin/python");
            commands.add("chmod 777 /system/bin/python");
            commands.add("cp __init__.py " + Global.PYTHON_ROOT + "/extras/python/ctypes/.");
            commands.add("rm __init__.py");
            commands.add(String.format("busybox dos2unix %s/android/apython.rc", Global.SCRIPT_ROOT));

            try {
                runSu2Command(commands);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (final IOException e) {
            runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            String msg = "Exception occurred during installation.  Msg: " + e.getMessage();
                            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
            e.printStackTrace();
        }

        dismissProgressDialog();

        return true;
    }

    private void dismissProgressDialog() {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                }
        );
    }

    private void showProgressDialog() {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.show();
                    }
                }
        );
    }

    /**
     * Download the content of the given HTTP url and store it at the given file name.  The file will be stored in this application's
     * local files directory.
     *
     * @param httpURL The HTTP URL whose content is to be downloaded.
     * @param localFileName The name of the file where the content of the HTTP URL will be stored locally.
     * @throws IOException If there is any IO operation error.
     */
    private void downloadFile(String httpURL, String localFileName) throws IOException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;
        try {
            response = httpclient.execute(new HttpGet(httpURL));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                FileOutputStream fos = openFileOutput(localFileName, MODE_PRIVATE);
                response.getEntity().writeTo(fos);
                fos.close();
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } finally {
            try {
                if (response != null) {
                    response.getEntity().consumeContent();
                }
            } catch (IOException e) {
                // Do nothing.
            }
        }
    }

    /**
     * Extract the content of zip file to the specified directory.
     *
     * @param zipFile The zip file to be extracted.
     * @param extractedDirectory The directory where the content of the zip file will be extracted to.
     * @throws IOException If there if problem with extracting the zip file.
     */
    private List<String> extractPackage(String zipFile, String extractedDirectory) throws IOException {
        File testPackageFile = new File(getFilesDir(), zipFile);
        byte[] buffer = new byte[8192];
        File expandedTestPackageDir = new File(extractedDirectory);

        // A list to collect all files in the extracted test package.
        List<String> fileList = new LinkedList<String>();

        // NOTE: The code to extract zip and tar files are very similar, but they cannot be shared because they don't share common
        // interface.
        if (zipFile.endsWith(".zip")) {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(testPackageFile));
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String name = zipEntry.getName();
                fileList.add(name);
                File expandedFilePath = new File(expandedTestPackageDir, name);
                if (zipEntry.isDirectory()) {
                    expandedFilePath.mkdirs();
                } else {
                    // This zip entry is a file.
                    long bytesLeft = zipEntry.getSize();
                    expandedFilePath.getParentFile().mkdirs();
                    FileOutputStream expandedFOS = new FileOutputStream(expandedFilePath);
                    while (bytesLeft > 0) {
                        int bytesRead = zipInputStream.read(buffer, 0, (int) Math.min(bytesLeft, buffer.length));
                        expandedFOS.write(buffer, 0, bytesRead);
                        bytesLeft = bytesLeft - bytesRead;
                    }
                    expandedFOS.close();
                }
            }
            zipInputStream.close();
        } else if (zipFile.endsWith(".tar")) {
            Log.d(TAG, String.format("busybox tar -x -f \"%s\" -C \"%s\"", testPackageFile.getAbsolutePath(), extractedDirectory));
            Utility.runCommandThroughSh(String.format("busybox tar -x -f \"%s\" -C \"%s\"", testPackageFile.getAbsolutePath(),
                    extractedDirectory));
            Log.d(TAG, String.format("busybox tar -t -f \"%s\"", testPackageFile.getAbsolutePath()));
            String extractedFilesText = Utility.runCommandThroughSh(String.format("busybox tar -t -f \"%s\"",
                    testPackageFile.getAbsolutePath()));
            Collections.addAll(fileList, extractedFilesText.split("\n"));
        } else if (zipFile.endsWith(".tar.gz")) {
            Log.d(TAG, String.format("busybox tar -x -z -f \"%s\" -C \"%s\"", testPackageFile.getAbsolutePath(), extractedDirectory));
            Utility.runCommandThroughSh(String.format("busybox tar -x -z -f \"%s\" -C \"%s\"", testPackageFile.getAbsolutePath(),
                    extractedDirectory));
            Log.d(TAG, String.format("busybox tar -t -f \"%s\"", testPackageFile.getAbsolutePath()));
            String extractedFilesText = Utility.runCommandThroughSh(String.format("busybox tar -t -f \"%s\"",
                    testPackageFile.getAbsolutePath()));
            Collections.addAll(fileList, extractedFilesText.split("\n"));
        }
        return fileList;
    }

    private void startTestingFromPackage(String url) {
        // Start an AsyncTask to initiate testing.
        new AsyncTask<String, String, String>() {

            private String localFileName;
            private String url;

            @Override
            protected String doInBackground(String... params) {
                installPythonAndApython();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.setTitle("Running remote test request");
                    }
                });
                publishProgress("Running remote test request.");
                showProgressDialog();

                url = params[0];
                localFileName = "tests.zip";
                if (url.endsWith(".tar")) {
                    localFileName = "tests.tar";
                } else if (url.endsWith(".gz")) {
                    localFileName = "tests.tar.gz";
                }

                // Download test package from remote site.
                publishProgress("Downloading test package from " + url);
                try {
                    downloadFile(url, localFileName);
                } catch (IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, String.format("Cannot download test package \"%s\"", url),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e(TAG, String.format("Cannot download test package \"%s\"", url));
                    e.printStackTrace();
                    return e.getMessage();
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                progressDialog.setMessage("Downloading \"" + values[0] + "\"...");
            }

            @Override
            protected void onPostExecute(String s) {
                if (s != null) {
                    return;
                }

                // Extract the downloaded test package.
                publishProgress("Extracting test package...");
                try {
                    List<String> extractedFilesList = extractPackage(localFileName, Global.SCRIPT_ROOT);

                    // Write the list of extracted files and extracted python files.
                    File extractedListFile = new File(Global.EXTRACTED_FILES_LIST_PATH);
                    File possibleTestListFile = new File(Global.POSSIBLE_TEST_LIST_PATH);
                    BufferedOutputStream extractedListBOS = new BufferedOutputStream(new FileOutputStream(extractedListFile));
                    BufferedOutputStream possibleTestListBOS = new BufferedOutputStream(new FileOutputStream(possibleTestListFile));
                    for (String extractedFile : extractedFilesList) {
                        byte[] extractedFileAsBytes = (extractedFile + "\n").getBytes();
                        if (extractedFile.toLowerCase().endsWith(".py")) {
                            possibleTestListBOS.write(extractedFileAsBytes);
                        }
                        extractedListBOS.write(extractedFileAsBytes);
                    }
                    possibleTestListBOS.close();
                    extractedListBOS.close();
                    Log.d(TAG, "Finished extracting test package");
                } catch (IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Cannot extract the downloaded test package.  Cancelling test run",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.d(TAG, "Cannot extract the downloaded test package.  Cancelling test run");
                    e.printStackTrace();
                    return;
                }

                try {
                    // NOTE: Without this sleep, first test run may fail.  Maybe it is needed to let the device settles down after prep.
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // Do nothing.
                }

                // Start running tests.
                publishProgress("Running tests...");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO: Run the test list that is included in the test package.
//                        startScript(Global.POSSIBLE_TEST_LIST_PATH, url);
                        startScript(new File(new File(Global.SCRIPT_ROOT, "common"),
                                "testlist.txt").getAbsolutePath(), url);
//                        startScript(new File(new File(Global.SCRIPT_ROOT, "tests"), "001.testlist").getAbsolutePath());
                    }
                });

                dismissProgressDialog();
            }
        }.execute(url);
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (requestCode == REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				mScriptPath = intent.getStringExtra(SCRIPT_NAME);
				textViewInstruction.setText(mScriptPath);
			}
		}
	}

    /**
     * Start running specified tests.
     `*
     * @param scriptPath The test specifiers to run.  You can currently specify a test file or a .testlist file that contains list of
     *                   tests to run.
     */
	public void startScript(String scriptPath, String url) {
        // TODO: Automatically install python and apython as needed.
//        installPythonAndApython();

        // TODO: Add the capability to run every test specifiers that apython allows.
        /*
        Tests are run with the Python-based nose testing framework using the included runtests.py script. To run a test or set of tests:
        $ python runtests.py [ test-specifier ]

        where test-specifier may be:
            A directory: python runtests.py tests/samples/simple
            A file: python runtests.py tests/samples/simple/Test001.py
            A test class in a file: python runtests.py tests/samples/simple/Test001.py:test001
            A method of a test class in a file: python runtests.py tests/samples/simple/Test001.py:test001.test001004
            By name: python runtests.py "ECG_Andr.Morr.Blur.Apps.Acceptance Test:001-004"
            By a list of tests in a file: python runtests.py --run testlist

            The --run option accepts a comma-separated list of files, so you may run multiple lists of tests as follows:
                python runtests.py --run firstlist,secondlist,thirdlist
         */

        // First, retrieve the email settings.
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//String senderEmail = prefs.getString(getString(R.string.pref_sender_email_accounts_key), null);
		String recipientEmail = prefs.getString(getString(R.string.pref_recipient_email_address_key), "");
		recipientEmail = recipientEmail.trim();

        // TODO: UI to register phone for remote test execution, if this is going to be made available from the phone.
        if (Integer.parseInt(MonitorService.findProcess("python")) > 0) {
            Toast.makeText(MainActivity.this, "There is existing test run.  Please wait for existing test run to complete before " +
                    "attempting to start a new one.", Toast.LENGTH_SHORT).show();
        } else if (recipientEmail.length() <= 0) {
            Toast.makeText(MainActivity.this, "Please add email address to receive test report", Toast.LENGTH_SHORT).show();
        } else {
            ArrayList<String> commands = new ArrayList<String>();
            File logfile;

            if (scriptPath != null && (scriptPath.endsWith(".py") || scriptPath.endsWith(".testlist") || scriptPath.endsWith("testlist.txt"))) {
                String dateTimeIdentifier = getCurrentDateAndTimeAsString();

                // Determine the report directory.
                File reportDirectory = new File(Global.REPORTS_DIRECTORY, String.format("report.%s", dateTimeIdentifier));
                String file = scriptPath.substring(scriptPath.lastIndexOf("/") + 1);

                // Determine the console log location.
                File _localReportsDirectory = new File(Global.REPORTS_DIRECTORY);
                if (!_localReportsDirectory.exists()) {
                    _localReportsDirectory.mkdirs();
                }
                logfile = new File(Global.REPORTS_DIRECTORY, String.format("log.%s.log", dateTimeIdentifier));

                // Start building list of commands to execute tests.
                // Go to the directory where apython is located in.
                commands.add("cd " + Global.SCRIPT_ROOT);

                // Export LD_LIBRARY_PATH.
                commands.add("export LD_LIBRARY_PATH=" + Utility.runCommandThroughSh("echo $LD_LIBRARY_PATH"));

                // Export PYTHONPATH.
                commands.add("export PYTHONPATH=" + getPYTHONPATH(scriptPath));

                // Generate the test mapping.
                commands.add(String.format("python runtests.py --generate-map --collect-only --report-dir %s",
                        Global.GENERATE_MAP_REPORT_DIRECTORY));

                // Run the tests.
                if (file.endsWith(".py")) {
                    commands.add(String.format("python runtests.py %s --report-dir %s > %s",
                            scriptPath, reportDirectory, logfile.getAbsolutePath()));
                } else if (file.endsWith(".testlist") || file.endsWith("testlist.txt")) {
                    commands.add(String.format("python runtests.py --run %s --report-dir %s > %s",
                            scriptPath, reportDirectory, logfile.getAbsolutePath()));
                } else {
                    Toast.makeText(MainActivity.this, "Unrecognized file", Toast.LENGTH_SHORT);
                }

                try {
                    // Run the list of commands.
                    runSu2Command(commands);

                    // Move to home screen so that script can start from right place.
                    Intent i = new Intent(Intent.ACTION_MAIN);
                    i.addCategory(Intent.CATEGORY_HOME);
                    startActivity(i);

                    // Exit the application with releasing process from the system.
                    finish();

                    // Start execution monitoring service.
                    startMonitorService(file, reportDirectory, logfile, recipientEmail, url);

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Failed in executing command", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Please select script first!", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private String getCurrentDateAndTimeAsString() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int date = calendar.get(Calendar.DATE);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millisecond = calendar.get(Calendar.MILLISECOND);
        return String.format("%04d%02d%02d_%02d%02d%02d_%03d", year, month, date, hour, minute, second, millisecond);
    }

    private String getPYTHONPATH(String targetFile) {
		String pythonpath = "";
		int pos;

		targetFile = targetFile.replace(Global.SCRIPT_ROOT + "/", "");

		// get the path of script to run
		pos = targetFile.lastIndexOf("/");

		if (pos > -1) {
			pythonpath+= "." + targetFile.substring(0, pos);
		}

		// get path that contains library
		File dir = new File(Global.SCRIPT_ROOT);
		File[] flist = dir.listFiles();

		for (File f : flist) {
			if (f.isDirectory()) {
				if (!f.getName().equals("android") && !f.getName().equals("nose") && !f.getName().equals("extras")) {
					// check if the folder contains __init__.py file
					String[] filelist = f.list();

					for (String fname : filelist) {
						if (fname.equals("__init__.py")) {
							pythonpath += ":./" + f.getName();
							break;
						}
					}
				}
			}
		}

		return pythonpath;
	}

    private void runSu2Command(ArrayList<String> commandList) throws IOException {
		String[] commands = new String[commandList.size()];
		commandList.toArray(commands);

		Process p = Runtime.getRuntime().exec("su2");
		DataOutputStream os = new DataOutputStream(p.getOutputStream());
        InputStream errorStream = p.getErrorStream();
        InputStream inputStream = p.getInputStream();

		for (String cmd : commands) {
			Log.d("MainActivity-runSu2Command", "runSu2Command() cmd=" + cmd);
			os.writeBytes(cmd + "\n");
		}

        os.writeBytes("exit\n");
		os.flush();
		os.close();
        inputStream.close();
        errorStream.close();
	}
}
