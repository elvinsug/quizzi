package com.quizzi;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/api/results")
public class ResultsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        int sessionId = Integer.parseInt(req.getParameter("sessionId"));
        int playerId = Integer.parseInt(req.getParameter("playerId"));
        int questionId = Integer.parseInt(req.getParameter("questionId"));

        try (Connection conn = DBUtil.getConnection()) {
            // Get question correct answer
            PreparedStatement qStmt = conn.prepareStatement(
                "SELECT correct_answer FROM questions WHERE id=?");
            qStmt.setInt(1, questionId);
            ResultSet qRs = qStmt.executeQuery();
            String correctAnswer = "";
            if (qRs.next()) correctAnswer = qRs.getString("correct_answer");

            // Get player's response
            PreparedStatement rStmt = conn.prepareStatement(
                "SELECT choice, points_earned, response_time_ms FROM responses "
                + "WHERE game_session_id=? AND question_id=? AND player_id=?");
            rStmt.setInt(1, sessionId);
            rStmt.setInt(2, questionId);
            rStmt.setInt(3, playerId);
            ResultSet rRs = rStmt.executeQuery();

            if (!rRs.next()) {
                out.print("{\"status\":\"ok\",\"answered\":false,\"correctAnswer\":\"" + correctAnswer + "\"}");
                return;
            }

            String choice = rRs.getString("choice");
            int pointsEarned = rRs.getInt("points_earned");
            int responseTimeMs = rRs.getInt("response_time_ms");
            boolean correct = choice.equals(correctAnswer);

            // Get total score
            PreparedStatement tsStmt = conn.prepareStatement(
                "SELECT total_score FROM players WHERE id=?");
            tsStmt.setInt(1, playerId);
            ResultSet tsRs = tsStmt.executeQuery();
            int totalScore = 0;
            if (tsRs.next()) totalScore = tsRs.getInt("total_score");

            // Get rank
            PreparedStatement rkStmt = conn.prepareStatement(
                "SELECT COUNT(*) + 1 AS player_rank FROM players "
                + "WHERE game_session_id=? AND total_score > (SELECT total_score FROM players WHERE id=?)");
            rkStmt.setInt(1, sessionId);
            rkStmt.setInt(2, playerId);
            ResultSet rkRs = rkStmt.executeQuery();
            int rank = 1;
            if (rkRs.next()) rank = rkRs.getInt("player_rank");

            out.print("{\"status\":\"ok\",\"answered\":true"
                + ",\"choice\":\"" + choice + "\""
                + ",\"correct\":" + correct
                + ",\"correctAnswer\":\"" + correctAnswer + "\""
                + ",\"pointsEarned\":" + pointsEarned
                + ",\"responseTimeMs\":" + responseTimeMs
                + ",\"totalScore\":" + totalScore
                + ",\"rank\":" + rank + "}");

        } catch (Exception e) {
            out.print("{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }
}
