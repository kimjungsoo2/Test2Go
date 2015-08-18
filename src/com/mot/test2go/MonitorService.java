package com.mot.test2go;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This service is responsible to monitor the status of running test.  It will update the notification throughout the duration of test
 * run.  When test run completes, this service will update the notification one last time to link to the test report and send out an email.
 */
public class MonitorService extends Service {
    private final static String TAG = "Test2Go-" + MonitorService.class.getName();

    //private final static Long INTERVAL_TIME = 10000L; // interval time - 60 sec
	private final static String PROCESS_RUNNING = "1";
	private final static String PROCESS_STOP = "0";
	private static final String SEPARATOR = System.getProperty("line.separator");
	private static final String DELIMINATOR = Character.valueOf((char) 219).toString();

	public static final String SCRIPT_NAME = "script_to_run";
	public static final String EMAIL_RECIPIENT = "email_recipient";
	public static final String EMAIL_SENDER = "email_sender";
    public static final String REPORT_DIRECTORY = "report_directory";
    public static final String LOG_FILE = "log_file";
    public static final String TEST_URL = "test_url";
    public static final String SENDER_EMAIL_ACCOUNT = "scriptrunnertestdepot@gmail.com";
    public static final String SENDER_EMAIL_PASSWORD = "sunnyvaleTesterRobot";
    public String emailStatus = "";

	private String mScriptFile;
	private String[] mToEmail;
    private String testURL;

	NotificationManager nm;
	Timer timer = new Timer();
    private File logFile;
    private File apythonResultFile;
    private File reportDirectory;

    @Override
	public void onCreate() {
		super.onCreate();
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		//Toast.makeText(this, R.string.testing_start, Toast.LENGTH_SHORT).show();
		showNotification(R.drawable.android_ylw_24, getText(R.string.testing_start), null);
		checkPythonProcess(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

        Log.i(TAG, "Service is being started");

		mScriptFile = intent.getExtras().getString(SCRIPT_NAME);
		logFile = (File) intent.getExtras().get(LOG_FILE);
		reportDirectory = (File) intent.getExtras().get(REPORT_DIRECTORY);
        apythonResultFile = new File(reportDirectory, "index.html");
		mToEmail = intent.getExtras().getString(EMAIL_RECIPIENT).split(DELIMINATOR);
        if (intent.getExtras().containsKey(TEST_URL)) {
            testURL = intent.getExtras().getString(TEST_URL);
        }

		return START_REDELIVER_INTENT;
	}

	private void checkPythonProcess(final Context context) {

		// get the interval time set on preference screen
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String frequencyValue = prefs.getString(getString(R.string.pref_frequency_key), "60000");
		Long intervalTime = Long.parseLong(frequencyValue, 10);

		TimerTask task = new TimerTask() {
			@Override
			public void run() {

                String result = findProcess("python");

                if (result.equals(PROCESS_RUNNING)) {
                    showNotification(R.drawable.android_ylw_24, getText(R.string.testing_process), null);
                } else if (result.equals(PROCESS_STOP)) {
/*
                if (findChildProcess("python")) {
					showNotification(R.drawable.android_ylw_24, getText(R.string.testing_process), null);
                } else {
*/
					this.cancel(); // stop timer
                    sendEmail(SENDER_EMAIL_ACCOUNT, SENDER_EMAIL_PASSWORD, mToEmail);

                    // Upload the logs, if the test run being monitored is a it's a remote execution.
                    if (testURL != null) {
                        new AsyncTask<String, Void, String>() {
                            @Override
                            protected String doInBackground(String... params) {
                                String url = params[0];
                                String parameterName = "file";
                                String fileToUploadPath = params[1];
                                String responsePath = params[2];
                                // Attach the test log as a file parameter to the post request.  The filename is derived from the
                                // test URL because the unique ID used in the report file needs to match the unique ID in the test
                                // package URL.
                                String fileName = testURL.substring(testURL.lastIndexOf("/") + 1);
                                fileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".log";

                                Utility.doHttpPostWith1FileParameter(url, parameterName, fileToUploadPath, fileName, responsePath);

                                return null;
                            }
                        }.execute(Global.REMOTE_TEST_EXECUTION_RESULT_ADDRESS, apythonResultFile.getAbsolutePath(),
                                new File(getFilesDir(), "res.txt").getAbsolutePath());
                        // TODO: Derive the upload URL from the test url.
                    }

                    // TODO: Delete files that are extracted from remote test package.
                    // TODO: Delete the files Global.EXTRACTED_FILES_LIST_PATH and Global.POSSIBLE_TEST_LIST_PATH if they exist.

                    // Update notification to indicates that testing is completed and link the test report to the notification.
                    Intent intent = new Intent(MonitorService.this, FileWebViewActivity.class);
                    intent.putExtra("filePath", new File(reportDirectory, "index.html").getAbsolutePath());
                    showNotification(R.drawable.android_grn_24, getText(R.string.testing_end) + "\n" + emailStatus, intent);

                    // This test run is completed.  This service is no longer required.
                    stopSelf();
				}
			}

		};

		timer.scheduleAtFixedRate(task, 2000, intervalTime);
	}

    /**
     * Get the test device's information and return it as String.
     *
     * @return The device information as String.
     */
	@SuppressLint("NewApi")
	private String getDeviceInfo() {
		StringBuilder info = new StringBuilder();

		info.append("Device: ").append(Build.DEVICE);
		info.append(SEPARATOR);
		info.append("Product: ").append(Build.PRODUCT);
		info.append(SEPARATOR);
		info.append("Model: ").append(Build.MODEL);
		info.append(SEPARATOR);
		info.append("Serial Number: ").append(Build.SERIAL);
		info.append(SEPARATOR);
		info.append("Android Version: ").append(Build.VERSION.RELEASE);
		info.append(SEPARATOR);
		info.append("Build Number: ").append(Build.FINGERPRINT);

		return info.toString();
	}

    /**
     * Send out email as specified user to the list of users in mToEmail.
     *
     * @param user The user that will be used to send the email with.
     * @param pass The password for the user account that will be used to send the email with.
     * @param mToEmail The list of email addresses the email will be sent out to.
     */
	protected void sendEmail(String user, String pass, String[] mToEmail) {
		if (user != null && pass != null) {
			// get report summary
			String mBody = "";
			Mail m = new Mail(user, pass);

            // NOTE: Including the apython result file instead of the log file because log file could be incomplete due to Android system
            // killing Test2Go process due to low memory.
			mBody += "TEST REPORT by Test2Go" + SEPARATOR;
			mBody += "--------------------------------------------------------------------------" + SEPARATOR + SEPARATOR;
			mBody += getDeviceInfo() + SEPARATOR + SEPARATOR + SEPARATOR;
			mBody += readLogFileAndStripHTMLTags(apythonResultFile.getAbsolutePath()) + SEPARATOR + SEPARATOR + SEPARATOR;
			mBody += SEPARATOR + SEPARATOR;
			mBody += "______________________________________________________" + SEPARATOR;
			mBody += "This email is auto-generated by Test2Go.";

			m.setTo(mToEmail);
			m.setFrom(user);
			m.setSubject("TEST REPORT by Test2Go");
			m.setBody(mBody);

			try {
                // Attach log file.
                m.addAttachment(apythonResultFile.getAbsolutePath());

				if (!m.send()) {
					emailStatus = "Failed in sending Test Report Email";
				} else {
					emailStatus = "Test Report Email is sent successfully";
				}
			} catch (Exception e) {
				emailStatus = "Test Report Email is not sent due to exception " + e;
				Log.e("MonitorService", "Could not send email with user " + user + " and password " + pass, e);
			}
		}
	}

    /**
     * Read the content of specified log file, remove the HTML tags, and return it as String.
     *
     * @param path The location of the log file to be read.
     * @return The content of the log file with the HTML tags removed.
     */
	private String readLogFileAndStripHTMLTags(String path) {
		StringBuilder content = new StringBuilder();
		File f = new File(path);
		String line = "";

		if (f.isFile()) {
			try {
				@SuppressWarnings("resource")
				BufferedReader reader = new BufferedReader(new FileReader(f));

				while ((line = reader.readLine()) != null) {
					// rip off html tags
					line = line.replaceAll("(<a.*?>)", "");
					line = line.replaceAll("(</a>)", "");
					line = line.replaceAll("(<pre.*?>)", "");
					line = line.replaceAll("(</pre>)", "");

					content.append(line);
					content.append(SEPARATOR);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

//        return Html.fromHtml(content.toString()).toString();
        return content.toString();
	}

    /**
     * Find out the number of processes running processName.
     * WARNING: This method will search all running processes, not only the ones run from Test2Go.
     *
     * @param processName The name of the process to be counted.
     * @return The number of processes running processName.
     */
	public static String findProcess(String processName) {
		String result = "";

		try {
			Process p = Runtime.getRuntime().exec("sh");
			DataOutputStream os = new DataOutputStream(p.getOutputStream());
			InputStreamReader is = new InputStreamReader(p.getInputStream());
			BufferedReader reader = new BufferedReader(is);

			os.writeBytes("ps | grep " + processName + " | busybox wc -l\n");
			os.writeBytes("exit\n");
			os.flush();
			os.close();

			Log.d("MonitorService-findProcess", "ps | grep " + processName + " | busybox wc -l");

			result = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
            e.printStackTrace();
        }

		return result;
	}

    /**
     * Find out if the current package has a child process named processName or not.
     * WARNING: This method will search only processes run from Test2Go.
     *
     * @param processName The name of the child process to find.
     * @return true if the current package has a child process named processName.  Otherwise, returns false.
     */
	private boolean findChildProcess(String processName) {
        String packageName = getApplication().getPackageName();

        try {
            String test2goPID = Utility.runCommandAndGetResult("busybox pgrep " + packageName);
            String pstreeOutput = Utility.runCommandAndGetResult(String.format("busybox pstree -p %s", test2goPID));
            return pstreeOutput.contains(processName);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Show or update existing notification with new information.
     *
     * @param icon The icon for the notification.
     * @param message The message to be displayed in the notification.
     * @param intent The intent to be executed when the notification is pressed.  Defaults to null, which means,
     *               opens Test2Go's MainActivity.
     */
	private void showNotification(int icon, CharSequence message, Intent intent) {
        // TODO: Consider running the service in foreground so that it won't be easily killed.
        PendingIntent contentIntent;
        if (intent == null) {
            contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
        Notification.Builder nb = new Notification.Builder(this);
        Notification notification = nb.
                setContentTitle(getText(R.string.app_name)).
                setContentText(message).
                setSmallIcon(icon).
                setAutoCancel(true).
                setContentIntent(contentIntent).
                build();

		nm.notify(R.string.monitor_service_id, notification);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
