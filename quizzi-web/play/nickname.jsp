<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Quizzi — Enter Nickname</title>
    <link rel="stylesheet" href="../css/quizzi.css">
</head>
<body>
    <div class="auth-container">
        <div class="auth-card" style="animation: fadeScaleIn 0.4s ease-out;">
            <div class="quizzi-logo" style="font-size:2.5rem; margin:0 0 2rem;">QUIZZI</div>

            <div class="form-group" style="margin-bottom:1.25rem;">
                <input type="text" id="nicknameInput" class="form-control"
                       placeholder="Your nickname" maxlength="20" autocomplete="off"
                       style="text-align:center; font-size:1.1rem; font-weight:700;">
            </div>
            <button class="btn btn-black btn-lg btn-block" id="goBtn" onclick="joinWithNickname()">
                LET'S GO!
            </button>
            <div id="errorMsg" class="hidden" style="margin-top:1rem; color:var(--wrong-red); font-weight:700; font-size:0.9rem;"></div>
        </div>
    </div>

    <script>
        const pin = new URLSearchParams(window.location.search).get('pin');
        const nicknameInput = document.getElementById('nicknameInput');
        nicknameInput.addEventListener('keydown', e => { if (e.key === 'Enter') joinWithNickname(); });

        async function joinWithNickname() {
            const nickname = nicknameInput.value.trim();
            if (!nickname) { showError('Please enter a nickname'); return; }
            const goBtn = document.getElementById('goBtn');
            goBtn.disabled = true; goBtn.textContent = 'JOINING...';
            try {
                const resp = await fetch('/quizzi/api/join?pin=' + encodeURIComponent(pin) + '&nickname=' + encodeURIComponent(nickname));
                const data = await resp.json();
                if (data.status === 'ok') {
                    window.location.href = 'game.jsp?sessionId=' + data.sessionId + '&playerId=' + data.playerId + '&nickname=' + encodeURIComponent(nickname);
                } else {
                    showError(data.message || 'Could not join game');
                    goBtn.disabled = false; goBtn.textContent = "LET'S GO!";
                }
            } catch (e) {
                showError('Connection error. Please try again.');
                goBtn.disabled = false; goBtn.textContent = "LET'S GO!";
            }
        }
        function showError(msg) { const el = document.getElementById('errorMsg'); el.textContent = msg; el.classList.remove('hidden'); }
        nicknameInput.focus();
    </script>
</body>
</html>
