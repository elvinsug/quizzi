-- ============================================================
-- Quizzi Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS quizzi;
USE quizzi;

-- 1. Quizzes created by the instructor
CREATE TABLE quizzes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Questions belonging to a quiz
CREATE TABLE questions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    quiz_id INT NOT NULL,
    question_text VARCHAR(500) NOT NULL,
    option_a VARCHAR(200) NOT NULL,
    option_b VARCHAR(200) NOT NULL,
    option_c VARCHAR(200),
    option_d VARCHAR(200),
    correct_answer CHAR(1) NOT NULL,       -- 'a', 'b', 'c', or 'd'
    time_limit_seconds INT DEFAULT 20,      -- countdown per question
    points_possible INT DEFAULT 1000,       -- max points for this question
    question_order INT NOT NULL,            -- display order within the quiz
    FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE
);

-- 3. Live game sessions
CREATE TABLE game_sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    quiz_id INT NOT NULL,
    game_pin VARCHAR(6) NOT NULL UNIQUE,    -- 6-digit join code
    status ENUM('waiting','active','showing_question','showing_results','showing_leaderboard','finished') DEFAULT 'waiting',
    current_question_order INT DEFAULT 0,   -- which question is active (0 = not started)
    question_started_at BIGINT,             -- epoch millis when current question timer started
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (quiz_id) REFERENCES quizzes(id)
);

-- 4. Players who joined a game session
CREATE TABLE players (
    id INT AUTO_INCREMENT PRIMARY KEY,
    game_session_id INT NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    total_score INT DEFAULT 0,
    FOREIGN KEY (game_session_id) REFERENCES game_sessions(id) ON DELETE CASCADE
);

-- 5. Individual responses
CREATE TABLE responses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    game_session_id INT NOT NULL,
    question_id INT NOT NULL,
    player_id INT NOT NULL,
    choice CHAR(1) NOT NULL,                -- 'a', 'b', 'c', or 'd'
    response_time_ms INT,                   -- milliseconds from question start
    points_earned INT DEFAULT 0,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (game_session_id) REFERENCES game_sessions(id),
    FOREIGN KEY (question_id) REFERENCES questions(id),
    FOREIGN KEY (player_id) REFERENCES players(id)
);
