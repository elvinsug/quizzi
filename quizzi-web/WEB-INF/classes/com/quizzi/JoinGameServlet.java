package com.quizzi;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/join")
public class JoinGameServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        String pin = req.getParameter("pin");
        String nickname = req.getParameter("nickname");

        if (pin == null || nickname == null || nickname.trim().isEmpty()) {
            out.print("{\"status\":\"error\",\"message\":\"PIN and nickname are required\"}");
            return;
        }
        nickname = nickname.trim();
        if (nickname.length() > 20) nickname = nickname.substring(0, 20);

        try (Connection conn = DBUtil.getConnection()) {
            // Find the game session
            PreparedStatement sessionStmt = conn.prepareStatement(
                "SELECT gs.id, q.title FROM game_sessions gs "
                + "JOIN quizzes q ON gs.quiz_id = q.id "
                + "WHERE gs.game_pin=? AND gs.status IN ('waiting','active','showing_question')");
            sessionStmt.setString(1, pin);
            ResultSet sessionRs = sessionStmt.executeQuery();

            if (!sessionRs.next()) {
                out.print("{\"status\":\"error\",\"message\":\"Game not found or already ended\"}");
                return;
            }

            int sessionId = sessionRs.getInt("id");
            String quizTitle = sessionRs.getString("title");

            // Check nickname uniqueness within session
            PreparedStatement nickCheck = conn.prepareStatement(
                "SELECT id FROM players WHERE game_session_id=? AND nickname=?");
            nickCheck.setInt(1, sessionId);
            nickCheck.setString(2, nickname);
            ResultSet nickRs = nickCheck.executeQuery();
            if (nickRs.next()) {
                out.print("{\"status\":\"error\",\"message\":\"Nickname already taken\"}");
                return;
            }

            // Create player
            PreparedStatement insertPlayer = conn.prepareStatement(
                "INSERT INTO players (game_session_id, nickname) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            insertPlayer.setInt(1, sessionId);
            insertPlayer.setString(2, nickname);
            insertPlayer.executeUpdate();

            ResultSet playerKeys = insertPlayer.getGeneratedKeys();
            playerKeys.next();
            int playerId = playerKeys.getInt(1);

            out.print("{\"status\":\"ok\",\"playerId\":" + playerId
                + ",\"sessionId\":" + sessionId
                + ",\"quizTitle\":\"" + escapeJson(quizTitle) + "\"}");

        } catch (Exception e) {
            out.print("{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
