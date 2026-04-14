package com.quizzi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.quizzi.R;
import com.quizzi.models.GameState;
import com.quizzi.network.PollingService;

public class RankActivity extends AppCompatActivity {

    private PollingService pollingService;
    private int sessionId, playerId;
    private String nickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rank);

        sessionId = getIntent().getIntExtra("sessionId", 0);
        playerId = getIntent().getIntExtra("playerId", 0);
        nickname = getIntent().getStringExtra("nickname");
        int rank = getIntent().getIntExtra("rank", 0);
        int totalPlayers = getIntent().getIntExtra("totalPlayers", 0);
        int score = getIntent().getIntExtra("score", 0);

        TextView rankText = findViewById(R.id.rankText);
        TextView outOfText = findViewById(R.id.outOfText);
        TextView scoreText = findViewById(R.id.scoreText);
        TextView encourageText = findViewById(R.id.encourageText);

        rankText.setText("#" + rank);
        outOfText.setText("out of " + totalPlayers + " players");
        scoreText.setText(String.format("%,d points", score));

        if (rank == 1) encourageText.setText("You're the champion! \uD83C\uDFC6");
        else if (rank <= 3) encourageText.setText("Amazing work, " + nickname + "! \uD83C\uDF1F");
        else encourageText.setText("Keep it up, " + nickname + "!");

        pollingService = new PollingService();
        pollingService.startPolling(sessionId, playerId, new PollingService.StatusCallback() {
            @Override
            public void onStatusUpdate(GameState state) {
                if ("showing_question".equals(state.status)) {
                    pollingService.stopPolling();
                    Intent intent = new Intent(RankActivity.this, QuestionActivity.class);
                    intent.putExtra("sessionId", sessionId);
                    intent.putExtra("playerId", playerId);
                    intent.putExtra("nickname", nickname);
                    startActivity(intent);
                    finish();
                } else if ("finished".equals(state.status)) {
                    pollingService.stopPolling();
                    Intent intent = new Intent(RankActivity.this, GameOverActivity.class);
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
