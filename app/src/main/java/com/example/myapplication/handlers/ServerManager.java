package com.example.myapplication.handlers;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ServerManager {

    static HttpURLConnection conn;
    static String ServerURL = "http://10.99.221.227:8889/";

    public static boolean SendJsonToServer(String message, String method, String root) {
        {
            try {
                URL url = new URL(ServerURL+root);
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod(method);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(message.getBytes().length);

                conn.setRequestProperty("Content-Type", "main_activity/json;charset=utf-8");
                conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");

                conn.connect();

                OutputStream os = new BufferedOutputStream(conn.getOutputStream());
                os.write(message.getBytes());

                os.flush();
                os.close();

                String statusCode = String.valueOf(conn.getResponseCode());
                Log.i("STATUS", statusCode);
                return true;
            } catch (IOException e){
                Log.e("REQUEST_ERR", e.toString());
                return false;
            }
        }
    }

    public static String GetStringErr() {
        String response = "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            for (String line; (line = br.readLine()) != null; response += line);
        } catch (IOException|NullPointerException e) {
            Log.e("ERR_READINGBUFF", e.toString());
            return response;
        }
        return response;
    }

    public static String GetStringResponse() {
        String response = "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            for (String line; (line = br.readLine()) != null; response += line);
        } catch (IOException|NullPointerException e) {
            Log.e("RESP_READINGBUFF", e.toString());
            return response;
        }
        return response;
    }
}