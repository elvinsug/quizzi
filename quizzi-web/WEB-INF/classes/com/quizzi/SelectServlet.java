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

@WebServlet("/select")
public class SelectServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        int sessionId = Integer.parseInt(req.getParameter("sessionId"));
        int questionId = Integer.parseInt(req.getParameter("questionId"));
        int playerId = Integer.parseInt(req.getParameter("playerId"));
        String choice = req.getParameter("choice").toLowerCase();
        int responseTimeMs = Integer.parseInt(req.getParameter("time"));

        try (Connection conn = DBUtil.getConnection()) {

            // Check for duplicate submission
            PreparedStatement dupCheck = conn.prepareStatement(
                "SELECT id FROM responses WHERE game_session_id=? AND question_id=? AND player_id=?");
            dupCheck.setInt(1, sessionId);
            dupCheck.setInt(2, questionId);
            dupCheck.setInt(3, playerId);
            ResultSet dupRs = dupCheck.executeQuery();
            if (dupRs.next()) {
                out.print("{\"status\":\"error\",\"message\":\"Already answered\"}");
                return;
            }

            // Look up the question details
            PreparedStatement qStmt = conn.prepareStatement(
                "SELECT correct_answer, time_limit_seconds, points_possible FROM questions WHERE id=?");
            qStmt.setInt(1, questionId);
            ResultSet qRs = qStmt.executeQuery();
            if (!qRs.next()) {
                out.print("{\"status\":\"error\",\"message\":\"Question not found\"}");
                return;
            }

            String correctAnswer = qRs.getString("correct_answer");
            int timeLimitSeconds = qRs.getInt("time_limit_seconds");
            int pointsPossible = qRs.getInt("points_possible");

            boolean isCorrect = choice.equals(correctAnswer);
            int pointsEarned = 0;
            if (isCorrect) {
                double ratio = (double) responseTimeMs / (timeLimitSeconds * 1000.0);
                pointsEarned = (int) Math.floor((1.0 - ratio / 2.0) * pointsPossible);
                if (pointsEarned < 0) pointsEarned = 0;
            }

            // Calculate streak bonus
            int streakBonus = 0;
            if (isCorrect) {
                int streak = getStreak(conn, sessionId, playerId, questionId);
                streak++; // include this correct answer
                if (streak == 2) streakBonus = 100;
                else if (streak == 3) streakBonus = 200;
                else if (streak == 4) streakBonus = 300;
                else if (streak >= 5) streakBonus = 500;
                pointsEarned += streakBonus;
            }

            // Insert response
            PreparedStatement insertStmt = conn.prepareStatement(
                "INSERT INTO responses (game_session_id, question_id, player_id, choice, response_time_ms, points_earned) VALUES (?,?,?,?,?,?)");
            insertStmt.setInt(1, sessionId);
            insertStmt.setInt(2, questionId);
            insertStmt.setInt(3, playerId);
            insertStmt.setString(4, choice);
            insertStmt.setInt(5, responseTimeMs);
            insertStmt.setInt(6, pointsEarned);
            insertStmt.executeUpdate();

            // Update player's total score
            PreparedStatement updateScore = conn.prepareStatement(
                "UPDATE players SET total_score = total_score + ? WHERE id=?");
            updateScore.setInt(1, pointsEarned);
            updateScore.setInt(2, playerId);
            updateScore.executeUpdate();

            // Fetch new total score
            PreparedStatement scoreStmt = conn.prepareStatement(
                "SELECT total_score FROM players WHERE id=?");
            scoreStmt.setInt(1, playerId);
            ResultSet scoreRs = scoreStmt.executeQuery();
            int totalScore = 0;
            if (scoreRs.next()) totalScore = scoreRs.getInt("total_score");

            out.print("{\"status\":\"ok\",\"correct\":" + isCorrect
                + ",\"pointsEarned\":" + pointsEarned
                + ",\"streakBonus\":" + streakBonus
                + ",\"totalScore\":" + totalScore + "}");

        } catch (Exception e) {
            out.print("{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    /**
     * Count how many consecutive correct answers the player has leading up to
     * (but not including) the given questionId, within the same session.
     */
    private int getStreak(Connection conn, int sessionId, int playerId, int currentQuestionId) throws Exception {
        PreparedStatement stmt = conn.prepareStatement(
            "SELECT r.points_earned, q.question_order FROM responses r "
            + "JOIN questions q ON r.question_id = q.id "
            + "WHERE r.game_session_id=? AND r.player_id=? "
            + "ORDER BY q.question_order DESC");
        stmt.setInt(1, sessionId);
        stmt.setInt(2, playerId);
        ResultSet rs = stmt.executeQuery();
        int streak = 0;
        while (rs.next()) {
            if (rs.getInt("points_earned") > 0) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }
}
