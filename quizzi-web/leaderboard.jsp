<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quizzi — Leaderboard</title>
    <link rel="icon" type="image/png" href="images/favicon.png">
    <link rel="stylesheet" href="css/quizzi.css">
</head>
<body>
    <div class="game-bg" style="justify-content:center;">
        <h1 class="text-center" style="font-size:2rem; margin-bottom:2rem;">LEADERBOARD</h1>
        <ul class="leaderboard-list" id="lbList"></ul>
        <div class="text-center mt-4">
            <a href="index.jsp" class="btn btn-black">BACK TO DASHBOARD</a>
        </div>
    </div>
    <script>
        const sessionId = new URLSearchParams(window.location.search).get('sessionId');
        async function loadLeaderboard() {
            const resp = await fetch('/quizzi/api/leaderboard?sessionId=' + sessionId);
            const data = await resp.json();
            const list = document.getElementById('lbList');
            const medals = ['&#129351;','&#129352;','&#129353;'];
            list.innerHTML = '';
            data.forEach((entry, i) => {
                const li = document.createElement('li');
                li.className = 'leaderboard-item';
                li.innerHTML = `<span class="leaderboard-rank">${medals[i] || entry.rank}</span>
                    <span class="leaderboard-name">${escHtml(entry.nickname)}</span>
                    <span class="leaderboard-score">${entry.score.toLocaleString()} pts</span>`;
                list.appendChild(li);
            });
        }
        function escHtml(s) { const d = document.createElement('div'); d.textContent = s||''; return d.innerHTML; }
        loadLeaderboard();
    </script>
</body>
</html>
