<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Quizzi — Game</title>
    <link rel="stylesheet" href="../css/quizzi.css">
    <script src="../js/polling.js"></script>
</head>
<body>

    <!-- LOBBY -->
    <div id="viewLobby" class="quizzi-bg" style="justify-content:center; padding:2rem;">
        <div class="quizzi-logo" style="font-size:2rem;">QUIZZI</div>
        <h2 style="margin-top:1.5rem; font-size:1.5rem;">You're in!</h2>
        <p style="font-size:1.3rem; font-weight:700; margin:0.5rem 0; color:var(--text-primary);" id="playerName"></p>
        <p style="color:var(--text-muted); margin-top:1rem;">Waiting for the host to start...</p>
        <div class="waiting-dots" style="margin-top:1.5rem;"><span></span><span></span><span></span></div>
        <p style="color:var(--text-light); margin-top:2rem; font-size:0.9rem;" id="lobbyPlayerCount"></p>
    </div>

    <!-- QUESTION -->
    <div id="viewQuestion" class="game-bg hidden" style="padding:1.5rem;">
        <div class="question-header" style="max-width:600px;">
            <span class="step-indicator" id="qNumber"></span>
            <div class="timer" id="timerDisplay">--</div>
        </div>
        <div class="question-text" id="qText" style="max-width:600px; font-size:1.25rem;"></div>
        <div class="answer-grid" style="max-width:500px;" id="answerBtns">
            <button class="answer-btn answer-a" onclick="submitAnswer('a')">
                <span class="answer-icon">&#9650;</span><span id="optA"></span></button>
            <button class="answer-btn answer-b" onclick="submitAnswer('b')">
                <span class="answer-icon">&#9670;</span><span id="optB"></span></button>
            <button class="answer-btn answer-c" onclick="submitAnswer('c')">
                <span class="answer-icon">&#9679;</span><span id="optC"></span></button>
            <button class="answer-btn answer-d" onclick="submitAnswer('d')">
                <span class="answer-icon">&#9632;</span><span id="optD"></span></button>
        </div>
    </div>

    <!-- WAITING -->
    <div id="viewWaiting" class="quizzi-bg hidden" style="justify-content:center; padding:2rem;">
        <div style="font-size:3rem;">&#128274;</div>
        <h2 style="margin-top:1rem;">Answer locked in!</h2>
        <p style="color:var(--text-muted); margin-top:0.5rem;">Waiting for everyone...</p>
        <div class="waiting-dots" style="margin-top:1.5rem;"><span></span><span></span><span></span></div>
    </div>

    <!-- SCORE REVEAL -->
    <div id="viewScore" class="score-reveal hidden">
        <div class="icon" id="scoreIcon"></div>
        <h1 id="scoreTitle" style="font-size:2rem;"></h1>
        <div class="points" id="scorePoints"></div>
        <div class="total" id="scoreTotal"></div>
        <div id="streakBadge" class="streak-badge hidden"></div>
    </div>

    <!-- RANK -->
    <div id="viewRank" class="quizzi-bg hidden" style="justify-content:center; padding:2rem;">
        <h2 style="font-size:1.2rem; color:var(--text-muted);">You are</h2>
        <div style="font-size:4rem; font-weight:800; margin:0.5rem 0;" id="rankNumber"></div>
        <p style="color:var(--text-muted);" id="rankOutOf"></p>
        <div style="font-size:1.5rem; font-weight:700; margin-top:1rem;" id="rankScore"></div>
    </div>

    <!-- GAME OVER -->
    <div id="viewGameOver" class="quizzi-bg hidden" style="justify-content:center; padding:2rem;">
        <div style="font-size:3rem;">&#127881;</div>
        <h1 style="font-size:2rem; margin:1rem 0;">Game Over!</h1>
        <p style="font-size:1.1rem; color:var(--text-muted);">Your final rank:</p>
        <div style="font-size:3.5rem; font-weight:800; margin:0.5rem 0;" id="finalRank"></div>
        <p style="font-size:1.2rem; font-weight:700;" id="finalScore"></p>
        <p style="color:var(--text-muted); margin-top:1rem;" id="finalMessage"></p>
        <a href="./" class="btn btn-black btn-lg" style="margin-top:2rem;">PLAY AGAIN</a>
    </div>

    <!-- STATUS BANNER -->
    <div id="statusBanner" class="status-banner info hidden">
        <span id="bannerText"></span>
        <button class="banner-close" onclick="document.getElementById('statusBanner').classList.add('hidden')">&times;</button>
    </div>

    <script>
        const params = new URLSearchParams(window.location.search);
        const sessionId = parseInt(params.get('sessionId'));
        const playerId = parseInt(params.get('playerId'));
        const nickname = decodeURIComponent(params.get('nickname') || '');
        let currentQuestionId = null, hasAnswered = false, timerInterval = null, currentData = null;

        document.getElementById('playerName').textContent = nickname;

        function hideAll() {
            ['viewLobby','viewQuestion','viewWaiting','viewScore','viewRank','viewGameOver'].forEach(id => {
                const el = document.getElementById(id);
                el.classList.add('hidden');
            });
        }

        QuizziPoll.start(sessionId, playerId, {
            onStateChange(data) {
                currentData = data;
                hideAll();
                switch (data.status) {
                    case 'waiting': showLobby(data); break;
                    case 'showing_question': showQuestion(data); break;
                    case 'showing_results': showScoreReveal(data); break;
                    case 'showing_leaderboard': showRank(data); break;
                    case 'finished': showGameOver(data); break;
                }
            },
            onUpdate(data) {
                currentData = data;
                if (data.status === 'waiting')
                    document.getElementById('lobbyPlayerCount').textContent = data.playerCount + ' player(s) in the lobby';
            },
            onError(msg) {
                document.getElementById('bannerText').textContent = msg;
                document.getElementById('statusBanner').classList.remove('hidden');
            }
        });

        function showLobby(data) {
            document.getElementById('viewLobby').classList.remove('hidden');
            document.getElementById('lobbyPlayerCount').textContent = data.playerCount + ' player(s) in the lobby';
        }

        function showQuestion(data) {
            hasAnswered = false;
            currentQuestionId = data.questionId;
            document.getElementById('viewQuestion').classList.remove('hidden');
            document.getElementById('qNumber').textContent = 'Q' + data.currentQuestionOrder + ' of ' + data.totalQuestions;
            document.getElementById('qText').textContent = data.questionText;
            document.getElementById('optA').textContent = data.optionA;
            document.getElementById('optB').textContent = data.optionB;
            const btns = document.querySelectorAll('#answerBtns .answer-btn');
            btns.forEach(b => b.classList.remove('disabled', 'selected'));
            if (data.optionC) { document.getElementById('optC').textContent = data.optionC; btns[2].classList.remove('hidden'); }
            else { btns[2].classList.add('hidden'); }
            if (data.optionD) { document.getElementById('optD').textContent = data.optionD; btns[3].classList.remove('hidden'); }
            else { btns[3].classList.add('hidden'); }
            startTimer(data.timeLimit, data.questionStartedAt);
        }

        function startTimer(timeLimit, startedAt) {
            if (timerInterval) clearInterval(timerInterval);
            const display = document.getElementById('timerDisplay');
            timerInterval = setInterval(() => {
                const elapsed = (Date.now() - startedAt) / 1000;
                const remaining = Math.max(0, Math.ceil(timeLimit - elapsed));
                display.textContent = remaining;
                display.className = 'timer' + (remaining <= 5 ? ' danger' : remaining <= 10 ? ' warning' : '');
                if (remaining <= 0) { clearInterval(timerInterval); timerInterval = null; }
            }, 250);
        }

        async function submitAnswer(choice) {
            if (hasAnswered) return;
            hasAnswered = true;
            const btns = document.querySelectorAll('#answerBtns .answer-btn');
            btns.forEach(b => b.classList.add('disabled'));
            btns[{a:0,b:1,c:2,d:3}[choice]].classList.add('selected');
            const responseTime = Date.now() - (currentData ? currentData.questionStartedAt : Date.now());
            try {
                await fetch('/quizzi/select?sessionId=' + sessionId + '&questionId=' + currentQuestionId + '&playerId=' + playerId + '&choice=' + choice + '&time=' + responseTime);
            } catch (e) { console.error('Submit error:', e); }
            hideAll();
            document.getElementById('viewWaiting').classList.remove('hidden');
        }

        function showScoreReveal(data) {
            if (timerInterval) { clearInterval(timerInterval); timerInterval = null; }
            const scoreDiv = document.getElementById('viewScore');
            scoreDiv.classList.remove('hidden', 'correct', 'wrong');
            const isCorrect = data.playerCorrect === true;
            scoreDiv.classList.add(isCorrect ? 'correct' : 'wrong');
            document.getElementById('scoreIcon').textContent = isCorrect ? '\u2705' : '\u274C';
            document.getElementById('scoreTitle').textContent = isCorrect ? 'Correct!' : 'Wrong!';
            document.getElementById('scorePoints').textContent = '+' + (data.playerPointsEarned||0).toLocaleString() + ' points';
            document.getElementById('scoreTotal').textContent = 'Total: ' + (data.playerTotalScore||0).toLocaleString() + ' points';
            document.getElementById('streakBadge').classList.add('hidden');
        }

        function showRank(data) {
            document.getElementById('viewRank').classList.remove('hidden');
            document.getElementById('rankNumber').textContent = '#' + (data.playerRank || '?');
            document.getElementById('rankOutOf').textContent = 'out of ' + (data.totalPlayers || '?') + ' players';
            document.getElementById('rankScore').textContent = (data.playerScore||0).toLocaleString() + ' points';
        }

        function showGameOver(data) {
            QuizziPoll.stop();
            document.getElementById('viewGameOver').classList.remove('hidden');
            document.getElementById('finalRank').textContent = '#' + (data.playerRank || '?');
            document.getElementById('finalScore').textContent = (data.playerScore||0).toLocaleString() + ' points';
            document.getElementById('finalMessage').textContent = 'Great game, ' + nickname + '!';
        }
    </script>
</body>
</html>
