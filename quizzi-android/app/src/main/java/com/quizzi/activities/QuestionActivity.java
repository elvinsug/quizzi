package com.quizzi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.quizzi.R;
import com.quizzi.models.GameState;
import com.quizzi.network.ApiClient;
import com.quizzi.network.PollingService;

import org.json.JSONObject;

public class QuestionActivity extends AppCompatActivity {

    private int sessionId, playerId;
    private String nickname;
    private PollingService pollingService;
    private CountDownTimer countDownTimer;
    private boolean hasAnswered = false;
    private int currentQuestionId;
    private long questionStartedAt;

    private TextView timerText, questionText, questionNumber;
    private MaterialButton btnA, btnB, btnC, btnD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);

        sessionId = getIntent().getIntExtra("sessionId", 0);
        playerId = getIntent().getIntExtra("playerId", 0);
        nickname = getIntent().getStringExtra("nickname");

        timerText = findViewById(R.id.timerText);
        questionText = findViewById(R.id.questionText);
        questionNumber = findViewById(R.id.questionNumber);
        btnA = findViewById(R.id.btnA);
        btnB = findViewById(R.id.btnB);
        btnC = findViewById(R.id.btnC);
        btnD = findViewById(R.id.btnD);

        btnA.setOnClickListener(v -> submitAnswer("a"));
        btnB.setOnClickListener(v -> submitAnswer("b"));
        btnC.setOnClickListener(v -> submitAnswer("c"));
        btnD.setOnClickListener(v -> submitAnswer("d"));

        pollingService = new PollingService();
        pollingService.startPolling(sessionId, playerId, new PollingService.StatusCallback() {
            @Override
            public void onStatusUpdate(GameState state) {
                if ("showing_question".equals(state.status)) {
                    showQuestion(state);
                } else if ("showing_results".equals(state.status)) {
                    pollingService.stopPolling();
                    goToScoreReveal(state);
                }
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void showQuestion(GameState state) {
        if (state.questionId == currentQuestionId) return;
        currentQuestionId = state.questionId;
        questionStartedAt = state.questionStartedAt;
        hasAnswered = false;

        questionNumber.setText(String.format("Q%d of %d", state.currentQuestionOrder, state.totalQuestions));
        questionText.setText(state.questionText);
        btnA.setText("\u25B2  " + state.optionA);
        btnB.setText("\u25C6  " + state.optionB);

        if (state.optionC != null && !state.optionC.isEmpty()) {
            btnC.setText("\u25CF  " + state.optionC);
            btnC.setVisibility(View.VISIBLE);
        } else {
            btnC.setVisibility(View.GONE);
        }

        if (state.optionD != null && !state.optionD.isEmpty()) {
            btnD.setText("\u25A0  " + state.optionD);
            btnD.setVisibility(View.VISIBLE);
        } else {
            btnD.setVisibility(View.GONE);
        }

        enableButtons(true);
        startCountdown(state.timeLimit, state.questionStartedAt);
    }

    private void startCountdown(int timeLimitSec, long startedAt) {
        if (countDownTimer != null) countDownTimer.cancel();

        long elapsed = System.currentTimeMillis() - startedAt;
        long remaining = (timeLimitSec * 1000L) - elapsed;
        if (remaining <= 0) remaining = 0;

        countDownTimer = new CountDownTimer(remaining, 250) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secs = (int) Math.ceil(millisUntilFinished / 1000.0);
                timerText.setText(String.valueOf(secs));
                if (secs <= 5) {
                    timerText.setTextColor(getResources().getColor(R.color.wrong_red, null));
                } else if (secs <= 10) {
                    timerText.setTextColor(getResources().getColor(R.color.answer_c, null));
                } else {
                    timerText.setTextColor(getResources().getColor(R.color.text_primary, null));
                }
            }

            @Override
            public void onFinish() {
                timerText.setText("0");
            }
        };
        countDownTimer.start();
    }

    private void submitAnswer(String choice) {
        if (hasAnswered) return;
        hasAnswered = true;
        enableButtons(false);

        long responseTime = System.currentTimeMillis() - questionStartedAt;

        new Thread(() -> {
            try {
                ApiClient.get("/select?sessionId=" + sessionId
                        + "&questionId=" + currentQuestionId
                        + "&playerId=" + playerId
                        + "&choice=" + choice
                        + "&time=" + responseTime);
            } catch (Exception e) {
                // Will handle in next poll
            }
        }).start();

        // Navigate to waiting screen
        Intent intent = new Intent(this, WaitingResultActivity.class);
        intent.putExtra("sessionId", sessionId);
        intent.putExtra("playerId", playerId);
        intent.putExtra("nickname", nickname);
        startActivity(intent);
        finish();
    }

    private void goToScoreReveal(GameState state) {
        if (countDownTimer != null) countDownTimer.cancel();
        Intent intent = new Intent(this, ScoreRevealActivity.class);
        intent.putExtra("sessionId", sessionId);
        intent.putExtra("playerId", playerId);
        intent.putExtra("nickname", nickname);
        intent.putExtra("correct", state.playerCorrect);
        intent.putExtra("pointsEarned", state.playerPointsEarned);
        intent.putExtra("totalScore", state.playerTotalScore);
        intent.putExtra("correctAnswer", state.correctAnswer);
        startActivity(intent);
        finish();
    }

    private void enableButtons(boolean enabled) {
        btnA.setEnabled(enabled);
        btnB.setEnabled(enabled);
        btnC.setEnabled(enabled);
        btnD.setEnabled(enabled);
        float alpha = enabled ? 1.0f : 0.5f;
        btnA.setAlpha(alpha);
        btnB.setAlpha(alpha);
        btnC.setAlpha(alpha);
        btnD.setAlpha(alpha);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pollingService != null) pollingService.stopPolling();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
