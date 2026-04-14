package com.quizzi.models;

public class Question {
    public int id;
    public String questionText;
    public String optionA;
    public String optionB;
    public String optionC;
    public String optionD;
    public int timeLimit;
    public int questionOrder;

    public Question(int id, String questionText, String optionA, String optionB,
                    String optionC, String optionD, int timeLimit, int questionOrder) {
        this.id = id;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.timeLimit = timeLimit;
        this.questionOrder = questionOrder;
    }
}
