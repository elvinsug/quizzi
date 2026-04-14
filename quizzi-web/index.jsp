<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quizzi — Dashboard</title>
    <link rel="stylesheet" href="css/quizzi.css">
</head>
<body>
    <div class="dashboard">
        <div class="dashboard-header">
            <div class="quizzi-logo">QUIZZI</div>
            <a href="create-quiz.jsp" class="btn btn-purple">+ Create Quiz</a>
        </div>

        <h2 style="margin-bottom:1rem;">My Quizzes</h2>

        <div id="quizList" class="quiz-grid">
            <div class="text-center" style="grid-column:1/-1; padding:4rem 0; color:var(--text-muted);">
                <p style="font-size:1.2rem; font-weight:700;">No quizzes yet</p>
                <p>Create your first quiz to get started!</p>
            </div>
        </div>
    </div>

    <script>
        async function loadQuizzes() {
            try {
                const resp = await fetch('/quizzi/api/quizzes');
                const quizzes = await resp.json();
                const container = document.getElementById('quizList');

                if (!Array.isArray(quizzes) || quizzes.length === 0) return;

                container.innerHTML = '';
                quizzes.forEach(q => {
                    const card = document.createElement('div');
                    card.className = 'quiz-card';
                    card.innerHTML = `
                        <div class="quiz-card-title">\${escHtml(q.title)}</div>
                        <div class="quiz-card-meta">
                            \${q.questionCount} question\${q.questionCount != 1 ? 's' : ''}
                            &middot; Created \${new Date(q.createdAt).toLocaleDateString()}
                        </div>
                        <p style="font-size:0.9rem; color:#666; margin-bottom:1rem;">\${escHtml(q.description || '')}</p>
                        <div class="quiz-card-actions">
                            <button class="btn btn-green" onclick="hostGame(\${q.id})">Host Live</button>
                            <a href="create-quiz.jsp?editId=\${q.id}" class="btn btn-blue" style="font-size:0.85rem;">Edit</a>
                        </div>
                    `;
                    container.appendChild(card);
                });
            } catch (e) {
                console.error('Failed to load quizzes:', e);
            }
        }

        async function hostGame(quizId) {
            try {
                const resp = await fetch('/quizzi/api/start?quizId=' + quizId, { method: 'POST' });
                const data = await resp.json();
                if (data.status === 'ok') {
                    window.location.href = 'lobby.jsp?sessionId=' + data.sessionId + '&pin=' + data.gamePin;
                } else {
                    alert('Error: ' + data.message);
                }
            } catch (e) {
                alert('Failed to start game: ' + e.message);
            }
        }

        function escHtml(s) {
            const d = document.createElement('div');
            d.textContent = s;
            return d.innerHTML;
        }

        loadQuizzes();
    </script>
</body>
</html>
