package com.quizzi;

import java.io.BufferedReader;
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
import jakarta.servlet.http.HttpSession;

@WebServlet("/api/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        req.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        String body = sb.toString();

        String username = CreateQuizServlet.extractJsonString(body, "username").trim();
        String password = CreateQuizServlet.extractJsonString(body, "password");
        String displayName = CreateQuizServlet.extractJsonString(body, "displayName").trim();

        if (username.isEmpty() || password.isEmpty()) {
            resp.setStatus(400);
            out.print("{\"status\":\"error\",\"message\":\"Username and password are required\"}");
            return;
        }
        if (username.length() < 3 || username.length() > 50) {
            resp.setStatus(400);
            out.print("{\"status\":\"error\",\"message\":\"Username must be 3-50 characters\"}");
            return;
        }
        if (password.length() < 4) {
            resp.setStatus(400);
            out.print("{\"status\":\"error\",\"message\":\"Password must be at least 4 characters\"}");
            return;
        }

        if (displayName.isEmpty()) displayName = username;

        try (Connection conn = DBUtil.getConnection()) {
            PreparedStatement check = conn.prepareStatement("SELECT id FROM users WHERE username = ?");
            check.setString(1, username);
            if (check.executeQuery().next()) {
                resp.setStatus(409);
                out.print("{\"status\":\"error\",\"message\":\"Username already taken\"}");
                return;
            }

            String hash = PasswordUtil.hash(password);
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO users (username, password_hash, display_name) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, username);
            stmt.setString(2, hash);
            stmt.setString(3, displayName);
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            keys.next();
            int userId = keys.getInt(1);

            HttpSession session = req.getSession(true);
            session.setAttribute("userId", userId);
            session.setAttribute("username", username);
            session.setAttribute("displayName", displayName);

            out.print("{\"status\":\"ok\",\"userId\":" + userId
                + ",\"username\":\"" + escapeJson(username) + "\""
                + ",\"displayName\":\"" + escapeJson(displayName) + "\"}");

        } catch (Exception e) {
            resp.setStatus(500);
            out.print("{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
