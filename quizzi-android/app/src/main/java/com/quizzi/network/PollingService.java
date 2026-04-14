package com.quizzi.network;

import android.os.Handler;
import android.os.Looper;

import com.quizzi.models.GameState;
import com.quizzi.utils.Constants;

public class PollingService {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private boolean isRunning = false;

    public interface StatusCallback {
        void onStatusUpdate(GameState state);
        void onError(String message);
    }

    public void startPolling(int sessionId, int playerId, StatusCallback callback) {
        isRunning = true;
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;
                new Thread(() -> {
                    try {
                        String json = ApiClient.get(
                                "/api/status?sessionId=" + sessionId + "&playerId=" + playerId);
                        GameState state = GameState.fromJson(json);
                        handler.post(() -> callback.onStatusUpdate(state));
                    } catch (Exception e) {
                        handler.post(() -> callback.onError(e.getMessage()));
                    }
                }).start();
                handler.postDelayed(this, Constants.POLL_INTERVAL_MS);
            }
        };
        handler.post(pollRunnable);
    }

    public void stopPolling() {
        isRunning = false;
        if (pollRunnable != null) {
            handler.removeCallbacks(pollRunnable);
        }
    }
}
