package com.quizzi;

import java.io.BufferedReader;
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
import jakarta.servlet.http.HttpSession;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

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

        if (username.isEmpty() || password.isEmpty()) {
            resp.setStatus(400);
            out.print("{\"status\":\"error\",\"message\":\"Username and password are required\"}");
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, username, password_hash, display_name FROM users WHERE username = ?");
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                resp.setStatus(401);
                out.print("{\"status\":\"error\",\"message\":\"Invalid username or password\"}");
                return;
            }

            String storedHash = rs.getString("password_hash");
            if (!PasswordUtil.verify(password, storedHash)) {
                resp.setStatus(401);
                out.print("{\"status\":\"error\",\"message\":\"Invalid username or password\"}");
                return;
            }

            int userId = rs.getInt("id");
            String displayName = rs.getString("display_name");

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
