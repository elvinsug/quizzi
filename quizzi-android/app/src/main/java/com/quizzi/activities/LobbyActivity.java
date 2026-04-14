package com.quizzi.activities;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.quizzi.R;
import com.quizzi.models.GameState;
import com.quizzi.network.PollingService;

public class LobbyActivity extends AppCompatActivity {

    private PollingService pollingService;
    private int sessionId, playerId;
    private String nickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        sessionId = getIntent().getIntExtra("sessionId", 0);
        playerId = getIntent().getIntExtra("playerId", 0);
        nickname = getIntent().getStringExtra("nickname");

        TextView nicknameText = findViewById(R.id.nicknameText);
        nicknameText.setText("Welcome, " + nickname + " \uD83C\uDF89");

        startDotAnimation();

        pollingService = new PollingService();
        pollingService.startPolling(sessionId, playerId, new PollingService.StatusCallback() {
            @Override
            public void onStatusUpdate(GameState state) {
                TextView countText = findViewById(R.id.playerCountText);
                countText.setText(state.playerCount + " player(s) in the lobby");

                if ("showing_question".equals(state.status)) {
                    pollingService.stopPolling();
                    navigateToQuestion(state);
                }
            }

            @Override
            public void onError(String message) {
                // Silently retry — polling will continue
            }
        });
    }

    private void startDotAnimation() {
        View[] dots = { findViewById(R.id.dot1), findViewById(R.id.dot2), findViewById(R.id.dot3) };
        for (int i = 0; i < dots.length; i++) {
            ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(dots[i],
                    PropertyValuesHolder.ofFloat("scaleX", 0.6f, 1.0f, 0.6f),
                    PropertyValuesHolder.ofFloat("scaleY", 0.6f, 1.0f, 0.6f),
                    PropertyValuesHolder.ofFloat("alpha", 0.4f, 1.0f, 0.4f));
            anim.setDuration(1400);
            anim.setRepeatCount(ObjectAnimator.INFINITE);
            anim.setStartDelay(i * 160L);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            anim.start();
        }
    }

    private void navigateToQuestion(GameState state) {
        Intent intent = new Intent(this, QuestionActivity.class);
        intent.putExtra("sessionId", sessionId);
        intent.putExtra("playerId", playerId);
        intent.putExtra("nickname", nickname);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pollingService != null) pollingService.stopPolling();
    }
}
