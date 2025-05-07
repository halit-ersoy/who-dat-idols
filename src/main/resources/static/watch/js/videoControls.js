// videoControls.js
export function initVideoControls(videoId) {
    const SKIP_SECONDS     = 10;
    const videoPlayer      = document.getElementById('videoPlayer');
    const videoSource      = document.getElementById('videoSource');
    const volumeControl    = document.getElementById('volumeControl');
    const backwardBtn      = document.getElementById('backwardButton');
    const forwardBtn       = document.getElementById('forwardButton');
    const playPauseWrapper = document.querySelector('.video-wrapper');
    const titleEl          = document.getElementById('title');
    const infoEl           = document.getElementById('videoInfo');
    const prevEpisodeBtn   = document.getElementById('prevEpisode');
    const nextEpisodeBtn   = document.getElementById('nextEpisode');

    if (!videoPlayer || !videoSource || !volumeControl || !playPauseWrapper) {
        console.error('Gerekli video oynatıcı elementleri bulunamadı.');
        return;
    }

    hideNativeVolumeControls();
    const playButton = createPlayPauseButton();
    setupVolumeControl();
    setupSkipButtons();
    setupKeyboardShortcuts();
    syncVolumeOnMetadata();
    setupViewCount();
    setupPlaybackEvents();
    setupErrorHandling();
    loadVideo(videoId);
    setupEpisodeNavigation(videoId);
    disableNativeShortcutsOnVideo();

    // --- Fonksiyonlar ---

    function hideNativeVolumeControls() {
        const style = document.createElement('style');
        style.textContent = `
      video::-webkit-media-controls-volume-slider,
      video::-webkit-media-controls-mute-button {
        display: none !important;
      }
    `;
        document.head.appendChild(style);
    }

    function createPlayPauseButton() {
        const btn = document.createElement('div');
        btn.className = 'play-pause-button';
        btn.innerHTML = '<i class="fas fa-play"></i>';
        playPauseWrapper.appendChild(btn);

        btn.addEventListener('click', () => {
            videoPlayer.paused ? videoPlayer.play() : videoPlayer.pause();
            updatePlayIcon();
        });

        return btn;
    }

    function updatePlayIcon() {
        playButton.innerHTML = videoPlayer.paused
            ? '<i class="fas fa-play"></i>'
            : '<i class="fas fa-pause"></i>';
    }

    function setupVolumeControl() {
        volumeControl.addEventListener('input', () => {
            videoPlayer.volume = volumeControl.value;
            updateVolumeIcon(volumeControl.value);
            updateVolumeSliderBackground();
        });
    }

    function updateVolumeIcon(volume) {
        const icon = volumeControl.previousElementSibling;
        if (!icon) return;
        icon.className = volume == 0
            ? 'fas fa-volume-mute'
            : volume < 0.5
                ? 'fas fa-volume-down'
                : 'fas fa-volume-up';
    }

    function updateVolumeSliderBackground() {
        const pct = volumeControl.value * 100;
        volumeControl.style.background = `
      linear-gradient(to right,
        #1ed760 0%, #1ed760 ${pct}%,
        #555 ${pct}%, #555 100%)
    `;
    }

    function setupSkipButtons() {
        backwardBtn?.addEventListener('click', () => skipVideo(-SKIP_SECONDS));
        forwardBtn?.addEventListener('click', () => skipVideo(SKIP_SECONDS));
    }

    function skipVideo(sec) {
        if (isNaN(videoPlayer.duration)) return;
        videoPlayer.currentTime = Math.max(0,
            Math.min(videoPlayer.duration, videoPlayer.currentTime + sec)
        );
    }

    function setupKeyboardShortcuts() {
        window.addEventListener('keydown', e => {
            const tag = document.activeElement.tagName;
            if (tag === 'INPUT' || tag === 'TEXTAREA') return;
            if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') {
                e.preventDefault();
                skipVideo(e.key === 'ArrowLeft' ? -SKIP_SECONDS : SKIP_SECONDS);
                console.log(`Atlama: ${videoPlayer.currentTime}s`);
            }
        });
    }

    function syncVolumeOnMetadata() {
        videoPlayer.addEventListener('loadedmetadata', () => {
            setTimeout(() => {
                volumeControl.value = videoPlayer.volume;
                updateVolumeIcon(videoPlayer.volume);
                updateVolumeSliderBackground();
            }, 100);
        });
    }

    function setupViewCount() {
        let firstPlay = true;
        videoPlayer.addEventListener('play', () => {
            if (firstPlay && videoId) {
                incrementViewCount(videoId);
                firstPlay = false;
            }
            updatePlayIcon();
        });
    }

    async function incrementViewCount(id) {
        try {
            const res = await fetch(`/api/video/increment-view?id=${id}`, { method: 'POST' });
            if (!res.ok) console.error('View count arttırılamadı');
        } catch (err) {
            console.error('View count hatası:', err);
        }
    }

    function setupPlaybackEvents() {
        videoPlayer.addEventListener('playing', () => videoPlayer.classList.add('playing'));
        videoPlayer.addEventListener('pause',   updatePlayIcon);
    }

    function setupErrorHandling() {
        videoPlayer.addEventListener('error', () => {
            if (titleEl) titleEl.innerText = 'Video yüklenirken hata oluştu.';
            if (infoEl)  infoEl.innerText  = 'Video bulunamadı veya oynatılamıyor.';
        });
    }

    function loadVideo(id) {
        if (!id) {
            if (titleEl) titleEl.innerText = 'Video bulunamadı';
            if (infoEl)  infoEl.innerText  = 'Geçerli bir ID girilmedi.';
            videoPlayer.style.display = 'none';
            return;
        }
        videoSource.src = `/media/video/${id}`;
        videoPlayer.load();
        titleEl.innerText = `Video ${id}`;
        infoEl .innerText = `Video ID: ${id}`;
    }

    function setupEpisodeNavigation(id) {
        prevEpisodeBtn?.addEventListener('click', () => {
            if (+id > 1) window.location.search = `?id=${+id - 1}`;
        });
        if (prevEpisodeBtn) prevEpisodeBtn.disabled = +id <= 1;

        nextEpisodeBtn?.addEventListener('click', () => {
            window.location.search = `?id=${+id + 1}`;
        });
    }

    function disableNativeShortcutsOnVideo() {
        videoPlayer.addEventListener('keydown', e => {
            if (['ArrowLeft','ArrowRight'].includes(e.key)) {
                e.preventDefault();
                e.stopPropagation();
            }
        }, true);
    }
}
