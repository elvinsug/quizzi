<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quizzi — Dashboard</title>
    <link rel="icon" type="image/png" href="images/favicon.png">
    <link rel="stylesheet" href="css/quizzi.css">
</head>
<body>
    <nav class="top-nav">
        <a href="index.jsp" class="logo"><img src="images/logo.png" alt="Quizzi" class="logo-icon">Quizzi</a>
        <div class="nav-links">
            <a href="index.jsp">My Quizzes</a>
            <a href="create-quiz.jsp">Create</a>
            <a href="play/index.jsp">Join</a>
        </div>
        <div class="nav-right" id="navRight"></div>
    </nav>

    <div class="dashboard">
        <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:1.5rem; flex-wrap:wrap; gap:1rem;">
            <div>
                <h2 style="font-size:1.6rem;" id="dashTitle">All Quizzes</h2>
                <p style="color:var(--text-muted); font-size:0.9rem; margin-top:0.25rem;" id="dashSubtitle">Create and host live quiz games</p>
            </div>
            <div style="display:flex; gap:0.75rem;">
                <a href="play/index.jsp" class="btn btn-outline">JOIN QUIZ</a>
                <a href="create-quiz.jsp" class="btn btn-black">+ CREATE QUIZ</a>
            </div>
        </div>

        <div id="quizList" class="quiz-grid">
            <div class="text-center" style="grid-column:1/-1; padding:4rem 0; color:var(--text-muted);">
                <p style="font-size:3rem; margin-bottom:1rem;">📝</p>
                <p style="font-size:1.1rem; font-weight:700;">No quizzes yet</p>
                <p style="margin-top:0.5rem;">Create your first quiz to get started!</p>
            </div>
        </div>
    </div>

    <script>
        let currentUser = null;

        async function checkAuth() {
            try {
                const resp = await fetch('/quizzi/api/auth/status');
                currentUser = await resp.json();
            } catch (e) {
                currentUser = { guest: true };
            }

            const navRight = document.getElementById('navRight');
            if (currentUser.guest) {
                navRight.innerHTML = `
                    <a href="welcome.jsp" class="btn btn-sm btn-outline">LOG IN</a>
                    <span class="user-badge">Guest</span>
                `;
                document.getElementById('dashTitle').textContent = 'All Quizzes';
                document.getElementById('dashSubtitle').textContent = 'Log in to save quizzes to your account';
            } else {
                navRight.innerHTML = `
                    <span class="user-badge">${escHtml(currentUser.displayName || currentUser.username)}</span>
                    <button class="btn btn-sm btn-outline" onclick="doLogout()">LOG OUT</button>
                `;
                document.getElementById('dashTitle').textContent = 'My Quizzes';
                document.getElementById('dashSubtitle').textContent = 'Welcome back, ' + escHtml(currentUser.displayName || currentUser.username);
            }
        }

        async function doLogout() {
            await fetch('/quizzi/api/logout', { method: 'POST' });
            window.location.href = 'welcome.jsp';
        }

        async function loadQuizzes() {
            try {
                const mine = currentUser && !currentUser.guest ? '?mine=true' : '';
                const resp = await fetch('/quizzi/api/quizzes' + mine);
                const quizzes = await resp.json();
                const container = document.getElementById('quizList');

                container.innerHTML = '';

                if (!Array.isArray(quizzes) || quizzes.length === 0) {
                    container.innerHTML = `
                        <div class="text-center" style="grid-column:1/-1; padding:4rem 0; color:var(--text-muted);">
                            <p style="font-size:3rem; margin-bottom:1rem;">📝</p>
                            <p style="font-size:1.1rem; font-weight:700;">No quizzes yet</p>
                            <p style="margin-top:0.5rem;">Create your first quiz to get started!</p>
                        </div>`;
                    return;
                }
                const pastels = ['var(--pastel-lavender)', 'var(--pastel-blue)', 'var(--pastel-beige)', 'var(--pastel-pink)', 'var(--pastel-cyan)', 'var(--pastel-yellow)', 'var(--pastel-green)'];

                quizzes.forEach((q, i) => {
                    const card = document.createElement('div');
                    card.className = 'quiz-card';
                    card.innerHTML = `
                        <div class="pastel-tag" style="background:${pastels[i % pastels.length]};">QUIZ</div>
                        <div class="quiz-card-title">${escHtml(q.title)}</div>
                        <div class="quiz-card-meta">
                            ${q.questionCount} question${q.questionCount != 1 ? 's' : ''}
                            &middot; ${new Date(q.createdAt).toLocaleDateString()}
                        </div>
                        <p style="font-size:0.9rem; color:var(--text-muted); margin-bottom:1rem;">${escHtml(q.description || '')}</p>
                        <div class="quiz-card-actions">
                            <button class="btn btn-black btn-sm" onclick="hostGame(${q.id})">HOST LIVE</button>
                            ${q.isOwner ? `<a href="create-quiz.jsp?editId=${q.id}" class="btn btn-outline btn-sm">Edit</a>
                            <button class="btn btn-outline btn-sm" onclick="deleteQuiz(${q.id})" title="Delete">&#128465;</button>` : ''}
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

        async function deleteQuiz(quizId) {
            if (!confirm('Are you sure you want to delete this quiz? This cannot be undone.')) return;
            try {
                const resp = await fetch('/quizzi/api/quiz?id=' + quizId, { method: 'DELETE' });
                const data = await resp.json();
                if (data.status === 'ok') {
                    loadQuizzes();
                } else {
                    alert('Error: ' + data.message);
                }
            } catch (e) {
                alert('Failed to delete quiz: ' + e.message);
            }
        }

        function escHtml(s) {
            const d = document.createElement('div');
            d.textContent = s;
            return d.innerHTML;
        }

        checkAuth().then(() => loadQuizzes());
    </script>
</body>
</html>
