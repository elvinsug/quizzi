<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Quizzi — Join Game</title>
    <link rel="stylesheet" href="../css/quizzi.css">
</head>
<body>
    <div class="auth-container">
        <div class="auth-card">
            <div class="quizzi-logo" style="font-size:2.5rem; margin:0 0 2rem;">QUIZZI</div>

            <p style="font-size:1rem; color:var(--text-muted); margin-bottom:1.5rem;">Enter Game PIN</p>

            <input type="tel" id="pinInput" class="pin-input" placeholder="------"
                   maxlength="6" inputmode="numeric" pattern="[0-9]*" autocomplete="off"
                   style="margin-bottom:1rem;">

            <button class="btn btn-black btn-lg btn-block hidden" id="joinBtn"
                    onclick="joinGame()">
                ENTER
            </button>

            <div id="errorMsg" class="hidden" style="margin-top:1rem; padding:0.75rem 1.5rem;
                 background:var(--pastel-red); border-radius:var(--radius-md); font-weight:700; font-size:0.9rem;"></div>
        </div>
    </div>

    <script>
        const pinInput = document.getElementById('pinInput');
        const joinBtn = document.getElementById('joinBtn');

        const urlPin = new URLSearchParams(window.location.search).get('pin');
        if (urlPin) {
            pinInput.value = urlPin;
            joinBtn.classList.remove('hidden');
        }

        pinInput.addEventListener('input', () => {
            pinInput.value = pinInput.value.replace(/\D/g, '').slice(0, 6);
            joinBtn.classList.toggle('hidden', pinInput.value.length !== 6);
        });
        pinInput.addEventListener('keydown', e => {
            if (e.key === 'Enter' && pinInput.value.length === 6) joinGame();
        });

        function joinGame() {
            const pin = pinInput.value;
            if (pin.length !== 6) return;
            window.location.href = 'nickname.jsp?pin=' + pin;
        }
    </script>
</body>
</html>
