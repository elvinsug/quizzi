<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quizzi — Game Lobby</title>
    <link rel="stylesheet" href="css/quizzi.css">
    <script src="https://cdn.jsdelivr.net/npm/qrcodejs@1.0.0/qrcode.min.js"></script>
    <script src="js/polling.js"></script>
</head>
<body>
    <div class="lobby-container">
        <div class="quizzi-logo">QUIZZI</div>

        <p style="font-size:1.1rem; opacity:0.8; margin-bottom:0.5rem;">Join at: <strong>quizzi/play</strong> or use the Quizzi Android app</p>

        <p style="font-size:1.1rem; opacity:0.7;">Game PIN</p>
        <div class="game-pin-display" id="pinDisplay"></div>

        <div class="qr-container" id="qrCode"></div>

        <p style="font-size:1.1rem; margin-top:1rem;">
            <span id="playerCount">0</span> player(s) joined
        </p>

        <div class="player-list" id="playerList"></div>

        <button class="btn btn-green btn-lg" id="startBtn" onclick="startGame()" style="margin-top:1.5rem;">
            Start Game
        </button>
    </div>

    <script>
        const params = new URLSearchParams(window.location.search);
        const sessionId = parseInt(params.get('sessionId'));
        const pin = params.get('pin');

        document.getElementById('pinDisplay').textContent = pin.replace(/(\d{3})(\d{3})/, '$1 $2');

        // Generate QR code pointing to the web player
        const baseUrl = window.location.origin;
        const playUrl = baseUrl + '/quizzi/play/?pin=' + pin;
        new QRCode(document.getElementById('qrCode'), {
            text: playUrl,
            width: 180,
            height: 180,
            colorDark: '#1A1A2E',
            colorLight: '#ffffff',
            correctLevel: QRCode.CorrectLevel.M
        });

        QuizziPoll.start(sessionId, null, {
            onUpdate(data) {
                document.getElementById('playerCount').textContent = data.playerCount;
                if (data.players) {
                    const list = document.getElementById('playerList');
                    list.innerHTML = '';
                    data.players.forEach(name => {
                        const chip = document.createElement('span');
                        chip.className = 'player-chip';
                        chip.textContent = name;
                        list.appendChild(chip);
                    });
                }
            },
            onStateChange(data) {
                if (data.status === 'showing_question') {
                    QuizziPoll.stop();
                    window.location.href = 'host-game.jsp?sessionId=' + sessionId;
                }
            }
        });

        async function startGame() {
            const resp = await fetch('/quizzi/api/next?sessionId=' + sessionId + '&action=next_question', { method: 'POST' });
            const data = await resp.json();
            if (data.status === 'ok') {
                QuizziPoll.stop();
                window.location.href = 'host-game.jsp?sessionId=' + sessionId;
            }
        }
    </script>
</body>
</html>
