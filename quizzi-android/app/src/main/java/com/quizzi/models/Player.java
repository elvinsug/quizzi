package com.quizzi.models;

public class Player {
    public int playerId;
    public int sessionId;
    public String nickname;
    public String quizTitle;

    public Player(int playerId, int sessionId, String nickname, String quizTitle) {
        this.playerId = playerId;
        this.sessionId = sessionId;
        this.nickname = nickname;
        this.quizTitle = quizTitle;
    }
}
