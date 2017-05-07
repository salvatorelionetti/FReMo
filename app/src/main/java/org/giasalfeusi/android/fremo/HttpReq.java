package org.giasalfeusi.android.fremo;

import android.util.Log;

import org.giasalfeusi.android.blen.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Created by salvy on 31/03/17.
 */

public class HttpReq {
    final static private String TAG = "HttpReq";
    final static int TIMEOUT_VALUE = 4000;

    public String notifyMeasure(List<String> measures, String api_key)
    {
        String ret = null;

        // TODO Don't log API!!!
        Log.d(TAG, String.format("notifyMeasure(%s)", measures));

        try
        {
            URL url = null;
            String response = null;
            //String parameters = "measures[]=" + TextUtils.join("&measures[]=,", measures);
            //String parameters = "measures=["+TextUtils.join(",", measures)+"]";
            String parameters;
            float T_object;
            HttpURLConnection connection = null;
            OutputStreamWriter request;

            String measure = measures.get(measures.size()-1);
            Log.i(TAG, String.format("measure(%s)", measure));
            byte[] T_bytes = Utils.hexStringToByteArray(measure.substring(0,8));
            float T_obj = ((T_bytes[1]<<8) + T_bytes[0])/128.0f;
            float T_amb = ((T_bytes[3]<<8) + T_bytes[2])/128.0f;

            parameters = String.format("key='%s',field1='%s'", api_key, T_obj);
            try {
                String urlString = String.format("https://api.thingspeak.com/update?api_key=%s&field1=%f&field2=%f", api_key, T_obj, T_amb);
                Log.i(TAG, String.format("create the url(%s)", urlString));
                //url = new URL("https://giasalfeusi.ns0.it/turin_ac/got_measure.py");
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            //create the connection
            try {
                Log.i(TAG, "open the connection");
                connection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            connection.setConnectTimeout(TIMEOUT_VALUE);
            connection.setReadTimeout(TIMEOUT_VALUE);

            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                ret = stringBuilder.toString();

                Log.d(TAG, String.format("ret(%s)", ret));
            }
            finally
            {
                connection.disconnect();
            }

        } catch (IOException e) {
            Log.e("HTTP POST:", e.toString());
        }

        return ret;
    }

    public String notifyMeasurePost(List<String> measures) {
        String ret = null;

        Log.d(TAG, String.format("notifyMeasure(%s)", measures));

        try {
            URL url = null;
            String response = null;
            //String parameters = "measures[]=" + TextUtils.join("&measures[]=,", measures);
            //String parameters = "measures=["+TextUtils.join(",", measures)+"]";
            String parameters;
            String api_key = "RWHKOZVDLK7G5B6P";
            float T_object;
            HttpURLConnection connection = null;
            OutputStreamWriter request;

            String measure = measures.get(measures.size()-1);
            Log.i(TAG, String.format("measure(%s)", measure));
            byte[] T_obj_bytes = Utils.hexStringToByteArray(measure.substring(0,4));
            float T_obj = ((T_obj_bytes[1]<<8) + T_obj_bytes[0])/128.0f;
            parameters = String.format("key='%s',field1='%s'", api_key, T_obj);
            try {
                Log.i(TAG, String.format("create the url, params(%s)", parameters));
                //url = new URL("https://giasalfeusi.ns0.it/turin_ac/got_measure.py");
                url = new URL("https://api.thingspeak.com/update");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            //create the connection
            try {
                Log.i(TAG, "open the connection");
                connection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            connection.setConnectTimeout(TIMEOUT_VALUE);
            connection.setReadTimeout(TIMEOUT_VALUE);
//            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            //set the request method to GET
            //connection.setRequestMethod("POST");
            connection.setRequestMethod("POST");
            //get the output stream from the connection you created
            request = new OutputStreamWriter(connection.getOutputStream());
            //write your data to the ouputstream
            Log.d(TAG, "new OutStream done");
            request.write(parameters);
            Log.d(TAG, "write done");
            request.flush();
            Log.d(TAG, "flush done");
            request.close();
            Log.d(TAG, "close done");
            String line = "";
            if (1==0) {
                //create your inputsream
                InputStreamReader isr = new InputStreamReader(
                        connection.getInputStream());
                //read in the data from input stream, this can be done a variety of ways
                BufferedReader reader = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                //get the string version of the response data
                response = sb.toString();
                ret = response;
                //do what you want with the data now
                Log.d(TAG, String.format("notifyMeasure got response(%s)", response));
                //always remember to close your input and output streams
                isr.close();
                reader.close();
            }

        } catch (IOException e) {
            Log.e("HTTP POST:", e.toString());
        }

        return ret;
    }
}