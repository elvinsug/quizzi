package com.quizzi;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/api/next")
public class NextQuestionServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        int sessionId = Integer.parseInt(req.getParameter("sessionId"));
        String action = req.getParameter("action");

        try (Connection conn = DBUtil.getConnection()) {
            switch (action) {
                case "next_question": {
                    PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE game_sessions SET status='showing_question', "
                        + "current_question_order = current_question_order + 1, "
                        + "question_started_at = ? WHERE id=?");
                    stmt.setLong(1, System.currentTimeMillis());
                    stmt.setInt(2, sessionId);
                    stmt.executeUpdate();
                    out.print("{\"status\":\"ok\",\"action\":\"next_question\"}");
                    break;
                }
                case "show_results": {
                    PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE game_sessions SET status='showing_results' WHERE id=?");
                    stmt.setInt(1, sessionId);
                    stmt.executeUpdate();
                    out.print("{\"status\":\"ok\",\"action\":\"show_results\"}");
                    break;
                }
                case "show_leaderboard": {
                    PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE game_sessions SET status='showing_leaderboard' WHERE id=?");
                    stmt.setInt(1, sessionId);
                    stmt.executeUpdate();
                    out.print("{\"status\":\"ok\",\"action\":\"show_leaderboard\"}");
                    break;
                }
                case "finish": {
                    PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE game_sessions SET status='finished' WHERE id=?");
                    stmt.setInt(1, sessionId);
                    stmt.executeUpdate();
                    out.print("{\"status\":\"ok\",\"action\":\"finish\"}");
                    break;
                }
                default:
                    out.print("{\"status\":\"error\",\"message\":\"Unknown action: " + action + "\"}");
            }
        } catch (Exception e) {
            out.print("{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }
}
