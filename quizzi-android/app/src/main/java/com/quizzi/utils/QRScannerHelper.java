package com.quizzi.utils;

import android.app.Activity;
import android.content.Intent;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import androidx.activity.result.ActivityResultLauncher;

public class QRScannerHelper {

    public static ScanOptions createScanOptions() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan the QR code on the host screen");
        options.setCameraId(0);
        options.setBeepEnabled(false);
        options.setOrientationLocked(true);
        return options;
    }

    /**
     * Extract the PIN from a scanned QR URL.
     * Expected format: http://host:port/quizzi/play/?pin=123456
     */
    public static String extractPinFromUrl(String url) {
        if (url == null) return null;
        // Try URL parameter
        int pinIdx = url.indexOf("pin=");
        if (pinIdx >= 0) {
            String pinStr = url.substring(pinIdx + 4);
            int end = pinStr.indexOf('&');
            if (end > 0) pinStr = pinStr.substring(0, end);
            if (pinStr.matches("\\d{6}")) return pinStr;
        }
        // If the QR just contains digits
        if (url.trim().matches("\\d{6}")) return url.trim();
        return null;
    }
}
