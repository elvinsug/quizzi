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

@WebServlet("/api/status")
public class GameStatusServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        int sessionId = Integer.parseInt(req.getParameter("sessionId"));
        String playerIdParam = req.getParameter("playerId");

        try (Connection conn = DBUtil.getConnection()) {
            // Get session info
            PreparedStatement gsStmt = conn.prepareStatement(
                "SELECT gs.*, q.title as quiz_title FROM game_sessions gs "
                + "JOIN quizzes q ON gs.quiz_id = q.id WHERE gs.id=?");
            gsStmt.setInt(1, sessionId);
            ResultSet gsRs = gsStmt.executeQuery();
            if (!gsRs.next()) {
                out.print("{\"status\":\"error\",\"message\":\"Session not found\"}");
                return;
            }

            String status = gsRs.getString("status");
            int quizId = gsRs.getInt("quiz_id");
            int currentQuestionOrder = gsRs.getInt("current_question_order");
            long questionStartedAt = gsRs.getLong("question_started_at");
            String quizTitle = gsRs.getString("quiz_title");

            // Player count
            PreparedStatement pcStmt = conn.prepareStatement(
                "SELECT COUNT(*) AS cnt FROM players WHERE game_session_id=?");
            pcStmt.setInt(1, sessionId);
            ResultSet pcRs = pcStmt.executeQuery();
            pcRs.next();
            int playerCount = pcRs.getInt("cnt");

            // Total questions
            PreparedStatement tqStmt = conn.prepareStatement(
                "SELECT COUNT(*) AS cnt FROM questions WHERE quiz_id=?");
            tqStmt.setInt(1, quizId);
            ResultSet tqRs = tqStmt.executeQuery();
            tqRs.next();
            int totalQuestions = tqRs.getInt("cnt");

            StringBuilder json = new StringBuilder();
            json.append("{\"status\":\"").append(status).append("\"");
            json.append(",\"quizTitle\":\"").append(escapeJson(quizTitle)).append("\"");
            json.append(",\"currentQuestionOrder\":").append(currentQuestionOrder);
            json.append(",\"totalQuestions\":").append(totalQuestions);
            json.append(",\"playerCount\":").append(playerCount);
            json.append(",\"questionStartedAt\":").append(questionStartedAt);

            if ("waiting".equals(status)) {
                // Include player list
                PreparedStatement plStmt = conn.prepareStatement(
                    "SELECT nickname FROM players WHERE game_session_id=? ORDER BY id");
                plStmt.setInt(1, sessionId);
                ResultSet plRs = plStmt.executeQuery();
                json.append(",\"players\":[");
                boolean first = true;
                while (plRs.next()) {
                    if (!first) json.append(",");
                    first = false;
                    json.append("\"").append(escapeJson(plRs.getString("nickname"))).append("\"");
                }
                json.append("]");
            }

            if ("showing_question".equals(status) && currentQuestionOrder > 0) {
                // Include question data
                PreparedStatement qStmt = conn.prepareStatement(
                    "SELECT id, question_text, option_a, option_b, option_c, option_d, time_limit_seconds, points_possible "
                    + "FROM questions WHERE quiz_id=? AND question_order=?");
                qStmt.setInt(1, quizId);
                qStmt.setInt(2, currentQuestionOrder);
                ResultSet qRs = qStmt.executeQuery();
                if (qRs.next()) {
                    json.append(",\"questionId\":").append(qRs.getInt("id"));
                    json.append(",\"questionText\":\"").append(escapeJson(qRs.getString("question_text"))).append("\"");
                    json.append(",\"optionA\":\"").append(escapeJson(qRs.getString("option_a"))).append("\"");
                    json.append(",\"optionB\":\"").append(escapeJson(qRs.getString("option_b"))).append("\"");
                    String optC = qRs.getString("option_c");
                    String optD = qRs.getString("option_d");
                    json.append(",\"optionC\":\"").append(escapeJson(optC != null ? optC : "")).append("\"");
                    json.append(",\"optionD\":\"").append(escapeJson(optD != null ? optD : "")).append("\"");
                    json.append(",\"timeLimit\":").append(qRs.getInt("time_limit_seconds"));

                    // Answer count for this question
                    int qId = qRs.getInt("id");
                    PreparedStatement acStmt = conn.prepareStatement(
                        "SELECT COUNT(*) AS cnt FROM responses WHERE game_session_id=? AND question_id=?");
                    acStmt.setInt(1, sessionId);
                    acStmt.setInt(2, qId);
                    ResultSet acRs = acStmt.executeQuery();
                    acRs.next();
                    json.append(",\"answerCount\":").append(acRs.getInt("cnt"));
                }
            }

            if ("showing_results".equals(status) && currentQuestionOrder > 0) {
                appendResultsData(conn, json, sessionId, quizId, currentQuestionOrder, playerIdParam);
            }

            if ("showing_leaderboard".equals(status)) {
                appendLeaderboardData(conn, json, sessionId, playerIdParam);
            }

            if ("finished".equals(status)) {
                appendLeaderboardData(conn, json, sessionId, playerIdParam);
            }

            json.append("}");
            out.print(json.toString());

        } catch (Exception e) {
            out.print("{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private void appendResultsData(Connection conn, StringBuilder json, int sessionId, int quizId, int questionOrder, String playerIdParam) throws Exception {
        PreparedStatement qStmt = conn.prepareStatement(
            "SELECT id, question_text, option_a, option_b, option_c, option_d, correct_answer FROM questions WHERE quiz_id=? AND question_order=?");
        qStmt.setInt(1, quizId);
        qStmt.setInt(2, questionOrder);
        ResultSet qRs = qStmt.executeQuery();
        if (!qRs.next()) return;

        int questionId = qRs.getInt("id");
        json.append(",\"questionId\":").append(questionId);
        json.append(",\"questionText\":\"").append(escapeJson(qRs.getString("question_text"))).append("\"");
        json.append(",\"optionA\":\"").append(escapeJson(qRs.getString("option_a"))).append("\"");
        json.append(",\"optionB\":\"").append(escapeJson(qRs.getString("option_b"))).append("\"");
        String optC = qRs.getString("option_c");
        String optD = qRs.getString("option_d");
        json.append(",\"optionC\":\"").append(escapeJson(optC != null ? optC : "")).append("\"");
        json.append(",\"optionD\":\"").append(escapeJson(optD != null ? optD : "")).append("\"");
        json.append(",\"correctAnswer\":\"").append(qRs.getString("correct_answer")).append("\"");

        // Response distribution
        PreparedStatement distStmt = conn.prepareStatement(
            "SELECT choice, COUNT(*) AS cnt FROM responses WHERE game_session_id=? AND question_id=? GROUP BY choice");
        distStmt.setInt(1, sessionId);
        distStmt.setInt(2, questionId);
        ResultSet distRs = distStmt.executeQuery();
        int cA = 0, cB = 0, cC = 0, cD = 0;
        while (distRs.next()) {
            switch (distRs.getString("choice")) {
                case "a": cA = distRs.getInt("cnt"); break;
                case "b": cB = distRs.getInt("cnt"); break;
                case "c": cC = distRs.getInt("cnt"); break;
                case "d": cD = distRs.getInt("cnt"); break;
            }
        }
        json.append(",\"countA\":").append(cA);
        json.append(",\"countB\":").append(cB);
        json.append(",\"countC\":").append(cC);
        json.append(",\"countD\":").append(cD);

        // Player's own result
        if (playerIdParam != null) {
            int playerId = Integer.parseInt(playerIdParam);
            PreparedStatement prStmt = conn.prepareStatement(
                "SELECT choice, points_earned FROM responses WHERE game_session_id=? AND question_id=? AND player_id=?");
            prStmt.setInt(1, sessionId);
            prStmt.setInt(2, questionId);
            prStmt.setInt(3, playerId);
            ResultSet prRs = prStmt.executeQuery();
            if (prRs.next()) {
                String playerChoice = prRs.getString("choice");
                int pointsEarned = prRs.getInt("points_earned");
                boolean correct = playerChoice.equals(qRs.getString("correct_answer"));
                json.append(",\"playerChoice\":\"").append(playerChoice).append("\"");
                json.append(",\"playerCorrect\":").append(correct);
                json.append(",\"playerPointsEarned\":").append(pointsEarned);

                PreparedStatement tsStmt = conn.prepareStatement("SELECT total_score FROM players WHERE id=?");
                tsStmt.setInt(1, playerId);
                ResultSet tsRs = tsStmt.executeQuery();
                if (tsRs.next()) json.append(",\"playerTotalScore\":").append(tsRs.getInt("total_score"));
            }
        }
    }

    private void appendLeaderboardData(Connection conn, StringBuilder json, int sessionId, String playerIdParam) throws Exception {
        PreparedStatement lbStmt = conn.prepareStatement(
            "SELECT id, nickname, total_score FROM players WHERE game_session_id=? ORDER BY total_score DESC");
        lbStmt.setInt(1, sessionId);
        ResultSet lbRs = lbStmt.executeQuery();

        json.append(",\"leaderboard\":[");
        boolean first = true;
        int rank = 0;
        int playerRank = -1;
        int playerScore = 0;
        while (lbRs.next()) {
            rank++;
            if (!first) json.append(",");
            first = false;
            int pid = lbRs.getInt("id");
            String nick = lbRs.getString("nickname");
            int score = lbRs.getInt("total_score");
            json.append("{\"rank\":").append(rank)
                .append(",\"nickname\":\"").append(escapeJson(nick)).append("\"")
                .append(",\"score\":").append(score).append("}");

            if (playerIdParam != null && pid == Integer.parseInt(playerIdParam)) {
                playerRank = rank;
                playerScore = score;
            }
        }
        json.append("]");

        if (playerIdParam != null && playerRank > 0) {
            json.append(",\"playerRank\":").append(playerRank);
            json.append(",\"playerScore\":").append(playerScore);
            json.append(",\"totalPlayers\":").append(rank);
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
