// Shared Quizee Navbar Dual-Music Controller (Single Tap: Music 1 / Pause | Double Tap: Next Music - Vocalise 2)
(function () {
    'use strict';

    const TRACKS = [
        {
            id: 1,
            name: 'Quizee Theme',
            shortName: 'Music 1',
            src: 'assets/quzeaudio.mp3',
            badge: '1'
        },
        {
            id: 2,
            name: 'Vocalise 2',
            shortName: 'Vocalise 2',
            src: 'assets/Vocalise2.mp3',
            badge: '2'
        }
    ];

    const ICON_PLAY = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18V5l12-2v13"></path><circle cx="6" cy="18" r="3"></circle><circle cx="18" cy="16" r="3"></circle></svg>';
    const ICON_ACTIVE = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon><path d="M15.54 8.46a5 5 0 0 1 0 7.07"></path><path d="M19.07 4.93a10 10 0 0 1 0 14.14"></path></svg>';

    let currentTrackIndex = 0;
    let clickCount = 0;
    let clickTimeout = null;
    let toastTimeout = null;
    const TAP_THRESHOLD_MS = 280;

    function showAudioToast(mainText, subText) {
        let toast = document.getElementById('quizee-audio-toast');
        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'quizee-audio-toast';
            document.body.appendChild(toast);
        }

        toast.innerHTML = `
            <div class="audio-toast-main">${mainText}</div>
            ${subText ? `<div class="audio-toast-sub">${subText}</div>` : ''}
        `;

        toast.classList.add('visible');

        if (toastTimeout) clearTimeout(toastTimeout);
        toastTimeout = setTimeout(() => {
            toast.classList.remove('visible');
        }, 2200);
    }

    function initAudio() {
        const audio = document.getElementById('quizeAudioPlayer');
        const audioBtns = document.querySelectorAll('.quize-audio-btn');
        if (!audio || !audioBtns.length) return;

        // Ensure buttons have badge element for track number
        audioBtns.forEach((btn) => {
            if (!btn.querySelector('.audio-btn-badge')) {
                const badge = document.createElement('span');
                badge.className = 'audio-btn-badge';
                badge.textContent = '1';
                btn.appendChild(badge);
            }
        });

        function setAudioState(isPlaying) {
            const track = TRACKS[currentTrackIndex];
            audioBtns.forEach((btn) => {
                const icon = btn.querySelector('.audio-btn-icon');
                const text = btn.querySelector('.audio-btn-text');
                const badge = btn.querySelector('.audio-btn-badge');

                if (badge) {
                    badge.textContent = track.badge;
                }

                if (isPlaying) {
                    btn.classList.add('is-playing');
                    if (icon) icon.innerHTML = ICON_ACTIVE;
                    if (text) text.textContent = track.shortName;
                    btn.title = `Playing: ${track.name} (Tap: Pause | Double tap: Next Music)`;
                } else {
                    btn.classList.remove('is-playing');
                    if (icon) icon.innerHTML = ICON_PLAY;
                    if (text) text.textContent = 'Music';
                    btn.title = 'Play Music (Tap: Music 1 | Double tap: Vocalise 2)';
                }
            });
        }

        async function playTrack(index, showToastNotification = true) {
            if (index < 0 || index >= TRACKS.length) index = 0;
            const track = TRACKS[index];
            const isDifferentTrack = (currentTrackIndex !== index) || (!audio.currentSrc || !audio.currentSrc.includes(track.src));

            currentTrackIndex = index;

            if (isDifferentTrack) {
                const sourceEl = audio.querySelector('source');
                if (sourceEl) {
                    sourceEl.src = track.src;
                }
                audio.src = track.src;
                audio.load();
            }

            try {
                await audio.play();
                setAudioState(true);
                if (showToastNotification) {
                    const otherTrack = TRACKS[(index === 0) ? 1 : 0];
                    showAudioToast(`🎵 Playing: ${track.name}`, `Tap: Pause • Double tap: ${otherTrack.shortName}`);
                }
            } catch (err) {
                console.warn('[Quizee Audio] Play failed, attempting reload:', err);
                try {
                    audio.load();
                    await audio.play();
                    setAudioState(true);
                    if (showToastNotification) {
                        showAudioToast(`🎵 Playing: ${track.name}`, 'Tap: Pause • Double tap: Next Music');
                    }
                } catch (retryErr) {
                    console.error('[Quizee Audio] Playback failed completely:', retryErr);
                    setAudioState(false);
                    showAudioToast('⚠️ Tap to allow music playback', 'Browser requires interaction');
                }
            }
        }

        function pauseAudio() {
            audio.pause();
            setAudioState(false);
            showAudioToast('⏸ Music Paused', 'Tap: Play Music 1 • Double tap: Vocalise 2');
        }

        function handleSingleTap() {
            if (!audio.paused) {
                pauseAudio();
            } else {
                playTrack(0);
            }
        }

        function handleDoubleTap() {
            let nextIndex = 1;
            if (!audio.paused) {
                nextIndex = (currentTrackIndex === 0) ? 1 : 0;
            } else {
                nextIndex = 1;
            }
            playTrack(nextIndex);
        }

        function handleButtonClick(e) {
            if (e) {
                e.preventDefault();
                e.stopPropagation();
            }

            clickCount++;

            if (clickCount === 1) {
                clickTimeout = setTimeout(() => {
                    clickCount = 0;
                    handleSingleTap();
                }, TAP_THRESHOLD_MS);
            } else if (clickCount >= 2) {
                clearTimeout(clickTimeout);
                clickCount = 0;
                handleDoubleTap();
            }
        }

        audio.addEventListener('ended', () => {
            const nextIdx = (currentTrackIndex + 1) % TRACKS.length;
            playTrack(nextIdx, true);
        });

        audio.addEventListener('pause', () => setAudioState(false));
        audio.addEventListener('play', () => setAudioState(true));
        audio.addEventListener('error', (err) => {
            console.warn('[Quizee Audio] Audio player event error:', err);
            setAudioState(false);
        });

        audioBtns.forEach((btn) => {
            btn.addEventListener('click', handleButtonClick);
            btn.addEventListener('dblclick', (e) => {
                e.preventDefault();
                e.stopPropagation();
            });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAudio);
    } else {
        initAudio();
    }
})();
