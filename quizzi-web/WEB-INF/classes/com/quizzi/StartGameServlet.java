package com.quizzi;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/api/start")
public class StartGameServlet extends HttpServlet {

    private static final Random RNG = new Random();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        int quizId = Integer.parseInt(req.getParameter("quizId"));

        try (Connection conn = DBUtil.getConnection()) {
            // Generate a unique 6-digit PIN
            String pin = generateUniquePin(conn);

            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO game_sessions (quiz_id, game_pin, status) VALUES (?, ?, 'waiting')",
                Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, quizId);
            stmt.setString(2, pin);
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            keys.next();
            int sessionId = keys.getInt(1);

            out.print("{\"status\":\"ok\",\"sessionId\":" + sessionId + ",\"gamePin\":\"" + pin + "\"}");

        } catch (Exception e) {
            resp.setStatus(500);
            out.print("{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    private String generateUniquePin(Connection conn) throws Exception {
        for (int attempts = 0; attempts < 100; attempts++) {
            int num = 100000 + RNG.nextInt(900000);
            String pin = String.valueOf(num);
            PreparedStatement check = conn.prepareStatement(
                "SELECT id FROM game_sessions WHERE game_pin=? AND status != 'finished'");
            check.setString(1, pin);
            ResultSet rs = check.executeQuery();
            if (!rs.next()) return pin;
        }
        throw new RuntimeException("Could not generate unique PIN after 100 attempts");
    }
}
