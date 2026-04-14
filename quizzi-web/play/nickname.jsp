<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Quizzi — Enter Nickname</title>
    <link rel="stylesheet" href="../css/quizzi.css">
    <style>
        .nickname-card {
            animation: slideUp 0.4s ease-out;
        }
        @keyframes slideUp {
            from { transform: translateY(40px); opacity: 0; }
            to { transform: translateY(0); opacity: 1; }
        }
    </style>
</head>
<body>
    <div class="quizzi-bg" style="justify-content:center; padding:2rem;">
        <div class="quizzi-logo" style="font-size:3rem; margin-bottom:2.5rem;">QUIZZI</div>

        <div class="card nickname-card" style="text-align:center;">
            <div class="form-group" style="margin-bottom:1.25rem;">
                <input type="text" id="nicknameInput" class="form-control"
                       placeholder="Nickname" maxlength="20" autocomplete="off"
                       style="text-align:center; font-size:1.2rem; font-weight:700;">
            </div>
            <button class="btn btn-green btn-lg btn-block" id="goBtn" onclick="joinWithNickname()">
                Let's go!
            </button>
            <div id="errorMsg" class="hidden" style="margin-top:1rem; color:var(--wrong-red); font-weight:700; font-size:0.9rem;"></div>
        </div>
    </div>

    <script>
        const pin = new URLSearchParams(window.location.search).get('pin');
        const nicknameInput = document.getElementById('nicknameInput');

        nicknameInput.addEventListener('keydown', e => {
            if (e.key === 'Enter') joinWithNickname();
        });

        async function joinWithNickname() {
            const nickname = nicknameInput.value.trim();
            if (!nickname) {
                showError('Please enter a nickname');
                return;
            }

            const goBtn = document.getElementById('goBtn');
            goBtn.disabled = true;
            goBtn.textContent = 'Joining...';

            try {
                const resp = await fetch('/quizzi/api/join?pin=' + encodeURIComponent(pin) + '&nickname=' + encodeURIComponent(nickname));
                const data = await resp.json();

                if (data.status === 'ok') {
                    window.location.href = 'game.jsp?sessionId=' + data.sessionId + '&playerId=' + data.playerId + '&nickname=' + encodeURIComponent(nickname);
                } else {
                    showError(data.message || 'Could not join game');
                    goBtn.disabled = false;
                    goBtn.textContent = "Let's go!";
                }
            } catch (e) {
                showError('Connection error. Please try again.');
                goBtn.disabled = false;
                goBtn.textContent = "Let's go!";
            }
        }

        function showError(msg) {
            const el = document.getElementById('errorMsg');
            el.textContent = msg;
            el.classList.remove('hidden');
        }

        nicknameInput.focus();
    </script>
</body>
</html>
