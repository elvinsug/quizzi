<%@ page contentType="text/html;charset=UTF-8" language="java" %>
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

    <!-- LOBBY: Waiting for host -->
    <div id="viewLobby" class="quizzi-bg" style="justify-content:center; padding:2rem;">
        <div class="quizzi-logo">QUIZZI</div>
        <h2 style="margin-top:1rem;">You're in!</h2>
        <p style="font-size:1.5rem; font-weight:700; margin:0.5rem 0;" id="playerName"></p>
        <p style="opacity:0.7; margin-top:1rem;">Waiting for the host to start the game...</p>
        <div class="waiting-dots" style="margin-top:1.5rem;">
            <span></span><span></span><span></span>
        </div>
        <p style="opacity:0.5; margin-top:2rem; font-size:0.9rem;" id="lobbyPlayerCount"></p>
    </div>

    <!-- QUESTION: 4 answer buttons -->
    <div id="viewQuestion" class="quizzi-bg hidden" style="padding:1.5rem;">
        <div class="question-header" style="max-width:600px;">
            <span class="question-number" id="qNumber"></span>
            <div class="timer" id="timerDisplay">--</div>
        </div>
        <div class="question-text" id="qText" style="max-width:600px; font-size:1.3rem;"></div>
        <div class="answer-grid" style="max-width:500px;" id="answerBtns">
            <button class="answer-btn answer-a" onclick="submitAnswer('a')">
                <span class="answer-icon">&#9650;</span><span id="optA"></span>
            </button>
            <button class="answer-btn answer-b" onclick="submitAnswer('b')">
                <span class="answer-icon">&#9670;</span><span id="optB"></span>
            </button>
            <button class="answer-btn answer-c" onclick="submitAnswer('c')">
                <span class="answer-icon">&#9679;</span><span id="optC"></span>
            </button>
            <button class="answer-btn answer-d" onclick="submitAnswer('d')">
                <span class="answer-icon">&#9632;</span><span id="optD"></span>
            </button>
        </div>
    </div>

    <!-- WAITING: Answer locked in -->
    <div id="viewWaiting" class="quizzi-bg hidden" style="justify-content:center; padding:2rem;">
        <div style="font-size:3rem;">&#128274;</div>
        <h2 style="margin-top:1rem;">Answer locked in!</h2>
        <p style="opacity:0.7; margin-top:0.5rem;">Waiting for everyone...</p>
        <div class="waiting-dots" style="margin-top:1.5rem;">
            <span></span><span></span><span></span>
        </div>
    </div>

    <!-- SCORE REVEAL: Correct or Wrong -->
    <div id="viewScore" class="score-reveal hidden">
        <div class="icon" id="scoreIcon"></div>
        <h1 id="scoreTitle" style="font-size:2rem;"></h1>
        <div class="points" id="scorePoints"></div>
        <div class="total" id="scoreTotal"></div>
        <div id="streakBadge" class="streak-badge hidden"></div>
    </div>

    <!-- RANK: Your position -->
    <div id="viewRank" class="quizzi-bg hidden" style="justify-content:center; padding:2rem;">
        <h2 style="font-size:1.3rem; opacity:0.7;">You are</h2>
        <div style="font-size:4rem; font-family:'Montserrat',sans-serif; font-weight:900; margin:0.5rem 0;" id="rankNumber"></div>
        <p style="font-size:1.1rem; opacity:0.7;" id="rankOutOf"></p>
        <div style="font-size:1.5rem; font-weight:700; margin-top:1rem;" id="rankScore"></div>
    </div>

    <!-- GAME OVER -->
    <div id="viewGameOver" class="quizzi-bg hidden" style="justify-content:center; padding:2rem;">
        <div style="font-size:3rem;">&#127881;</div>
        <h1 style="font-size:2rem; margin:1rem 0;">Game Over!</h1>
        <p style="font-size:1.2rem;">Your final rank:</p>
        <div style="font-size:3.5rem; font-family:'Montserrat',sans-serif; font-weight:900; margin:0.5rem 0;" id="finalRank"></div>
        <p style="font-size:1.3rem; font-weight:700;" id="finalScore"></p>
        <p style="opacity:0.7; margin-top:1rem;" id="finalMessage"></p>
        <a href="./" class="btn btn-purple btn-lg" style="margin-top:2rem;">Play Again</a>
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

        let currentQuestionId = null;
        let hasAnswered = false;
        let timerInterval = null;

        document.getElementById('playerName').textContent = nickname;

        function hideAll() {
            ['viewLobby','viewQuestion','viewWaiting','viewScore','viewRank','viewGameOver'].forEach(id =>
                document.getElementById(id).classList.add('hidden')
            );
            document.getElementById('viewLobby').className = 'quizzi-bg hidden';
            document.getElementById('viewQuestion').className = 'quizzi-bg hidden';
            document.getElementById('viewWaiting').className = 'quizzi-bg hidden';
            document.getElementById('viewRank').className = 'quizzi-bg hidden';
            document.getElementById('viewGameOver').className = 'quizzi-bg hidden';
        }

        QuizziPoll.start(sessionId, playerId, {
            onStateChange(data) {
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
                if (data.status === 'waiting') {
                    document.getElementById('lobbyPlayerCount').textContent =
                        data.playerCount + ' player(s) in the lobby';
                }
                if (data.status === 'showing_question' && timerInterval === null) {
                    startTimer(data.timeLimit, data.questionStartedAt);
                }
            },
            onError(msg) {
                const banner = document.getElementById('statusBanner');
                document.getElementById('bannerText').textContent = msg;
                banner.classList.remove('hidden');
            }
        });

        function showLobby(data) {
            document.getElementById('viewLobby').classList.remove('hidden');
            document.getElementById('lobbyPlayerCount').textContent =
                data.playerCount + ' player(s) in the lobby';
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
            btns.forEach(b => { b.classList.remove('disabled', 'selected'); });

            const optCBtn = btns[2];
            const optDBtn = btns[3];
            if (data.optionC) { document.getElementById('optC').textContent = data.optionC; optCBtn.classList.remove('hidden'); }
            else { optCBtn.classList.add('hidden'); }
            if (data.optionD) { document.getElementById('optD').textContent = data.optionD; optDBtn.classList.remove('hidden'); }
            else { optDBtn.classList.add('hidden'); }

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
            const choiceIdx = { a: 0, b: 1, c: 2, d: 3 };
            btns[choiceIdx[choice]].classList.add('selected');

            const now = Date.now();
            const responseTime = now - (currentData ? currentData.questionStartedAt : now);

            try {
                const resp = await fetch('/quizzi/select?sessionId=' + sessionId
                    + '&questionId=' + currentQuestionId
                    + '&playerId=' + playerId
                    + '&choice=' + choice
                    + '&time=' + responseTime);
                const data = await resp.json();
                // Store for showing results later
                window._lastAnswer = data;
            } catch (e) {
                console.error('Submit error:', e);
            }

            // Show waiting screen
            hideAll();
            document.getElementById('viewWaiting').classList.remove('hidden');
        }

        let currentData = null;
        const origOnUpdate = QuizziPoll;
        // Patch to keep currentData
        const origStart = QuizziPoll.start;

        // We actually need to capture data in onUpdate
        // Let's store it via a global
        (function patchPoll() {
            const _origStart = QuizziPoll.start.bind(QuizziPoll);
            // Already handled above via the onUpdate callback
        })();

        // Store data globally for submitAnswer
        const _origHandlers = {
            onUpdate(data) {
                currentData = data;
                if (data.status === 'waiting') {
                    document.getElementById('lobbyPlayerCount').textContent =
                        data.playerCount + ' player(s) in the lobby';
                }
            }
        };

        function showScoreReveal(data) {
            if (timerInterval) { clearInterval(timerInterval); timerInterval = null; }

            const scoreDiv = document.getElementById('viewScore');
            scoreDiv.classList.remove('hidden', 'correct', 'wrong');

            const isCorrect = data.playerCorrect === true;
            scoreDiv.classList.add(isCorrect ? 'correct' : 'wrong');
            document.getElementById('scoreIcon').textContent = isCorrect ? '\u2705' : '\u274C';
            document.getElementById('scoreTitle').textContent = isCorrect ? 'Correct!' : 'Wrong!';

            const pts = data.playerPointsEarned || 0;
            document.getElementById('scorePoints').textContent = '+' + pts.toLocaleString() + ' points';
            document.getElementById('scoreTotal').textContent = 'Total: ' + (data.playerTotalScore || 0).toLocaleString() + ' points';

            // Hide streak badge by default
            document.getElementById('streakBadge').classList.add('hidden');
        }

        function showRank(data) {
            document.getElementById('viewRank').classList.remove('hidden');
            document.getElementById('rankNumber').textContent = '#' + (data.playerRank || '?');
            document.getElementById('rankOutOf').textContent = 'out of ' + (data.totalPlayers || '?') + ' players';
            document.getElementById('rankScore').textContent = (data.playerScore || 0).toLocaleString() + ' points';
        }

        function showGameOver(data) {
            QuizziPoll.stop();
            document.getElementById('viewGameOver').classList.remove('hidden');
            document.getElementById('finalRank').textContent = '#' + (data.playerRank || '?');
            document.getElementById('finalScore').textContent = (data.playerScore || 0).toLocaleString() + ' points';
            document.getElementById('finalMessage').textContent = 'Great game, ' + nickname + '!';
        }
    </script>
</body>
</html>
