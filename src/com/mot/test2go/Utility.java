package com.mot.test2go;

import android.content.res.AssetManager;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;

/**
 * This class provides some utility functions.
 */
public class Utility {
    private final static String TAG = "Test2Go-" + Utility.class.getName();

    /**
     * Copy the asset at assetPath inside assetManager to destinationPath.  If the asset is a directory,
     * then the content of all that directory asset will be copied.
     *
     * @param assetManager The AssetManager that contains the asset to be copied.
     * @param assetPath The path of the asset to be copied.
     * @param destinationPath The path where the asset will be copied to.
     */
    public static void copyAssetFiles(AssetManager assetManager, String assetPath, String destinationPath) {
        InputStream source;
        OutputStream destination;

        try {
            source = assetManager.open(assetPath);

            File destinationFile = new File(destinationPath);
            if (!destinationFile.isDirectory()) {
                // If the specified destination is not a directory and doesn't exist, ensure the parent directory exists.
                if (!destinationFile.exists()) {
                    // Create parent folder if it does not exist.
                    File parent = destinationFile.getCanonicalFile().getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                }
            } else {
                if (assetPath.contains("/")) {
                    destinationFile = new File(destinationFile, assetPath.substring(assetPath.lastIndexOf("/") + 1));
                } else {
                    destinationFile = new File(destinationFile, assetPath);
                }
            }

            // This asset is a file, call copyFile to copy the file.
            destination = new FileOutputStream(destinationFile);

            copyFile(source, destination);

            source.close();
            destination.close();
        } catch(FileNotFoundException e) {
            // This asset is a directory, call copyAssetFiles with it.
            String[] list;
            try {
                list = assetManager.list(assetPath);
                for (String fileName : list) {
                    String subAssetPath = assetPath + "/" + fileName;
                    String subDestinationPath = new File(destinationPath, fileName).getAbsolutePath();
                    copyAssetFiles(assetManager, subAssetPath, subDestinationPath);
                }
            } catch (IOException e1) {
                // Do nothing.
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Copy the content from given input stream to output stream.
     *
     * @param in The InputStream where the content needs to copied from.
     * @param out The OutputStream where the content will be copied to.
     * @throws IOException
     */
    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;

        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    /**
     * Run a command that does not require any input, then return the output as String.  To avoid using up too much memory,
     * make sure that the command does not produce a lot of data.
     *
     * @param cmd The command to run.
     * @return The output of the executed command.
     * @throws java.io.IOException If there is problem doing any file-related operation.
     */
    static String runCommandAndGetResult(String cmd) throws IOException {
        Process p = Runtime.getRuntime().exec(cmd);
        InputStream is = p.getInputStream();

        // Read the output.
        StringBuilder sb = new StringBuilder();
        byte[] bytes = new byte[1024];
        int bytesRead = is.read(bytes);
        while (bytesRead != -1) {
            sb.append(new String(bytes, 0, bytesRead));
            bytesRead = is.read(bytes);
        }

        // Close the streams.
        is.close();
        p.getOutputStream().close();
        p.getErrorStream().close();

        return sb.toString();
    }

    /**
     * Run a command that does not require any input through shell, then return the output as String.  To avoid using up too much memory,
     * make sure that the command does not produce a lot of data.
     *
     * @param command The command to run through shell.
     * @return The output of the command executed through shell.
     */
    static String runCommandThroughSh(String command) {
        try {
            Process p = Runtime.getRuntime().exec("sh");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            InputStream is = p.getInputStream();

            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            os.close();

            Log.d(TAG, "runCommandThroughSh() cmd=" + command);

            // Read the output.
            StringBuilder sb = new StringBuilder();
            byte[] bytes = new byte[1024];
            int bytesRead = is.read(bytes);
            while (bytesRead != -1) {
                sb.append(new String(bytes, 0, bytesRead));
                bytesRead = is.read(bytes);
            }

            // Close the remaining streams.
            is.close();
            p.getErrorStream().close();

            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Make an HTTP post request to url that attaches a file type parameter named parameterName.  The content of the parameter will be
     * read from fileToUploadPath and the file parameter will have file name fileToUploadName.  If the HTTP post request is successfully
     * handled by the server, then the response will be saved in responsePath.
     *
     * @param url The URL where the HTTP post will be made out to.
     * @param parameterName The parameter name in the HTTP post request.
     * @param fileToUploadPath The path to the local file that will be uploaded.
     * @param fileToUploadName The file name of the uploaded file.  The reason the file name is not automatically derived from the
     *                         fileToUploadPath is because sometime the actual file to be uploaded may be saved under different name,
     *                         so rather than renaming the file to different name, upload it, and then rename it back,
     *                         this field should be used for that purpose.  If the file name should be as is, set this parameter to null.
     * @param responsePath The path to the
     */
    static void doHttpPostWith1FileParameter(final String url, final String parameterName, final String fileToUploadPath,
                                             final String fileToUploadName, final String responsePath) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost(url);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            if (fileToUploadName == null) {
                // Add a file parameter under name parameterName from a local file at fileToUploadPath.  The file name for this parameter
                // will be automatically derived from the local file path.
                builder.addBinaryBody(parameterName, new File(fileToUploadPath));
            } else {
                // Add a file parameter under name parameterName from a local file at fileToUploadPath with file name fileToUploadName.
                final File fileToUpload = new File(fileToUploadPath);
                InputStreamBody is = new InputStreamBody(new FileInputStream(fileToUpload), ContentType.TEXT_PLAIN,
                        fileToUploadName) {
                    @Override
                    public long getContentLength() {
                        return fileToUpload.length();
                    }
                };
                builder.addPart(parameterName, is);
            }

            // Build and execute the HTTP post request.
            httpPost.setEntity(builder.build());
            response = httpclient.execute(httpPost);

            // Check for the response status and write down the response if the request was successfully handled.
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                FileOutputStream fos = new FileOutputStream(responsePath);
                response.getEntity().writeTo(fos);
                fos.close();
            } else{
                // Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (IOException e) {
            // Note down the exception.
            Log.e(TAG, "Got exception when sending HTTP post request with file parameter..");
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
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
}
