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

@WebServlet("/api/quiz")
public class CreateQuizServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        req.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        // Read raw JSON body
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        String body = sb.toString();

        try (Connection conn = DBUtil.getConnection()) {
            // Manual JSON parsing (no external libraries)
            String title = extractJsonString(body, "title");
            String description = extractJsonString(body, "description");

            Integer userId = null;
            jakarta.servlet.http.HttpSession session = req.getSession(false);
            if (session != null && session.getAttribute("userId") != null) {
                userId = (Integer) session.getAttribute("userId");
            }

            PreparedStatement quizStmt = conn.prepareStatement(
                "INSERT INTO quizzes (title, description, user_id) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            quizStmt.setString(1, title);
            quizStmt.setString(2, description);
            if (userId != null) quizStmt.setInt(3, userId);
            else quizStmt.setNull(3, java.sql.Types.INTEGER);
            quizStmt.executeUpdate();

            ResultSet keys = quizStmt.getGeneratedKeys();
            keys.next();
            int quizId = keys.getInt(1);

            // Parse questions array
            String questionsArray = extractJsonArray(body, "questions");
            if (questionsArray != null && !questionsArray.isEmpty()) {
                String[] questionObjects = splitJsonArray(questionsArray);
                int order = 1;
                for (String qObj : questionObjects) {
                    String questionText = extractJsonString(qObj, "questionText");
                    String optA = extractJsonString(qObj, "optionA");
                    String optB = extractJsonString(qObj, "optionB");
                    String optC = extractJsonString(qObj, "optionC");
                    String optD = extractJsonString(qObj, "optionD");
                    String correct = extractJsonString(qObj, "correctAnswer");
                    int timeLimit = extractJsonInt(qObj, "timeLimit", 20);
                    int points = extractJsonInt(qObj, "points", 1000);

                    PreparedStatement qStmt = conn.prepareStatement(
                        "INSERT INTO questions (quiz_id, question_text, option_a, option_b, option_c, option_d, correct_answer, time_limit_seconds, points_possible, question_order) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?)");
                    qStmt.setInt(1, quizId);
                    qStmt.setString(2, questionText);
                    qStmt.setString(3, optA);
                    qStmt.setString(4, optB);
                    qStmt.setString(5, optC != null && !optC.isEmpty() ? optC : null);
                    qStmt.setString(6, optD != null && !optD.isEmpty() ? optD : null);
                    qStmt.setString(7, correct);
                    qStmt.setInt(8, timeLimit);
                    qStmt.setInt(9, points);
                    qStmt.setInt(10, order++);
                    qStmt.executeUpdate();
                }
            }

            out.print("{\"status\":\"ok\",\"quizId\":" + quizId + "}");

        } catch (Exception e) {
            resp.setStatus(500);
            out.print("{\"status\":\"error\",\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    // ---- Minimal JSON helpers (no external libs) ----

    static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        idx = json.indexOf(":", idx + search.length());
        if (idx < 0) return "";
        idx = json.indexOf("\"", idx + 1);
        if (idx < 0) return "";
        int end = idx + 1;
        while (end < json.length()) {
            if (json.charAt(end) == '\\') { end += 2; continue; }
            if (json.charAt(end) == '"') break;
            end++;
        }
        return json.substring(idx + 1, end)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n");
    }

    static int extractJsonInt(String json, String key, int defaultVal) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return defaultVal;
        idx = json.indexOf(":", idx + search.length());
        if (idx < 0) return defaultVal;
        idx++;
        while (idx < json.length() && json.charAt(idx) == ' ') idx++;
        int end = idx;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        if (end == idx) return defaultVal;
        try {
            return Integer.parseInt(json.substring(idx, end));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    static String extractJsonArray(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        idx = json.indexOf("[", idx + search.length());
        if (idx < 0) return null;
        int depth = 0;
        int start = idx;
        for (int i = idx; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) return json.substring(start + 1, i); }
        }
        return null;
    }

    static String[] splitJsonArray(String arrayContent) {
        java.util.List<String> items = new java.util.ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    items.add(arrayContent.substring(start, i + 1).trim());
                    start = i + 1;
                    while (start < arrayContent.length() && (arrayContent.charAt(start) == ',' || arrayContent.charAt(start) == ' ')) start++;
                }
            }
        }
        return items.toArray(new String[0]);
    }
}
