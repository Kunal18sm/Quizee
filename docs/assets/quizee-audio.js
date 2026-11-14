// Shared Quizee Navbar Music Player Controller (Compact 3-Button Mini Remote: Prev, Play/Pause, Next)
(function () {
    'use strict';

    const TRACKS = [
        {
            id: 1,
            title: 'Quizee Theme',
            src: 'assets/quzeaudio.mp3'
        },
        {
            id: 2,
            title: 'Vocalise 2',
            src: 'assets/Vocalise2.mp3'
        }
    ];

    const ICON_PLAY = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18V5l12-2v13"></path><circle cx="6" cy="18" r="3"></circle><circle cx="18" cy="16" r="3"></circle></svg>';
    const ICON_ACTIVE = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon><path d="M15.54 8.46a5 5 0 0 1 0 7.07"></path><path d="M19.07 4.93a10 10 0 0 1 0 14.14"></path></svg>';

    let currentTrackIndex = 0;
    let popoverEl = null;

    function createPopover() {
        if (document.getElementById('quizee-music-popover')) {
            popoverEl = document.getElementById('quizee-music-popover');
            return popoverEl;
        }

        const pop = document.createElement('div');
        pop.id = 'quizee-music-popover';
        pop.className = 'quizee-music-popover';
        pop.setAttribute('role', 'toolbar');
        pop.setAttribute('aria-label', 'Music Controls');

        pop.innerHTML = `
            <div class="qmp-controls">
                <button type="button" class="qmp-btn qmp-btn-prev" id="qmpBtnPrev" title="Previous Track" aria-label="Previous Track">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polygon points="19 20 9 12 19 4 19 20"></polygon><line x1="5" y1="19" x2="5" y2="5"></line></svg>
                </button>

                <button type="button" class="qmp-btn qmp-btn-play" id="qmpBtnPlay" title="Play / Pause" aria-label="Play / Pause">
                    <svg class="qmp-icon-play" width="15" height="15" viewBox="0 0 24 24" fill="currentColor"><polygon points="6 4 20 12 6 20 6 4"></polygon></svg>
                    <svg class="qmp-icon-pause" width="15" height="15" viewBox="0 0 24 24" fill="currentColor" style="display:none;"><rect x="6" y="4" width="4" height="16"></rect><rect x="14" y="4" width="4" height="16"></rect></svg>
                </button>

                <button type="button" class="qmp-btn qmp-btn-next" id="qmpBtnNext" title="Next Track" aria-label="Next Track">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polygon points="5 4 15 12 5 20 5 4"></polygon><line x1="19" y1="5" x2="19" y2="19"></line></svg>
                </button>
            </div>
        `;

        document.body.appendChild(pop);
        popoverEl = pop;
        return pop;
    }

    function initAudio() {
        const audio = document.getElementById('quizeAudioPlayer');
        const audioBtns = document.querySelectorAll('.quize-audio-btn');
        if (!audio || !audioBtns.length) return;

        createPopover();

        const btnPrev = document.getElementById('qmpBtnPrev');
        const btnPlay = document.getElementById('qmpBtnPlay');
        const btnNext = document.getElementById('qmpBtnNext');
        const iconPlay = btnPlay ? btnPlay.querySelector('.qmp-icon-play') : null;
        const iconPause = btnPlay ? btnPlay.querySelector('.qmp-icon-pause') : null;

        function updatePopoverUI() {
            const isPlaying = !audio.paused;
            const track = TRACKS[currentTrackIndex];

            if (iconPlay && iconPause) {
                if (isPlaying) {
                    iconPlay.style.display = 'none';
                    iconPause.style.display = 'block';
                    btnPlay.title = `Pause (${track.title})`;
                } else {
                    iconPlay.style.display = 'block';
                    iconPause.style.display = 'none';
                    btnPlay.title = `Play (${track.title})`;
                }
            }

            if (btnNext) {
                const nextTrack = TRACKS[(currentTrackIndex + 1) % TRACKS.length];
                btnNext.title = `Next: ${nextTrack.title}`;
            }

            if (btnPrev) {
                const prevTrack = TRACKS[(currentTrackIndex - 1 + TRACKS.length) % TRACKS.length];
                btnPrev.title = `Previous: ${prevTrack.title}`;
            }
        }

        function setAudioState(isPlaying) {
            const track = TRACKS[currentTrackIndex];
            audioBtns.forEach((btn) => {
                const icon = btn.querySelector('.audio-btn-icon');
                const text = btn.querySelector('.audio-btn-text');

                if (isPlaying) {
                    btn.classList.add('is-playing');
                    if (icon) icon.innerHTML = ICON_ACTIVE;
                    if (text) text.textContent = track.title;
                    btn.title = `Playing: ${track.title} (Click to open controls)`;
                } else {
                    btn.classList.remove('is-playing');
                    if (icon) icon.innerHTML = ICON_PLAY;
                    if (text) text.textContent = 'Music';
                    btn.title = 'Play Quizee Music (Click to open controls)';
                }
            });

            updatePopoverUI();
        }

        async function playTrack(index) {
            if (index < 0) index = TRACKS.length - 1;
            if (index >= TRACKS.length) index = 0;

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

            updatePopoverUI();

            try {
                await audio.play();
                setAudioState(true);
            } catch (err) {
                console.warn('[Quizee Audio] Play failed, attempting reload:', err);
                try {
                    audio.load();
                    await audio.play();
                    setAudioState(true);
                } catch (retryErr) {
                    console.error('[Quizee Audio] Playback failed completely:', retryErr);
                    setAudioState(false);
                }
            }
        }

        function pauseAudio() {
            audio.pause();
            setAudioState(false);
        }

        function togglePlayPause() {
            if (audio.paused) {
                playTrack(currentTrackIndex);
            } else {
                pauseAudio();
            }
        }

        function playNextTrack() {
            const nextIdx = (currentTrackIndex + 1) % TRACKS.length;
            playTrack(nextIdx);
        }

        function playPrevTrack() {
            const prevIdx = (currentTrackIndex - 1 + TRACKS.length) % TRACKS.length;
            playTrack(prevIdx);
        }

        function openPopover(anchorBtn) {
            updatePopoverUI();

            const rect = anchorBtn.getBoundingClientRect();
            popoverEl.style.display = 'inline-flex';
            const popoverWidth = popoverEl.offsetWidth || 116;
            const screenW = window.innerWidth;

            // Position vertically directly below the clicked button
            const topPos = rect.bottom + 6;
            popoverEl.style.top = `${topPos}px`;

            // Position horizontally: centered relative to anchor button
            let leftPos = rect.left + (rect.width / 2) - (popoverWidth / 2);
            if (leftPos < 8) leftPos = 8;
            if (leftPos + popoverWidth > screenW - 8) {
                leftPos = screenW - popoverWidth - 8;
            }

            popoverEl.style.left = `${leftPos}px`;
            popoverEl.style.right = 'auto';

            // Force reflow for animation
            void popoverEl.offsetWidth;
            popoverEl.classList.add('is-open');

            // If audio was paused, auto-play current track on open
            if (audio.paused) {
                playTrack(currentTrackIndex);
            }
        }

        function closePopover() {
            if (!popoverEl || !popoverEl.classList.contains('is-open')) return;
            popoverEl.classList.remove('is-open');
            setTimeout(() => {
                if (!popoverEl.classList.contains('is-open')) {
                    popoverEl.style.display = 'none';
                }
            }, 140);
        }

        function togglePopover(anchorBtn) {
            if (popoverEl.classList.contains('is-open')) {
                closePopover();
            } else {
                openPopover(anchorBtn);
            }
        }

        // Attach Navbar Audio Buttons
        audioBtns.forEach((btn) => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                togglePopover(btn);
            });
        });

        // 3 Real Player Buttons
        if (btnPrev) {
            btnPrev.addEventListener('click', (e) => {
                e.stopPropagation();
                playPrevTrack();
            });
        }

        if (btnPlay) {
            btnPlay.addEventListener('click', (e) => {
                e.stopPropagation();
                togglePlayPause();
            });
        }

        if (btnNext) {
            btnNext.addEventListener('click', (e) => {
                e.stopPropagation();
                playNextTrack();
            });
        }

        audio.addEventListener('ended', () => {
            playNextTrack();
        });

        audio.addEventListener('pause', () => setAudioState(false));
        audio.addEventListener('play', () => setAudioState(true));
        audio.addEventListener('error', (err) => {
            console.warn('[Quizee Audio] Audio player event error:', err);
            setAudioState(false);
        });

        // Close popover when clicking anywhere outside
        document.addEventListener('click', (e) => {
            if (!popoverEl || !popoverEl.classList.contains('is-open')) return;
            if (popoverEl.contains(e.target)) return;

            let clickedNavbarBtn = false;
            audioBtns.forEach((btn) => {
                if (btn.contains(e.target)) clickedNavbarBtn = true;
            });

            if (!clickedNavbarBtn) {
                closePopover();
            }
        });

        // Close on Escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && popoverEl && popoverEl.classList.contains('is-open')) {
                closePopover();
            }
        });

        updatePopoverUI();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAudio);
    } else {
        initAudio();
    }
})();
