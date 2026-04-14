<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Quizzi — Join Game</title>
    <link rel="stylesheet" href="../css/quizzi.css">
</head>
<body>
    <div class="quizzi-bg" style="justify-content:center; padding:2rem;">
        <div class="quizzi-logo" style="font-size:3rem; margin-bottom:2rem;">QUIZZI</div>

        <p style="font-size:1.1rem; opacity:0.75; margin-bottom:1.5rem;">Enter Game PIN</p>

        <input type="tel" id="pinInput" class="pin-input" placeholder="------"
               maxlength="6" inputmode="numeric" pattern="[0-9]*" autocomplete="off">

        <button class="btn btn-green btn-lg btn-block hidden" id="joinBtn"
                onclick="joinGame()" style="max-width:360px; margin-top:1.5rem;">
            Enter &rarr;
        </button>

        <div id="errorMsg" class="hidden" style="margin-top:1rem; padding:0.75rem 1.5rem;
             background:rgba(231,76,60,0.2); border-radius:var(--radius-md); font-weight:700;"></div>
    </div>

    <script>
        const pinInput = document.getElementById('pinInput');
        const joinBtn = document.getElementById('joinBtn');

        // Auto-fill PIN from QR code URL param
        const urlPin = new URLSearchParams(window.location.search).get('pin');
        if (urlPin) {
            pinInput.value = urlPin;
            joinBtn.classList.remove('hidden');
        }

        pinInput.addEventListener('input', () => {
            pinInput.value = pinInput.value.replace(/\D/g, '').slice(0, 6);
            if (pinInput.value.length === 6) {
                joinBtn.classList.remove('hidden');
            } else {
                joinBtn.classList.add('hidden');
            }
        });

        pinInput.addEventListener('keydown', e => {
            if (e.key === 'Enter' && pinInput.value.length === 6) joinGame();
        });

        async function joinGame() {
            const pin = pinInput.value;
            if (pin.length !== 6) return;
            window.location.href = 'nickname.jsp?pin=' + pin;
        }
    </script>
</body>
</html>
