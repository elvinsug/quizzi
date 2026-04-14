/**
 * Quizzi — Shared Polling Engine
 *
 * Usage (host):
 *   QuizziPoll.start(sessionId, null, hostHandlers);
 *
 * Usage (player):
 *   QuizziPoll.start(sessionId, playerId, playerHandlers);
 */
const QuizziPoll = (() => {
    const INTERVAL = 1500;
    let timer = null;
    let lastStatus = null;
    let lastQuestionOrder = -1;

    function start(sessionId, playerId, handlers) {
        stop();
        poll(sessionId, playerId, handlers);
    }

    function stop() {
        if (timer) clearTimeout(timer);
        timer = null;
        lastStatus = null;
        lastQuestionOrder = -1;
    }

    async function poll(sessionId, playerId, handlers) {
        try {
            let url = `/quizzi/api/status?sessionId=${sessionId}`;
            if (playerId) url += `&playerId=${playerId}`;

            const resp = await fetch(url);
            const data = await resp.json();

            if (data.status === 'error') {
                if (handlers.onError) handlers.onError(data.message);
            } else {
                const statusChanged = data.status !== lastStatus;
                const questionChanged = data.currentQuestionOrder !== lastQuestionOrder;

                if (statusChanged || questionChanged) {
                    lastStatus = data.status;
                    lastQuestionOrder = data.currentQuestionOrder;
                    if (handlers.onStateChange) handlers.onStateChange(data);
                }

                if (handlers.onUpdate) handlers.onUpdate(data);
            }
        } catch (err) {
            console.error('Polling error:', err);
            if (handlers.onError) handlers.onError('Network error — retrying...');
        }

        timer = setTimeout(() => poll(sessionId, playerId, handlers), INTERVAL);
    }

    return { start, stop };
})();
