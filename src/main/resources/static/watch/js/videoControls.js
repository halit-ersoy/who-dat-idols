// videoControls.js
export function initVideoControls(videoId) {
    const SKIP_SECONDS = 10;
    const videoPlayer = document.getElementById('videoPlayer');
    const videoSource = document.getElementById('videoSource');
    const volumeControl = document.getElementById('volumeControl');
    const backwardBtn = document.getElementById('backwardButton');
    const forwardBtn = document.getElementById('forwardButton');
    const fullscreenBtn = document.getElementById('fullscreenButton');
    const playerContainer = document.getElementById('playerContainer');
    const playPauseWrapper = document.querySelector('.video-wrapper');
    const titleEl = document.getElementById('title');
    const infoEl = document.getElementById('videoInfo');
    const prevEpisodeBtn = document.getElementById('prevEpisode');
    const nextEpisodeBtn = document.getElementById('nextEpisode');

    if (!videoPlayer || !videoSource || !volumeControl || !playPauseWrapper) {
        console.error('Gerekli video oynatıcı elementleri bulunamadı.');
        return;
    }

    hideNativeVolumeControls();
    const playButton = createPlayPauseButton();
    setupVolumeControl();
    setupSkipButtons();
    setupFullscreenToggle();
    setupProgressBar();
    setupPlaybackSpeed();
    setupKeyboardShortcuts();
    syncVolumeOnMetadata();
    setupViewCount();
    setupPlaybackEvents();
    setupErrorHandling();
    loadVideo(videoId);
    loadSources(videoId);
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

        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            videoPlayer.paused ? videoPlayer.play() : videoPlayer.pause();
            updatePlayIcon();
        });

        // Add click listener to the whole video wrapper
        videoPlayer.addEventListener('click', () => {
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

    function setupFullscreenToggle() {
        if (!fullscreenBtn || !playerContainer) return;

        fullscreenBtn.addEventListener('click', toggleFullscreen);

        // Listen for fullscreen change events (standard and vendor-prefixed)
        document.addEventListener('fullscreenchange', updateFullscreenIcon);
        document.addEventListener('webkitfullscreenchange', updateFullscreenIcon);
        document.addEventListener('mozfullscreenchange', updateFullscreenIcon);
        document.addEventListener('MSFullscreenChange', updateFullscreenIcon);

        function toggleFullscreen() {
            if (!document.fullscreenElement &&    // standard
                !document.webkitFullscreenElement && // chrome, safari and opera
                !document.mozFullScreenElement &&    // firefox
                !document.msFullscreenElement) {     // ie/edge

                // Enter fullscreen
                if (playerContainer.requestFullscreen) {
                    playerContainer.requestFullscreen();
                } else if (playerContainer.webkitRequestFullscreen) {
                    playerContainer.webkitRequestFullscreen();
                } else if (playerContainer.mozRequestFullScreen) {
                    playerContainer.mozRequestFullScreen();
                } else if (playerContainer.msRequestFullscreen) {
                    playerContainer.msRequestFullscreen();
                }
            } else {
                // Exit fullscreen
                if (document.exitFullscreen) {
                    document.exitFullscreen();
                } else if (document.webkitExitFullscreen) {
                    document.webkitExitFullscreen();
                } else if (document.mozCancelFullScreen) {
                    document.mozCancelFullScreen();
                } else if (document.msExitFullscreen) {
                    document.msExitFullscreen();
                }
            }
        }

        function updateFullscreenIcon() {
            const isFullscreen = !!(document.fullscreenElement ||
                document.webkitFullscreenElement ||
                document.mozFullScreenElement ||
                document.msFullscreenElement);

            if (isFullscreen) {
                fullscreenBtn.innerHTML = '<i class="fas fa-compress"></i>';
                fullscreenBtn.title = "Tam Ekran Çık";
                playerContainer.classList.add('fullscreen-mode');
            } else {
                fullscreenBtn.innerHTML = '<i class="fas fa-expand"></i>';
                fullscreenBtn.title = "Tam Ekran";
                playerContainer.classList.remove('fullscreen-mode');
            }
        }
    }

    function setupProgressBar() {
        const progressContainer = document.getElementById('progressContainer');
        const progressBar = document.getElementById('progressBar');
        const timeDisplay = document.getElementById('timeDisplay');

        if (!progressContainer || !progressBar) return;

        const formatTime = (seconds) => {
            if (isNaN(seconds)) return "00:00";
            const mins = Math.floor(seconds / 60);
            const secs = Math.floor(seconds % 60);
            return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
        };

        const updateProgress = () => {
            const percentage = (videoPlayer.currentTime / videoPlayer.duration) * 100;
            progressBar.style.width = (percentage || 0) + '%';
            if (timeDisplay) {
                timeDisplay.innerText = `${formatTime(videoPlayer.currentTime)} / ${formatTime(videoPlayer.duration)}`;
            }
        };

        videoPlayer.addEventListener('timeupdate', updateProgress);
        videoPlayer.addEventListener('loadedmetadata', updateProgress);

        progressContainer.addEventListener('click', (e) => {
            const rect = progressContainer.getBoundingClientRect();
            const pos = (e.clientX - rect.left) / rect.width;
            videoPlayer.currentTime = pos * videoPlayer.duration;
        });

        // Optional: Dragging functionality
        let isDragging = false;
        progressContainer.addEventListener('mousedown', () => isDragging = true);
        window.addEventListener('mouseup', () => isDragging = false);
        window.addEventListener('mousemove', (e) => {
            if (isDragging) {
                const rect = progressContainer.getBoundingClientRect();
                let pos = (e.clientX - rect.left) / rect.width;
                pos = Math.max(0, Math.min(1, pos));
                videoPlayer.currentTime = pos * videoPlayer.duration;
            }
        });
    }

    function setupPlaybackSpeed() {
        const speedButton = document.getElementById('speedButton');
        const speedOptions = document.querySelectorAll('.speed-option');
        const speedOptionsContainer = document.querySelector('.speed-options');

        if (!speedButton) return;

        speedOptions.forEach(option => {
            option.addEventListener('click', (e) => {
                e.stopPropagation();
                const speed = parseFloat(option.getAttribute('data-speed'));
                videoPlayer.playbackRate = speed;
                speedButton.innerText = speed + 'x';

                // Update active state
                speedOptions.forEach(opt => opt.classList.remove('active'));
                option.classList.add('active');

                // Close menu after selection
                if (speedOptionsContainer) {
                    speedOptionsContainer.style.opacity = '0';
                    speedOptionsContainer.style.visibility = 'hidden';

                    // Reset visibility after a short delay so hover can work again
                    setTimeout(() => {
                        speedOptionsContainer.style.opacity = '';
                        speedOptionsContainer.style.visibility = '';
                    }, 500);
                }
            });
        });

        // Set default active
        speedOptions.forEach(opt => {
            if (opt.getAttribute('data-speed') === "1") {
                opt.classList.add('active');
            }
        });
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
        videoPlayer.addEventListener('playing', () => {
            videoPlayer.classList.add('playing');
            playPauseWrapper.classList.add('loaded');
        });
        videoPlayer.addEventListener('pause', updatePlayIcon);

        // Skeleton logic
        videoPlayer.addEventListener('waiting', () => {
            playPauseWrapper.classList.remove('loaded');
        });
        videoPlayer.addEventListener('canplay', () => {
            playPauseWrapper.classList.add('loaded');
        });
        videoPlayer.addEventListener('loadstart', () => {
            playPauseWrapper.classList.remove('loaded');
        });
    }

    function setupErrorHandling() {
        videoPlayer.addEventListener('error', () => {
            playPauseWrapper.classList.add('loaded'); // Hata durumunda skeleton'ı kaldır ki mesaj görünsün
            if (titleEl) titleEl.innerText = 'Video yüklenirken hata oluştu.';
            if (infoEl) infoEl.innerText = 'Video bulunamadı veya oynatılamıyor.';
        });
    }

    function loadVideo(id) {
        if (!id) {
            playPauseWrapper.classList.add('loaded'); // ID yoksa skeleton'ı kaldır
            if (titleEl) titleEl.innerText = 'Video bulunamadı';
            if (infoEl) infoEl.innerText = 'Geçerli bir ID girilmedi.';
            videoPlayer.style.display = 'none';
            return;
        }

        const hlsUrl = `/media/video/${id}/playlist.m3u8`;
        const mp4Url = `/media/video/${id}`;

        if (Hls.isSupported()) {
            const hls = new Hls({
                maxBufferLength: 30, // 30 seconds buffer
                maxMaxBufferLength: 60,
            });
            hls.loadSource(hlsUrl);
            hls.attachMedia(videoPlayer);
            hls.on(Hls.Events.MANIFEST_PARSED, () => {
                // HLS successfully loaded
                console.log('HLS loaded successfully');
            });
            hls.on(Hls.Events.ERROR, (event, data) => {
                if (data.fatal) {
                    console.warn('HLS fatal error, falling back to MP4:', data.type);
                    hls.destroy();
                    fallbackToMp4(mp4Url);
                }
            });
        } else if (videoPlayer.canPlayType('application/vnd.apple.mpegurl')) {
            // Native HLS support (Safari)
            videoPlayer.src = hlsUrl;
        } else {
            fallbackToMp4(mp4Url);
        }

        titleEl.innerText = `İçerik Yükleniyor...`;
        infoEl.innerText = `ID: ${id}`;
    }

    function fallbackToMp4(url) {
        videoSource.src = url;
        videoPlayer.load();
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
            if (['ArrowLeft', 'ArrowRight'].includes(e.key)) {
                e.preventDefault();
                e.stopPropagation();
            }
        }, true);
    }

    async function loadSources(id) {
        const switcher = document.getElementById('sourceSwitcher');
        if (!switcher) return;

        switcher.innerHTML = `
            <button class="source-btn active" id="btn-hls">
                <i class="fas fa-bolt"></i> Ana Kaynak (HLS)
            </button>
        `;

        document.getElementById('btn-hls').onclick = () => switchToHls();

        try {
            console.log("DEBUG: Loading sources for contentId:", id);
            const res = await fetch(`/media/video/${id}/sources`);
            const sources = await res.json();
            console.log("DEBUG: Sources received:", sources);

            if (sources.length === 0) {
                console.log("DEBUG: No external sources found.");
            }

            sources.forEach(src => {
                const btn = document.createElement('button');
                btn.className = 'source-btn';
                btn.innerHTML = `<i class="fas fa-external-link-alt"></i> ${src.sourceName}`;
                btn.onclick = () => switchToSource(src, btn);
                switcher.appendChild(btn);
            });
        } catch (err) {
            console.error('Kaynaklar yüklenemedi:', err);
        }
    }

    function switchToHls() {
        const iframeContainer = document.getElementById('iframePlayerContainer');
        iframeContainer.style.display = 'none';
        iframeContainer.innerHTML = '';
        videoPlayer.style.display = 'block';

        // Show custom controls and play button
        const controls = document.querySelector('.video-controls');
        if (controls) controls.style.display = ''; // Revert to CSS default (flex)

        const playBtn = document.querySelector('.play-pause-button');
        if (playBtn) playBtn.style.display = ''; // Revert to CSS default

        document.querySelectorAll('.source-btn').forEach(b => b.classList.remove('active'));
        document.getElementById('btn-hls').classList.add('active');
    }

    function switchToSource(source, btn) {
        videoPlayer.pause();
        videoPlayer.style.display = 'none';

        // Hide custom controls and play button
        const controls = document.querySelector('.video-controls');
        if (controls) controls.style.display = 'none';

        const playBtn = document.querySelector('.play-pause-button');
        if (playBtn) playBtn.style.display = 'none';

        const iframeContainer = document.getElementById('iframePlayerContainer');
        iframeContainer.style.display = 'block';

        let finalUrl = source.sourceUrl.trim();
        // If it's a full iframe tag, extract the src
        if (finalUrl.toLowerCase().startsWith('<iframe')) {
            const match = finalUrl.match(/src=["']([^"']+)["']/i);
            if (match && match[1]) {
                finalUrl = match[1];
            }
        }

        iframeContainer.innerHTML = `
            <iframe src="${finalUrl}" 
                    style="width:100%; height:100%; border:none;" 
                    allowfullscreen 
                    allow="autoplay; encrypted-media" 
                    sandbox="allow-forms allow-scripts allow-pointer-lock allow-same-origin allow-top-navigation">
            </iframe>`;

        document.querySelectorAll('.source-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
    }
}
