package com.quizzi.network;

import com.quizzi.utils.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiClient {

    public static String get(String endpoint) throws IOException {
        URL url = new URL(Constants.BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(Constants.CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(Constants.READ_TIMEOUT_MS);

        try {
            return readResponse(conn);
        } finally {
            conn.disconnect();
        }
    }

    public static String post(String endpoint) throws IOException {
        URL url = new URL(Constants.BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(Constants.CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(Constants.READ_TIMEOUT_MS);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.flush();
        }

        try {
            return readResponse(conn);
        } finally {
            conn.disconnect();
        }
    }

    private static String readResponse(HttpURLConnection conn) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}
