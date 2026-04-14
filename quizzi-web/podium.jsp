<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quizzi — Final Results</title>
    <link rel="stylesheet" href="css/quizzi.css">
</head>
<body>
    <div class="quizzi-bg" style="padding:2rem;">
        <h1 class="text-center" style="font-size:2.5rem; margin:1rem 0;">&#127942; FINAL RESULTS &#127942;</h1>
        <div class="podium" id="podiumDisplay"></div>

        <h3 class="text-center mt-4" style="opacity:0.8;">Full Rankings</h3>
        <ul class="leaderboard-list" id="fullRankings" style="margin-top:0.75rem; max-width:600px; width:100%;"></ul>

        <div class="text-center mt-4">
            <a href="index.jsp" class="btn btn-purple btn-lg">Back to Quizzes</a>
        </div>
    </div>

    <script>
        const params = new URLSearchParams(window.location.search);
        const sessionId = params.get('sessionId');

        async function loadResults() {
            const resp = await fetch('/quizzi/api/leaderboard?sessionId=' + sessionId);
            const lb = await resp.json();

            const podium = document.getElementById('podiumDisplay');
            const medals = ['&#129351;','&#129352;','&#129353;'];
            const classes = ['podium-1st','podium-2nd','podium-3rd'];

            const order = [1, 0, 2];
            order.forEach(idx => {
                if (lb[idx]) {
                    const div = document.createElement('div');
                    div.className = 'podium-place ' + classes[idx];
                    div.innerHTML = `
                        <div class="podium-medal">${medals[idx]}</div>
                        <div class="podium-bar">
                            <span class="podium-name">${escHtml(lb[idx].nickname)}</span>
                            <span class="podium-score">${lb[idx].score.toLocaleString()} pts</span>
                        </div>
                    `;
                    podium.appendChild(div);
                }
            });

            const full = document.getElementById('fullRankings');
            lb.slice(3).forEach(entry => {
                const li = document.createElement('li');
                li.className = 'leaderboard-item';
                li.innerHTML = `
                    <span class="leaderboard-rank">${entry.rank}</span>
                    <span class="leaderboard-name">${escHtml(entry.nickname)}</span>
                    <span class="leaderboard-score">${entry.score.toLocaleString()} pts</span>
                `;
                full.appendChild(li);
            });
        }

        function escHtml(s) {
            const d = document.createElement('div');
            d.textContent = s || '';
            return d.innerHTML;
        }

        loadResults();
    </script>
</body>
</html>
