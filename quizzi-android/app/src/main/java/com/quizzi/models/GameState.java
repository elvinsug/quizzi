package com.quizzi.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    public String status;
    public String quizTitle;
    public int currentQuestionOrder;
    public int totalQuestions;
    public int playerCount;
    public long questionStartedAt;

    // Question fields
    public int questionId;
    public String questionText;
    public String optionA;
    public String optionB;
    public String optionC;
    public String optionD;
    public int timeLimit;
    public int answerCount;

    // Results fields
    public String correctAnswer;
    public int countA, countB, countC, countD;
    public String playerChoice;
    public boolean playerCorrect;
    public int playerPointsEarned;
    public int playerTotalScore;

    // Leaderboard fields
    public List<LeaderboardEntry> leaderboard;
    public int playerRank;
    public int playerScore;
    public int totalPlayers;

    // Lobby fields
    public List<String> players;

    public static GameState fromJson(String json) {
        GameState state = new GameState();
        try {
            JSONObject obj = new JSONObject(json);
            state.status = obj.optString("status", "");
            state.quizTitle = obj.optString("quizTitle", "");
            state.currentQuestionOrder = obj.optInt("currentQuestionOrder", 0);
            state.totalQuestions = obj.optInt("totalQuestions", 0);
            state.playerCount = obj.optInt("playerCount", 0);
            state.questionStartedAt = obj.optLong("questionStartedAt", 0);

            state.questionId = obj.optInt("questionId", 0);
            state.questionText = obj.optString("questionText", "");
            state.optionA = obj.optString("optionA", "");
            state.optionB = obj.optString("optionB", "");
            state.optionC = obj.optString("optionC", "");
            state.optionD = obj.optString("optionD", "");
            state.timeLimit = obj.optInt("timeLimit", 20);
            state.answerCount = obj.optInt("answerCount", 0);

            state.correctAnswer = obj.optString("correctAnswer", "");
            state.countA = obj.optInt("countA", 0);
            state.countB = obj.optInt("countB", 0);
            state.countC = obj.optInt("countC", 0);
            state.countD = obj.optInt("countD", 0);
            state.playerChoice = obj.optString("playerChoice", "");
            state.playerCorrect = obj.optBoolean("playerCorrect", false);
            state.playerPointsEarned = obj.optInt("playerPointsEarned", 0);
            state.playerTotalScore = obj.optInt("playerTotalScore", 0);

            state.playerRank = obj.optInt("playerRank", 0);
            state.playerScore = obj.optInt("playerScore", 0);
            state.totalPlayers = obj.optInt("totalPlayers", 0);

            if (obj.has("players")) {
                state.players = new ArrayList<>();
                JSONArray arr = obj.getJSONArray("players");
                for (int i = 0; i < arr.length(); i++) {
                    state.players.add(arr.getString(i));
                }
            }

            if (obj.has("leaderboard")) {
                state.leaderboard = new ArrayList<>();
                JSONArray arr = obj.getJSONArray("leaderboard");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject e = arr.getJSONObject(i);
                    state.leaderboard.add(new LeaderboardEntry(
                            e.getInt("rank"),
                            e.getString("nickname"),
                            e.getInt("score")
                    ));
                }
            }

        } catch (Exception e) {
            state.status = "error";
        }
        return state;
    }

    public static class LeaderboardEntry {
        public int rank;
        public String nickname;
        public int score;

        public LeaderboardEntry(int rank, String nickname, int score) {
            this.rank = rank;
            this.nickname = nickname;
            this.score = score;
        }
    }
}
