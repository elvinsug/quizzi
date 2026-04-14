package com.quizzi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.quizzi.R;
import com.quizzi.models.GameState;
import com.quizzi.network.PollingService;

public class ScoreRevealActivity extends AppCompatActivity {

    private PollingService pollingService;
    private int sessionId, playerId;
    private String nickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score_reveal);

        sessionId = getIntent().getIntExtra("sessionId", 0);
        playerId = getIntent().getIntExtra("playerId", 0);
        nickname = getIntent().getStringExtra("nickname");
        boolean correct = getIntent().getBooleanExtra("correct", false);
        int pointsEarned = getIntent().getIntExtra("pointsEarned", 0);
        int totalScore = getIntent().getIntExtra("totalScore", 0);

        LinearLayout root = findViewById(R.id.rootLayout);
        TextView iconText = findViewById(R.id.iconText);
        TextView titleText = findViewById(R.id.titleText);
        TextView pointsText = findViewById(R.id.pointsText);
        TextView totalText = findViewById(R.id.totalText);
        TextView correctAnswerText = findViewById(R.id.correctAnswerText);

        if (correct) {
            root.setBackgroundResource(R.drawable.bg_correct);
            iconText.setText("\u2705");
            titleText.setText(R.string.correct);
        } else {
            root.setBackgroundResource(R.drawable.bg_wrong);
            iconText.setText("\u274C");
            titleText.setText(R.string.wrong);
        }

        pointsText.setText(String.format("+%,d", pointsEarned));
        totalText.setText(String.format("Total: %,d points", totalScore));

        // Animate scale-in
        root.setScaleX(0.8f);
        root.setScaleY(0.8f);
        root.setAlpha(0f);
        root.animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(500).setInterpolator(new OvershootInterpolator()).start();

        // Start polling for leaderboard state
        pollingService = new PollingService();
        pollingService.startPolling(sessionId, playerId, new PollingService.StatusCallback() {
            @Override
            public void onStatusUpdate(GameState state) {
                if ("showing_leaderboard".equals(state.status)) {
                    pollingService.stopPolling();
                    Intent intent = new Intent(ScoreRevealActivity.this, RankActivity.class);
                    intent.putExtra("sessionId", sessionId);
                    intent.putExtra("playerId", playerId);
                    intent.putExtra("nickname", nickname);
                    intent.putExtra("rank", state.playerRank);
                    intent.putExtra("totalPlayers", state.totalPlayers);
                    intent.putExtra("score", state.playerScore);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pollingService != null) pollingService.stopPolling();
    }
}
