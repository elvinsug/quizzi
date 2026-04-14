<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quizzi — Welcome</title>
    <link rel="icon" type="image/png" href="images/favicon.png">
    <link rel="stylesheet" href="css/quizzi.css">
</head>
<body>
    <div class="auth-container">
        <div class="auth-card" id="welcomeView">
            <img src="images/logo.png" alt="Quizzi" class="quizzi-logo-img">
            <div class="quizzi-logo" style="margin:0 0 0.5rem; font-size:2.2rem;">QUIZZI</div>
            <p style="margin-bottom:2.5rem;">Create and play live quizzes with friends</p>

            <button class="btn btn-black btn-lg btn-block" onclick="showView('loginView')" style="margin-bottom:0.75rem;">LOG IN</button>
            <button class="btn btn-outline btn-lg btn-block" onclick="showView('signupView')" style="margin-bottom:1.5rem;">SIGN UP</button>

            <div class="auth-divider">or</div>

            <button class="btn btn-white btn-lg btn-block" onclick="continueAsGuest()" style="margin-top:1.5rem;">CONTINUE AS GUEST</button>
        </div>

        <div class="auth-card hidden" id="loginView">
            <h1>Log In</h1>
            <p>Welcome back to Quizzi</p>

            <div class="form-group" style="text-align:left;">
                <label>Username</label>
                <input type="text" id="loginUsername" class="form-control" placeholder="Your username" autocomplete="username">
            </div>
            <div class="form-group" style="text-align:left;">
                <label>Password</label>
                <input type="password" id="loginPassword" class="form-control" placeholder="Your password" autocomplete="current-password">
            </div>

            <div id="loginError" class="hidden" style="color:var(--wrong-red); font-weight:700; font-size:0.9rem; margin-bottom:1rem;"></div>

            <button class="btn btn-black btn-lg btn-block" id="loginBtn" onclick="doLogin()">LOG IN</button>

            <p style="margin-top:1.5rem; color:var(--text-muted); font-size:0.9rem;">
                Don't have an account? <span class="auth-link" onclick="showView('signupView')">Sign up</span>
            </p>
            <p style="margin-top:0.5rem;">
                <span class="auth-link" onclick="showView('welcomeView')" style="font-size:0.85rem; color:var(--text-muted);">Back</span>
            </p>
        </div>

        <div class="auth-card hidden" id="signupView">
            <h1>Sign Up</h1>
            <p>Create your Quizzi account</p>

            <div class="form-group" style="text-align:left;">
                <label>Username</label>
                <input type="text" id="signupUsername" class="form-control" placeholder="Choose a username" autocomplete="username" maxlength="50">
            </div>
            <div class="form-group" style="text-align:left;">
                <label>Display Name</label>
                <input type="text" id="signupDisplayName" class="form-control" placeholder="Your display name (optional)" maxlength="100">
            </div>
            <div class="form-group" style="text-align:left;">
                <label>Password</label>
                <input type="password" id="signupPassword" class="form-control" placeholder="Choose a password" autocomplete="new-password">
            </div>

            <div id="signupError" class="hidden" style="color:var(--wrong-red); font-weight:700; font-size:0.9rem; margin-bottom:1rem;"></div>

            <button class="btn btn-black btn-lg btn-block" id="signupBtn" onclick="doSignup()">SIGN UP</button>

            <p style="margin-top:1.5rem; color:var(--text-muted); font-size:0.9rem;">
                Already have an account? <span class="auth-link" onclick="showView('loginView')">Log in</span>
            </p>
            <p style="margin-top:0.5rem;">
                <span class="auth-link" onclick="showView('welcomeView')" style="font-size:0.85rem; color:var(--text-muted);">Back</span>
            </p>
        </div>
    </div>

    <script>
        function showView(id) {
            ['welcomeView','loginView','signupView'].forEach(v =>
                document.getElementById(v).classList.add('hidden'));
            document.getElementById(id).classList.remove('hidden');
        }

        function continueAsGuest() {
            window.location.href = 'index.jsp';
        }

        async function doLogin() {
            const username = document.getElementById('loginUsername').value.trim();
            const password = document.getElementById('loginPassword').value;
            const errEl = document.getElementById('loginError');
            errEl.classList.add('hidden');

            if (!username || !password) {
                errEl.textContent = 'Please fill in all fields';
                errEl.classList.remove('hidden');
                return;
            }

            const btn = document.getElementById('loginBtn');
            btn.disabled = true;
            btn.textContent = 'LOGGING IN...';

            try {
                const resp = await fetch('/quizzi/api/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password })
                });
                const data = await resp.json();
                if (data.status === 'ok') {
                    window.location.href = 'index.jsp';
                } else {
                    errEl.textContent = data.message || 'Login failed';
                    errEl.classList.remove('hidden');
                }
            } catch (e) {
                errEl.textContent = 'Connection error';
                errEl.classList.remove('hidden');
            }
            btn.disabled = false;
            btn.textContent = 'LOG IN';
        }

        async function doSignup() {
            const username = document.getElementById('signupUsername').value.trim();
            const displayName = document.getElementById('signupDisplayName').value.trim();
            const password = document.getElementById('signupPassword').value;
            const errEl = document.getElementById('signupError');
            errEl.classList.add('hidden');

            if (!username || !password) {
                errEl.textContent = 'Username and password are required';
                errEl.classList.remove('hidden');
                return;
            }

            const btn = document.getElementById('signupBtn');
            btn.disabled = true;
            btn.textContent = 'CREATING ACCOUNT...';

            try {
                const resp = await fetch('/quizzi/api/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password, displayName })
                });
                const data = await resp.json();
                if (data.status === 'ok') {
                    window.location.href = 'index.jsp';
                } else {
                    errEl.textContent = data.message || 'Sign up failed';
                    errEl.classList.remove('hidden');
                }
            } catch (e) {
                errEl.textContent = 'Connection error';
                errEl.classList.remove('hidden');
            }
            btn.disabled = false;
            btn.textContent = 'SIGN UP';
        }

        document.getElementById('loginPassword').addEventListener('keydown', e => {
            if (e.key === 'Enter') doLogin();
        });
        document.getElementById('signupPassword').addEventListener('keydown', e => {
            if (e.key === 'Enter') doSignup();
        });
    </script>
</body>
</html>
