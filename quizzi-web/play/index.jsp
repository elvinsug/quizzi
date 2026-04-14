<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Quizzi — Join Game</title>
    <link rel="icon" type="image/png" href="../images/favicon.png">
    <link rel="stylesheet" href="../css/quizzi.css">
</head>
<body>
    <div class="auth-container">
        <div class="auth-card">
            <img src="../images/logo.png" alt="Quizzi" class="quizzi-logo-img">
            <div class="quizzi-logo" style="font-size:2.5rem; margin:0 0 2rem;">QUIZZI</div>

            <p style="font-size:1rem; color:var(--text-muted); margin-bottom:1.5rem;">Enter Game PIN</p>

            <div class="pin-boxes" id="pinBoxes">
                <input type="tel" class="pin-box" maxlength="1" inputmode="numeric" pattern="[0-9]*" autocomplete="off" data-index="0">
                <input type="tel" class="pin-box" maxlength="1" inputmode="numeric" pattern="[0-9]*" autocomplete="off" data-index="1">
                <input type="tel" class="pin-box" maxlength="1" inputmode="numeric" pattern="[0-9]*" autocomplete="off" data-index="2">
                <input type="tel" class="pin-box" maxlength="1" inputmode="numeric" pattern="[0-9]*" autocomplete="off" data-index="3">
                <input type="tel" class="pin-box" maxlength="1" inputmode="numeric" pattern="[0-9]*" autocomplete="off" data-index="4">
                <input type="tel" class="pin-box" maxlength="1" inputmode="numeric" pattern="[0-9]*" autocomplete="off" data-index="5">
            </div>

            <button class="btn btn-black btn-lg btn-block hidden" id="joinBtn"
                    onclick="joinGame()" style="margin-top:1.5rem;">
                ENTER
            </button>

            <div id="errorMsg" class="hidden" style="margin-top:1rem; padding:0.75rem 1.5rem;
                 background:var(--pastel-red); border-radius:var(--radius-md); font-weight:700; font-size:0.9rem;"></div>
        </div>
    </div>

    <script>
        const boxes = document.querySelectorAll('.pin-box');
        const joinBtn = document.getElementById('joinBtn');

        function getPin() {
            return Array.from(boxes).map(b => b.value).join('');
        }

        function updateBtn() {
            joinBtn.classList.toggle('hidden', getPin().length !== 6);
        }

        boxes.forEach((box, i) => {
            box.addEventListener('input', () => {
                box.value = box.value.replace(/\D/g, '').slice(0, 1);
                if (box.value && i < boxes.length - 1) boxes[i + 1].focus();
                updateBtn();
            });
            box.addEventListener('keydown', e => {
                if (e.key === 'Backspace' && !box.value && i > 0) {
                    boxes[i - 1].value = '';
                    boxes[i - 1].focus();
                    updateBtn();
                }
                if (e.key === 'Enter' && getPin().length === 6) joinGame();
            });
            box.addEventListener('paste', e => {
                e.preventDefault();
                const data = (e.clipboardData || window.clipboardData).getData('text').replace(/\D/g, '').slice(0, 6);
                for (let j = 0; j < data.length && j < boxes.length; j++) {
                    boxes[j].value = data[j];
                }
                if (data.length > 0) boxes[Math.min(data.length, boxes.length) - 1].focus();
                updateBtn();
            });
        });

        const urlPin = new URLSearchParams(window.location.search).get('pin');
        if (urlPin) {
            const digits = urlPin.replace(/\D/g, '').slice(0, 6);
            for (let j = 0; j < digits.length && j < boxes.length; j++) {
                boxes[j].value = digits[j];
            }
            updateBtn();
        }

        boxes[0].focus();

        function joinGame() {
            const pin = getPin();
            if (pin.length !== 6) return;
            window.location.href = 'nickname.jsp?pin=' + pin;
        }
    </script>
</body>
</html>
