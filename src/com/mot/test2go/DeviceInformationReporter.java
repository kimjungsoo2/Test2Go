package com.mot.test2go;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class is responsible to generate device information file and report it to PingMe server.
 */
public class DeviceInformationReporter extends BroadcastReceiver {
    private static final String TAG = "Test2Go-" + DeviceInformationReporter.class.getName();
    Context context;
    private SimpleDateFormat timestampFormatter = new SimpleDateFormat("_yyyyMMddHHmmss");

    private boolean isDeviceMultiSimEnabled() {
        try {
            Class.forName("android.telephony.MSimTelephonyManager");
            android.telephony.MSimTelephonyManager tm = android.telephony.MSimTelephonyManager.getDefault();
            return tm.isMultiSimEnabled();
        } catch (ClassNotFoundException e) {
            // Cannot find MSimTelephonyManager class.  That means this device is not Motorola multi-SIM enabled device.
        }

        return false;
    }

    private String getDevicePhoneNumber() {
        if (isDeviceMultiSimEnabled()) {
            String number = getDevicePhoneNumber(0);
            if (number != null && number.trim().length() != 0) {
                return number;
            }
            number = getDevicePhoneNumber(1);
            return number;
        }

        // This device does not have multi-SIM feature.
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getLine1Number();
    }

    private String getDevicePhoneNumber(int sub) {
        if (isDeviceMultiSimEnabled()) {
            android.telephony.MSimTelephonyManager tm = android.telephony.MSimTelephonyManager.getDefault();
            return tm.getLine1Number(sub);
        }

        // This device does not have multi-SIM feature.
        return "";
    }

    private void reportDeviceInformation() {
        AsyncTask<Void, Void, Void> at = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                // Get the device's phone number.  If the device supports multiple SIM cards, then get the first SIM's phone number.
                String phoneNumber = getDevicePhoneNumber();

                // Get the device's serial ID.
                String deviceID = Build.SERIAL;

                // Get the device's software version.
                String softwareVersion = Build.DISPLAY;

                // Get the device's product ID.
                String product = Build.PRODUCT;

                // Get the device's timezone.
                String timezone = TimeZone.getDefault().getDisplayName(TimeZone.getDefault().inDaylightTime(new Date()), TimeZone.SHORT);

                // Get the device's current country location with geo-coder.
                String country = "NA";
                LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                Location lastKnownLocation = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                Geocoder geocoder = new Geocoder(context);
                try {
                    List<Address> addresses = geocoder.getFromLocation(
                            lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), 1
                    );
                    for (Address address : addresses) {
                        country = address.getCountryCode();
                    }
                } catch (IOException e) {
                    // Do nothing.
                }

                // Prepare the device information text to be uploaded.
                String textToUpload = "";
                textToUpload += "Country: " + country + "\n";
                textToUpload += "Phone number: " + phoneNumber + "\n";
                textToUpload += "Device ID: " + deviceID + "\n";
                textToUpload += "Software: " + softwareVersion + "\n";
                textToUpload += "Product: " + product + "\n";
                textToUpload += "Timezone: " + timezone + "\n";
                FileOutputStream fos;
                String deviceInformationFilePath = new File(context.getFilesDir(), "deviceInformation.txt").getAbsolutePath();
                try {
                    fos = new FileOutputStream(deviceInformationFilePath);
                    fos.write(textToUpload.getBytes());
                    fos.close();
                } catch (IOException e) {
                    String msg = "Cannot generate device information.";
                    Log.e(TAG, msg);
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    return null;
                }

                // Upload the device information file to the server.
                String parameterName = "logs";
                String responsePath = new File(context.getFilesDir(), "deviceInformationUpload.log").getAbsolutePath();
                Utility.doHttpPostWith1FileParameter(
                        Global.DEVICE_SERVER_ADDRESS,
                        parameterName,
                        deviceInformationFilePath,
                        deviceID + timestampFormatter.format(new Date()),
                        responsePath
                );

                // Remove the device information file.
                File deviceInformationFile = new File(deviceInformationFilePath);
                if (deviceInformationFile.exists()) {
                    deviceInformationFile.delete();
                }

                return null;
            }
        };
        at.execute();
    }

    public void onReceive(Context context, Intent intent) {
        this.context = context;
        reportDeviceInformation();
    }
}
