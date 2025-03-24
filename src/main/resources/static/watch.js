document.addEventListener('DOMContentLoaded', function() {
    const urlParams = new URLSearchParams(window.location.search);
    const id = urlParams.get('id');

    // Get the video player element
    const videoPlayer = document.getElementById('videoPlayer');
    const videoSource = document.getElementById('videoSource');
    const volumeControl = document.getElementById('volumeControl');

    // Set a specific skip amount variable that can be used consistently
    const SKIP_SECONDS = 10;

    // Hide the native volume control
    const style = document.createElement('style');
    style.textContent = `
        video::-webkit-media-controls-volume-slider,
        video::-webkit-media-controls-mute-button {
            display: none !important;
        }
    `;
    document.head.appendChild(style);

    // Initialize volume control
    volumeControl.addEventListener('input', function() {
        videoPlayer.volume = this.value;
        updateVolumeIcon(this.value);
    });

    // Function to update volume icon based on volume level
    function updateVolumeIcon(volume) {
        const volumeIcon = volumeControl.previousElementSibling;
        if (volume == 0) {
            volumeIcon.className = 'fas fa-volume-mute';
        } else if (volume < 0.5) {
            volumeIcon.className = 'fas fa-volume-down';
        } else {
            volumeIcon.className = 'fas fa-volume-up';
        }
    }

    // Function to skip video time - centralized to ensure consistency
    function skipVideo(seconds) {
        if (videoPlayer && !isNaN(videoPlayer.duration)) {
            if (seconds < 0) {
                videoPlayer.currentTime = Math.max(0, videoPlayer.currentTime + seconds);
            } else {
                videoPlayer.currentTime = Math.min(videoPlayer.duration, videoPlayer.currentTime + seconds);
            }
        }
    }

    // Add click event listeners for the skip buttons
    document.getElementById('backwardButton').addEventListener('click', function() {
        skipVideo(-SKIP_SECONDS);
    });

    document.getElementById('forwardButton').addEventListener('click', function() {
        skipVideo(SKIP_SECONDS);
    });

    // Completely disable default arrow key behaviors
    window.addEventListener('keydown', function(e) {
        if ((e.key === 'ArrowLeft' || e.key === 'ArrowRight') &&
            document.activeElement.tagName !== 'INPUT' &&
            document.activeElement.tagName !== 'TEXTAREA') {
            e.preventDefault();
        }
    }, true); // Use capturing phase

    // Add our custom arrow key handler
    document.addEventListener('keydown', function(e) {
        if (document.activeElement.tagName !== 'INPUT' && document.activeElement.tagName !== 'TEXTAREA') {
            if (e.key === 'ArrowLeft') {
                skipVideo(-SKIP_SECONDS);
                console.log("Skipped back " + SKIP_SECONDS + " seconds to: " + videoPlayer.currentTime);
            } else if (e.key === 'ArrowRight') {
                skipVideo(SKIP_SECONDS);
                console.log("Skipped forward " + SKIP_SECONDS + " seconds to: " + videoPlayer.currentTime);
            }
        }
    });

    // Ensure volume control is properly synced when the video loads/plays
    videoPlayer.addEventListener('loadedmetadata', function() {
        setTimeout(function() {
            volumeControl.value = videoPlayer.volume;
            updateVolumeIcon(videoPlayer.volume);
        }, 100);
    });

    videoPlayer.addEventListener('play', function() {
        volumeControl.value = videoPlayer.volume;
        updateVolumeIcon(videoPlayer.volume);
    });

    // Error handling for video
    videoPlayer.addEventListener('error', function() {
        document.getElementById('title').innerText = "Video yüklenirken hata oluştu.";
        document.getElementById('videoInfo').innerText = "Video dosyası bulunamadı veya oynatılamıyor.";
    });

    if (id) {
        // Set the video source with the ID parameter
        videoSource.src = `/api/video/stream?id=${id}`;
        videoPlayer.load(); // Important: Must reload the video with the new source

        // Update UI with video info
        document.getElementById('title').innerText = `Video ${id}`;
        document.getElementById('videoInfo').innerText = `Video ID: ${id}`;

        // Configure episode navigation buttons
        const prevEpisodeBtn = document.getElementById('prevEpisode');
        const nextEpisodeBtn = document.getElementById('nextEpisode');

        // Disable previous button if we're at episode 1
        prevEpisodeBtn.disabled = (id <= 1);

        // Set up navigation handlers
        prevEpisodeBtn.addEventListener('click', function() {
            if (id > 1) {
                window.location.href = `/watch?id=${parseInt(id) - 1}`;
            }
        });

        nextEpisodeBtn.addEventListener('click', function() {
            window.location.href = `/watch?id=${parseInt(id) + 1}`;
        });
    } else {
        document.getElementById('title').innerText = "Video bulunamadı";
        document.getElementById('videoInfo').innerText = "Geçerli bir video ID'si belirtilmedi.";
        videoPlayer.style.display = 'none';
    }

    document.querySelector('.glow-button').addEventListener('click', function() {
        alert('İstek listesine eklendi!');
    });

    // Disable any default HTML5 video player keyboard shortcuts
    videoPlayer.addEventListener('keydown', function(e) {
        if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') {
            e.preventDefault();
            e.stopPropagation();
            return false;
        }
    }, true);
});