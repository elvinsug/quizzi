package com.quizzi.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.quizzi.R;

public class GameOverActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);

        String nickname = getIntent().getStringExtra("nickname");
        int rank = getIntent().getIntExtra("rank", 0);
        int score = getIntent().getIntExtra("score", 0);

        TextView rankText = findViewById(R.id.finalRankText);
        TextView scoreText = findViewById(R.id.finalScoreText);
        TextView messageText = findViewById(R.id.messageText);
        MaterialButton playAgainBtn = findViewById(R.id.playAgainBtn);

        rankText.setText("#" + rank);
        scoreText.setText(String.format("%,d points", score));
        messageText.setText("Great game, " + nickname + "!");

        playAgainBtn.setOnClickListener(v -> {
            Intent intent = new Intent(GameOverActivity.this, JoinGameActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
