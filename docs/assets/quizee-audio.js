// Shared Quizee Navbar Music Controller (all pages)
(function () {
    'use strict';

    const ICON_PLAY = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18V5l12-2v13"></path><circle cx="6" cy="18" r="3"></circle><circle cx="18" cy="16" r="3"></circle></svg>';
    const ICON_ACTIVE = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon><path d="M15.54 8.46a5 5 0 0 1 0 7.07"></path><path d="M19.07 4.93a10 10 0 0 1 0 14.14"></path></svg>';

    function initAudio() {
        const audio = document.getElementById('quizeAudioPlayer');
        const audioBtns = document.querySelectorAll('.quize-audio-btn');
        if (!audio || !audioBtns.length) return;

        function setAudioState(isPlaying) {
            audioBtns.forEach((btn) => {
                const icon = btn.querySelector('.audio-btn-icon');
                const text = btn.querySelector('.audio-btn-text');

                if (isPlaying) {
                    btn.classList.add('is-playing');
                    if (icon) icon.innerHTML = ICON_ACTIVE;
                    if (text) text.textContent = 'Playing';
                    btn.title = 'Pause Music';
                } else {
                    btn.classList.remove('is-playing');
                    if (icon) icon.innerHTML = ICON_PLAY;
                    if (text) text.textContent = 'Music';
                    btn.title = 'Play Quizee Music';
                }
            });
        }

        async function toggleAudio(e) {
            if (e) {
                e.preventDefault();
                e.stopPropagation();
            }

            if (audio.paused) {
                try {
                    const playPromise = audio.play();
                    if (playPromise !== undefined) {
                        await playPromise;
                        setAudioState(true);
                    }
                } catch (playErr) {
                    console.warn('[Quizee Audio] Initial play failed, attempting audio.load():', playErr);
                    try {
                        audio.load();
                        await audio.play();
                        setAudioState(true);
                    } catch (retryErr) {
                        console.error('[Quizee Audio] Playback failed completely:', retryErr);
                        setAudioState(false);
                    }
                }
            } else {
                audio.pause();
                setAudioState(false);
            }
        }

        audio.addEventListener('ended', () => setAudioState(false));
        audio.addEventListener('pause', () => setAudioState(false));
        audio.addEventListener('play', () => setAudioState(true));
        audio.addEventListener('error', (err) => {
            console.warn('[Quizee Audio] Audio element error:', err);
            setAudioState(false);
        });

        audioBtns.forEach((btn) => {
            btn.addEventListener('click', toggleAudio);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAudio);
    } else {
        initAudio();
    }
})();
