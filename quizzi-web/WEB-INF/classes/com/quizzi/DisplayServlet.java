package com.quizzi;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/display")
public class DisplayServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        int sessionId = Integer.parseInt(req.getParameter("sessionId"));
        int questionId = Integer.parseInt(req.getParameter("questionId"));

        try (Connection conn = DBUtil.getConnection()) {

            // Get question details
            PreparedStatement qStmt = conn.prepareStatement(
                "SELECT question_text, option_a, option_b, option_c, option_d, correct_answer FROM questions WHERE id=?");
            qStmt.setInt(1, questionId);
            ResultSet qRs = qStmt.executeQuery();

            if (!qRs.next()) {
                out.print("{\"status\":\"error\",\"message\":\"Question not found\"}");
                return;
            }

            String questionText = qRs.getString("question_text");
            String optA = qRs.getString("option_a");
            String optB = qRs.getString("option_b");
            String optC = qRs.getString("option_c");
            String optD = qRs.getString("option_d");
            String correctAnswer = qRs.getString("correct_answer");

            // Count responses per choice
            PreparedStatement countStmt = conn.prepareStatement(
                "SELECT choice, COUNT(*) AS count FROM responses WHERE question_id=? AND game_session_id=? GROUP BY choice");
            countStmt.setInt(1, questionId);
            countStmt.setInt(2, sessionId);
            ResultSet cRs = countStmt.executeQuery();

            int countA = 0, countB = 0, countC = 0, countD = 0;
            while (cRs.next()) {
                String ch = cRs.getString("choice");
                int cnt = cRs.getInt("count");
                switch (ch) {
                    case "a": countA = cnt; break;
                    case "b": countB = cnt; break;
                    case "c": countC = cnt; break;
                    case "d": countD = cnt; break;
                }
            }

            int total = countA + countB + countC + countD;
            int correctCount = 0;
            switch (correctAnswer) {
                case "a": correctCount = countA; break;
                case "b": correctCount = countB; break;
                case "c": correctCount = countC; break;
                case "d": correctCount = countD; break;
            }

            out.print("{\"questionText\":\"" + escapeJson(questionText) + "\""
                + ",\"optionA\":\"" + escapeJson(optA) + "\""
                + ",\"optionB\":\"" + escapeJson(optB) + "\""
                + ",\"optionC\":\"" + escapeJson(optC != null ? optC : "") + "\""
                + ",\"optionD\":\"" + escapeJson(optD != null ? optD : "") + "\""
                + ",\"correctAnswer\":\"" + correctAnswer + "\""
                + ",\"countA\":" + countA
                + ",\"countB\":" + countB
                + ",\"countC\":" + countC
                + ",\"countD\":" + countD
                + ",\"totalResponses\":" + total
                + ",\"correctCount\":" + correctCount
                + ",\"correctPercent\":" + (total > 0 ? Math.round(correctCount * 100.0 / total) : 0)
                + "}");

        } catch (Exception e) {
            out.print("{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
