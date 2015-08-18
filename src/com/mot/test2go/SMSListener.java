package com.mot.test2go;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BroadcastReceiver to listen to incoming SMS.
 */
public class SMSListener extends BroadcastReceiver {
    private static final String TAG = "Test2Go-" + SMSListener.class.getName();
    public static final Pattern PATTERN_DEVICE_PING = Pattern.compile("Are you online-([^\\s]+)$");

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received SMS");
        Bundle bundle = intent.getExtras();

        SmsMessage[] msgs = null;
        String msg_from;
        if (bundle != null) {
            //---retrieve the SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];
            for (int i = 0; i < msgs.length; i++) {
                msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                msg_from = msgs[i].getOriginatingAddress();
                String msgBody = msgs[i].getMessageBody();

                Log.d(TAG, msg_from);
                Log.d(TAG, msgBody);

                // Check if the message is a request to check device status.
                Matcher matcher = PATTERN_DEVICE_PING.matcher(msgBody.trim());
                String identifier = null;
                if (matcher.matches()) {
                    identifier = matcher.group(1);
                    SmsManager sms = SmsManager.getDefault();
                    sms.sendTextMessage(msg_from, null, "I am online-" + identifier, null, null);
                    return;
                }

                // If the content of SMS follows a specific format, initiate the tests.
                String url = null;
                if (msgBody.startsWith("FRM:test88@motorola.com")) {
                    int index = msgBody.indexOf("MSG:");
                    if (index != -1) {
                        url = msgBody.substring(index + 4);
                    }
                } else if (msgBody.contains("http://144.189.164.130")) {
                    // TODO: Update the IP address for pingme server.
                    int index = msgBody.indexOf("http://");
                    if (index != -1) {
                        url = msgBody.substring(index);
                    }
                }
                if (url != null) {
                    Log.d(TAG, "Received SMS request to start remote test.");

                    // If an existing test is running, then don't start Test2Go main activity at all.
                    if (Integer.parseInt(MonitorService.findProcess("python")) > 0) {
                        Log.e(TAG, "Received a remote test run request when test device is running another test session.");
                        Toast.makeText(
                                context,
                                "There is existing test run.  Please wait for existing test run to complete before attempting to start a new one.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    Intent futureIntent = new Intent(context, MainActivity.class);
                    futureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Log.d(TAG, "URL: " + url);
                    futureIntent.putExtra("remoteTestPackageURL", url);
                    context.startActivity(futureIntent);
                }
            }
        }
    }
}
