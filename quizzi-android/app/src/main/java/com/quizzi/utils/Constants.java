package com.quizzi.utils;

public class Constants {
    // For Android emulator → host machine, use 10.0.2.2
    // For physical device on same WiFi, use the machine's IP address
    public static final String BASE_URL = "http://10.0.2.2:8080/quizzi";
    public static final long POLL_INTERVAL_MS = 1500;
    public static final int CONNECT_TIMEOUT_MS = 5000;
    public static final int READ_TIMEOUT_MS = 5000;
}
