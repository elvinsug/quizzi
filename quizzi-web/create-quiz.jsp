<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quizzi — Create Quiz</title>
    <link rel="icon" type="image/png" href="images/favicon.png">
    <link rel="stylesheet" href="css/quizzi.css">
    <style>
        .question-list { margin-top: 1.5rem; }
        .question-editor .remove-btn {
            position: absolute; top: 12px; right: 12px;
            background: var(--wrong-red); color: #fff; border: none;
            width: 28px; height: 28px; border-radius: 50%;
            cursor: pointer; font-size: 1rem; display: flex;
            align-items: center; justify-content: center;
        }
        .time-points-row { display: flex; gap: 1rem; margin-top: 0.75rem; flex-wrap: wrap; }
        .time-points-row select, .time-points-row input {
            padding: 0.5rem; border-radius: var(--radius-sm);
            border: 2px solid #E8E8E8; font-family: 'Manrope', sans-serif;
        }
        .drag-handle {
            cursor: grab; color: var(--text-light); font-size: 1.2rem;
            margin-right: 0.5rem; user-select: none;
        }
    </style>
</head>
<body>
    <nav class="top-nav">
        <a href="index.jsp" class="logo"><img src="images/logo.png" alt="Quizzi" class="logo-icon">Quizzi</a>
        <div class="nav-links">
            <a href="index.jsp">My Quizzes</a>
        </div>
        <div class="nav-right">
            <button class="btn btn-black" onclick="saveQuiz()">SAVE QUIZ</button>
        </div>
    </nav>

    <div class="dashboard">
        <div style="max-width:800px; margin:0 auto;">
            <h2 style="margin-bottom:1.5rem;">Create New Quiz</h2>

            <div class="card" style="max-width:100%; margin-bottom:1.5rem;">
                <div class="form-group">
                    <label>Quiz Title</label>
                    <input type="text" id="quizTitle" class="form-control" placeholder="e.g. Marvel Trivia" maxlength="255">
                </div>
                <div class="form-group" style="margin-bottom:0;">
                    <label>Description (optional)</label>
                    <textarea id="quizDesc" class="form-control" rows="2" placeholder="Brief description of this quiz" style="resize:vertical;"></textarea>
                </div>
            </div>

            <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:1rem;">
                <h3 id="questionCount" style="font-size:1.1rem;">Questions (0)</h3>
                <button class="btn btn-black btn-sm" onclick="addQuestion()">+ ADD QUESTION</button>
            </div>

            <div id="questionList" class="question-list"></div>
        </div>
    </div>

    <script>
        let questions = [];

        function addQuestion() {
            questions.push({
                questionText: '', optionA: '', optionB: '', optionC: '', optionD: '',
                correctAnswer: 'a', timeLimit: 20, points: 1000
            });
            renderQuestions();
            const idx = questions.length - 1;
            document.getElementById('qText_' + idx).focus();
        }

        function removeQuestion(idx) {
            questions.splice(idx, 1);
            renderQuestions();
        }

        function renderQuestions() {
            document.getElementById('questionCount').textContent = 'Questions (' + questions.length + ')';
            const container = document.getElementById('questionList');
            container.innerHTML = '';

            questions.forEach((q, i) => {
                const div = document.createElement('div');
                div.className = 'question-editor';
                div.innerHTML = `
                    <button class="remove-btn" onclick="removeQuestion(${i})" title="Remove">&times;</button>
                    <div style="display:flex; align-items:center; margin-bottom:0.75rem;">
                        <span class="drag-handle">&#9776;</span>
                        <span class="step-indicator">Q${i + 1}</span>
                    </div>
                    <div class="form-group" style="margin-bottom:0.75rem;">
                        <input type="text" id="qText_${i}" class="form-control" placeholder="Enter question text"
                               value="${escAttr(q.questionText)}" onchange="updateQ(${i},'questionText',this.value)" maxlength="500">
                    </div>
                    <div class="answer-options">
                        <div class="answer-option-input opt-a">
                            <input type="radio" name="correct_${i}" class="correct-radio" value="a" ${q.correctAnswer==='a'?'checked':''}
                                   onchange="updateQ(${i},'correctAnswer','a')">
                            <input type="text" placeholder="Option A" value="${escAttr(q.optionA)}"
                                   onchange="updateQ(${i},'optionA',this.value)" maxlength="200">
                        </div>
                        <div class="answer-option-input opt-b">
                            <input type="radio" name="correct_${i}" class="correct-radio" value="b" ${q.correctAnswer==='b'?'checked':''}
                                   onchange="updateQ(${i},'correctAnswer','b')">
                            <input type="text" placeholder="Option B" value="${escAttr(q.optionB)}"
                                   onchange="updateQ(${i},'optionB',this.value)" maxlength="200">
                        </div>
                        <div class="answer-option-input opt-c">
                            <input type="radio" name="correct_${i}" class="correct-radio" value="c" ${q.correctAnswer==='c'?'checked':''}
                                   onchange="updateQ(${i},'correctAnswer','c')">
                            <input type="text" placeholder="Option C (optional)" value="${escAttr(q.optionC)}"
                                   onchange="updateQ(${i},'optionC',this.value)" maxlength="200">
                        </div>
                        <div class="answer-option-input opt-d">
                            <input type="radio" name="correct_${i}" class="correct-radio" value="d" ${q.correctAnswer==='d'?'checked':''}
                                   onchange="updateQ(${i},'correctAnswer','d')">
                            <input type="text" placeholder="Option D (optional)" value="${escAttr(q.optionD)}"
                                   onchange="updateQ(${i},'optionD',this.value)" maxlength="200">
                        </div>
                    </div>
                    <div class="time-points-row">
                        <label style="font-size:0.85rem; font-weight:600;">Time:
                            <select onchange="updateQ(${i},'timeLimit',parseInt(this.value))">
                                <option value="10" ${q.timeLimit===10?'selected':''}>10s</option>
                                <option value="15" ${q.timeLimit===15?'selected':''}>15s</option>
                                <option value="20" ${q.timeLimit===20?'selected':''}>20s</option>
                                <option value="30" ${q.timeLimit===30?'selected':''}>30s</option>
                            </select>
                        </label>
                        <label style="font-size:0.85rem; font-weight:600;">Points:
                            <select onchange="updateQ(${i},'points',parseInt(this.value))">
                                <option value="1000" ${q.points===1000?'selected':''}>Standard (1000)</option>
                                <option value="2000" ${q.points===2000?'selected':''}>Double (2000)</option>
                            </select>
                        </label>
                    </div>
                `;
                container.appendChild(div);
            });
        }

        function updateQ(idx, field, value) { questions[idx][field] = value; }

        async function saveQuiz() {
            const title = document.getElementById('quizTitle').value.trim();
            if (!title) { alert('Please enter a quiz title.'); return; }
            if (questions.length === 0) { alert('Add at least one question.'); return; }

            for (let i = 0; i < questions.length; i++) {
                if (!questions[i].questionText.trim()) { alert('Question ' + (i+1) + ' is missing text.'); return; }
                if (!questions[i].optionA.trim() || !questions[i].optionB.trim()) { alert('Question ' + (i+1) + ' needs at least options A and B.'); return; }
            }

            try {
                const resp = await fetch('/quizzi/api/quiz', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        title, description: document.getElementById('quizDesc').value.trim(), questions
                    })
                });
                const data = await resp.json();
                if (data.status === 'ok') { window.location.href = 'index.jsp'; }
                else { alert('Error: ' + data.message); }
            } catch (e) { alert('Failed to save quiz: ' + e.message); }
        }

        function escAttr(s) {
            return (s || '').replace(/&/g,'&amp;').replace(/"/g,'&quot;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
        }
    </script>
</body>
</html>
