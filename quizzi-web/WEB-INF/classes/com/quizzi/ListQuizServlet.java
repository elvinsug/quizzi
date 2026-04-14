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

@WebServlet("/api/quizzes")
public class ListQuizServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        try (Connection conn = DBUtil.getConnection()) {
            Integer userId = null;
            jakarta.servlet.http.HttpSession session = req.getSession(false);
            if (session != null && session.getAttribute("userId") != null) {
                userId = (Integer) session.getAttribute("userId");
            }

            String ownerFilter = req.getParameter("mine");

            PreparedStatement stmt;
            if ("true".equals(ownerFilter) && userId != null) {
                stmt = conn.prepareStatement(
                    "SELECT q.id, q.title, q.description, q.created_at, q.user_id, "
                    + "(SELECT COUNT(*) FROM questions WHERE quiz_id = q.id) AS question_count "
                    + "FROM quizzes q WHERE q.user_id = ? ORDER BY q.created_at DESC");
                stmt.setInt(1, userId);
            } else {
                stmt = conn.prepareStatement(
                    "SELECT q.id, q.title, q.description, q.created_at, q.user_id, "
                    + "(SELECT COUNT(*) FROM questions WHERE quiz_id = q.id) AS question_count "
                    + "FROM quizzes q ORDER BY q.created_at DESC");
            }
            ResultSet rs = stmt.executeQuery();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                first = false;
                json.append("{\"id\":").append(rs.getInt("id"))
                    .append(",\"title\":\"").append(escapeJson(rs.getString("title"))).append("\"")
                    .append(",\"description\":\"").append(escapeJson(rs.getString("description") != null ? rs.getString("description") : "")).append("\"")
                    .append(",\"questionCount\":").append(rs.getInt("question_count"))
                    .append(",\"createdAt\":\"").append(rs.getTimestamp("created_at").toString()).append("\"")
                    .append(",\"isOwner\":").append(userId != null && rs.getObject("user_id") != null && rs.getInt("user_id") == userId)
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
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
