package com.mot.test2go;

import android.content.Intent;
import android.os.Environment;
import java.io.File;

/**
 * Some shared global parameters.
 */
public class Global {
    /**
     * The path to the external storage path.
     */
    public static final String EXTERNAL_STORAGE_PATH = Environment.getExternalStorageDirectory().getPath().replace("/0", "/legacy");
    /**
     * The location where python will be copied to.
     */
    public static final String PYTHON_ROOT = "/data/python";
    /**
     * The location where apython will be copied to.  Also test scripts should go in here.
     */
    public static final String SCRIPT_ROOT = new File(EXTERNAL_STORAGE_PATH, "apython").getAbsolutePath();
    /**
     * The location where the test reports will be saved in.
     * NOTE: The Global.REPORTS_DIRECTORY is not a File because it can cause resource lock when deleted,
     * requiring device to be rebooted.
     */
    public static final String REPORTS_DIRECTORY = new File(SCRIPT_ROOT, "reports").getAbsolutePath();
    /**
     * The location where the report for generating test maps will be saved in.
     * NOTE: The Global.GENERATE_MAP_REPORT_DIRECTORY is not a File because it can cause resource lock when deleted,
     * requiring device to be rebooted.
     */
    public static final String GENERATE_MAP_REPORT_DIRECTORY = new File(SCRIPT_ROOT, "generateMapReport").getAbsolutePath();

    /**
     * This file contains the list of files extracted from a remote test package.
     */
    public static final String EXTRACTED_FILES_LIST_PATH = new File(Global.SCRIPT_ROOT, "extractedFiles.txt").getAbsolutePath();
    /**
     * This file contains the list of possible test case files extracted from a remote test package.
     */
    public static final String POSSIBLE_TEST_LIST_PATH = new File(Global.SCRIPT_ROOT, "possibleTestListFiles.testlist").getAbsolutePath();

    /**
     * The web address to upload device information file to.
     * TODO: Update this with the right address.
     */
    public static final String DEVICE_SERVER_ADDRESS = "http://144.189.164.130/bin/upload_info_file.php";

    /**
     * The web address to upload remote test execution test result file to.
     */
    public static final String REMOTE_TEST_EXECUTION_RESULT_ADDRESS = "http://144.189.164.130/bin/upload_test_results.php";

    /**
     * The address to get the python package.
     */
    public static final String PYTHON_SERVER_ADDRESS = "http://144.189.164.130/frmwks/python.zip";

    /**
     * The address to get the apython package.
     */
    public static final String APYTHON_SERVER_ADDRESS = "http://144.189.164.130/frmwks/apython.zip";
}
