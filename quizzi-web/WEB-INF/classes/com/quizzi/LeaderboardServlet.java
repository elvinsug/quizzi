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

@WebServlet("/api/leaderboard")
public class LeaderboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        int sessionId = Integer.parseInt(req.getParameter("sessionId"));

        try (Connection conn = DBUtil.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, nickname, total_score FROM players "
                + "WHERE game_session_id=? ORDER BY total_score DESC");
            stmt.setInt(1, sessionId);
            ResultSet rs = stmt.executeQuery();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            int rank = 0;
            while (rs.next()) {
                rank++;
                if (!first) json.append(",");
                first = false;
                json.append("{\"rank\":").append(rank)
                    .append(",\"playerId\":").append(rs.getInt("id"))
                    .append(",\"nickname\":\"").append(escapeJson(rs.getString("nickname"))).append("\"")
                    .append(",\"score\":").append(rs.getInt("total_score"))
                    .append("}");
            }
            json.append("]");
            out.print(json.toString());

        } catch (Exception e) {
            out.print("{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
