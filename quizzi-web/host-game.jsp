<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quizzi — Live Game</title>
    <link rel="icon" type="image/png" href="images/favicon.png">
    <link rel="stylesheet" href="css/quizzi.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
    <script src="js/polling.js"></script>
</head>
<body>
    <div class="game-bg" id="gameScreen">

        <!-- Question View -->
        <div id="viewQuestion" class="hidden" style="width:100%; max-width:740px;">
            <div class="question-header">
                <span class="step-indicator" id="qNumber"></span>
                <div class="timer" id="timerDisplay">--</div>
            </div>
            <div class="question-card">
                <div class="question-text" id="qText"></div>
            </div>
            <div class="answer-grid" id="answerGrid">
                <div class="answer-btn answer-a"><span id="optA"></span></div>
                <div class="answer-btn answer-b"><span id="optB"></span></div>
                <div class="answer-btn answer-c"><span id="optC"></span></div>
                <div class="answer-btn answer-d"><span id="optD"></span></div>
            </div>
            <div class="answer-count-bar mt-3" style="justify-content:center;">
                <span id="answerProgress">0 / 0 answered</span>
            </div>
            <div class="text-center mt-3">
                <button class="btn btn-black btn-lg" onclick="showResults()">SHOW RESULTS</button>
            </div>
        </div>

        <!-- Results View -->
        <div id="viewResults" class="hidden" style="width:100%; max-width:740px;">
            <div class="question-header">
                <span class="step-indicator" id="rQNumber"></span>
                <span style="font-weight:700; font-size:1rem;">
                    Correct: <span id="rCorrectAnswer" style="color:var(--correct-green);"></span>
                </span>
            </div>
            <div class="question-text" id="rQText"></div>
            <div class="results-chart-container">
                <canvas id="resultsChart" height="220"></canvas>
            </div>
            <p class="text-center mt-2" style="font-size:1rem; color:var(--text-muted);" id="rCorrectPercent"></p>
            <div class="text-center mt-3">
                <button class="btn btn-black btn-lg" onclick="showLeaderboard()">SHOW LEADERBOARD</button>
            </div>
        </div>

        <!-- Leaderboard View -->
        <div id="viewLeaderboard" class="hidden" style="width:100%; max-width:600px;">
            <h1 class="text-center" style="font-size:1.8rem; margin-bottom:1.5rem;">LEADERBOARD</h1>
            <ul class="leaderboard-list" id="lbList"></ul>
            <div class="text-center mt-4" id="lbActions"></div>
        </div>

        <!-- Finished / Podium View -->
        <div id="viewPodium" class="hidden" style="width:100%; max-width:700px;">
            <h1 class="text-center" style="font-size:1.8rem; margin-bottom:1rem;">FINAL RESULTS</h1>
            <div class="podium" id="podiumDisplay"></div>
            <h3 class="text-center mt-4" style="color:var(--text-muted); font-size:1rem;">Full Rankings</h3>
            <ul class="leaderboard-list" id="fullRankings" style="margin-top:0.75rem;"></ul>
            <div class="text-center mt-4">
                <a href="index.jsp" class="btn btn-black btn-lg">BACK TO QUIZZES</a>
            </div>
        </div>
    </div>

    <script>
        const params = new URLSearchParams(window.location.search);
        const sessionId = parseInt(params.get('sessionId'));
        let currentData = null;
        let timerInterval = null;
        let resultsChart = null;

        function hideAll() {
            ['viewQuestion','viewResults','viewLeaderboard','viewPodium'].forEach(id =>
                document.getElementById(id).classList.add('hidden'));
        }

        QuizziPoll.start(sessionId, null, {
            onStateChange(data) {
                currentData = data;
                hideAll();
                switch (data.status) {
                    case 'showing_question': showQuestionView(data); break;
                    case 'showing_results': showResultsView(data); break;
                    case 'showing_leaderboard': showLeaderboardView(data); break;
                    case 'finished': showPodiumView(data); break;
                }
            },
            onUpdate(data) {
                currentData = data;
                if (data.status === 'showing_question') {
                    document.getElementById('answerProgress').textContent =
                        (data.answerCount || 0) + ' / ' + data.playerCount + ' answered';
                }
            }
        });

        function showQuestionView(data) {
            document.getElementById('viewQuestion').classList.remove('hidden');
            document.getElementById('qNumber').textContent = 'Q' + data.currentQuestionOrder + ' of ' + data.totalQuestions;
            document.getElementById('qText').textContent = data.questionText;
            document.getElementById('optA').textContent = data.optionA;
            document.getElementById('optB').textContent = data.optionB;
            const optCEl = document.getElementById('optC').parentElement;
            const optDEl = document.getElementById('optD').parentElement;
            if (data.optionC) { document.getElementById('optC').textContent = data.optionC; optCEl.classList.remove('hidden'); }
            else { optCEl.classList.add('hidden'); }
            if (data.optionD) { document.getElementById('optD').textContent = data.optionD; optDEl.classList.remove('hidden'); }
            else { optDEl.classList.add('hidden'); }
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
                if (remaining <= 0) clearInterval(timerInterval);
            }, 250);
        }

        async function showResults() {
            await fetch('/quizzi/api/next?sessionId=' + sessionId + '&action=show_results', { method: 'POST' });
        }

        function showResultsView(data) {
            if (timerInterval) clearInterval(timerInterval);
            document.getElementById('viewResults').classList.remove('hidden');
            document.getElementById('rQNumber').textContent = 'Q' + data.currentQuestionOrder + ' of ' + data.totalQuestions;
            document.getElementById('rQText').textContent = data.questionText;
            const labels = { a: data.optionA, b: data.optionB, c: data.optionC || 'C', d: data.optionD || 'D' };
            document.getElementById('rCorrectAnswer').textContent = labels[data.correctAnswer] || data.correctAnswer;
            const total = (data.countA||0) + (data.countB||0) + (data.countC||0) + (data.countD||0);
            let correctCount = 0;
            switch(data.correctAnswer) {
                case 'a': correctCount = data.countA||0; break;
                case 'b': correctCount = data.countB||0; break;
                case 'c': correctCount = data.countC||0; break;
                case 'd': correctCount = data.countD||0; break;
            }
            const pct = total > 0 ? Math.round(correctCount * 100 / total) : 0;
            document.getElementById('rCorrectPercent').textContent = correctCount + ' / ' + total + ' got it right (' + pct + '%)';
            if (resultsChart) resultsChart.destroy();
            const ctx = document.getElementById('resultsChart').getContext('2d');
            const barColors = ['#F4737A','#5B8DEF','#F5C242','#4BC67E'];
            resultsChart = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: [data.optionA||'A', data.optionB||'B', data.optionC||'C', data.optionD||'D'],
                    datasets: [{ data: [data.countA||0, data.countB||0, data.countC||0, data.countD||0], backgroundColor: barColors, borderRadius: 8 }]
                },
                options: {
                    responsive: true,
                    plugins: { legend: { display: false } },
                    scales: {
                        y: { beginAtZero: true, ticks: { color: '#333', stepSize: 1 }, grid: { color: '#ECECEC' } },
                        x: { ticks: { color: '#333', font: { weight: 'bold' } }, grid: { display: false } }
                    }
                }
            });
        }

        async function showLeaderboard() {
            await fetch('/quizzi/api/next?sessionId=' + sessionId + '&action=show_leaderboard', { method: 'POST' });
        }

        function showLeaderboardView(data) {
            document.getElementById('viewLeaderboard').classList.remove('hidden');
            const list = document.getElementById('lbList');
            list.innerHTML = '';
            const lb = data.leaderboard || [];
            const medals = ['&#129351;','&#129352;','&#129353;','',''];
            lb.slice(0, 5).forEach((entry, i) => {
                const li = document.createElement('li');
                li.className = 'leaderboard-item';
                li.innerHTML = `
                    <span class="leaderboard-rank">${medals[i] || (i+1)}</span>
                    <span class="leaderboard-name">${escHtml(entry.nickname)}</span>
                    <span class="leaderboard-score">${entry.score.toLocaleString()} pts</span>`;
                list.appendChild(li);
            });
            const actions = document.getElementById('lbActions');
            const isLast = currentData && currentData.currentQuestionOrder >= currentData.totalQuestions;
            actions.innerHTML = isLast
                ? '<button class="btn btn-black btn-lg" onclick="finishGame()">FINISH GAME</button>'
                : '<button class="btn btn-black btn-lg" onclick="nextQuestion()">NEXT QUESTION</button>';
        }

        async function nextQuestion() { await fetch('/quizzi/api/next?sessionId=' + sessionId + '&action=next_question', { method: 'POST' }); }
        async function finishGame() { await fetch('/quizzi/api/next?sessionId=' + sessionId + '&action=finish', { method: 'POST' }); }

        function showPodiumView(data) {
            QuizziPoll.stop();
            document.getElementById('viewPodium').classList.remove('hidden');
            const lb = data.leaderboard || [];
            const podium = document.getElementById('podiumDisplay');
            const medals = ['&#129351;','&#129352;','&#129353;'];
            const classes = ['podium-1st','podium-2nd','podium-3rd'];
            podium.innerHTML = '';
            [1, 0, 2].forEach(idx => {
                if (lb[idx]) {
                    const div = document.createElement('div');
                    div.className = 'podium-place ' + classes[idx];
                    div.innerHTML = `<div class="podium-medal">${medals[idx]}</div>
                        <div class="podium-bar"><span class="podium-name">${escHtml(lb[idx].nickname)}</span>
                        <span class="podium-score">${lb[idx].score.toLocaleString()} pts</span></div>`;
                    podium.appendChild(div);
                }
            });
            const full = document.getElementById('fullRankings');
            full.innerHTML = '';
            lb.slice(3).forEach(entry => {
                const li = document.createElement('li');
                li.className = 'leaderboard-item';
                li.innerHTML = `<span class="leaderboard-rank">${entry.rank}</span>
                    <span class="leaderboard-name">${escHtml(entry.nickname)}</span>
                    <span class="leaderboard-score">${entry.score.toLocaleString()} pts</span>`;
                full.appendChild(li);
            });
        }

        function escHtml(s) { const d = document.createElement('div'); d.textContent = s || ''; return d.innerHTML; }
    </script>
</body>
</html>
