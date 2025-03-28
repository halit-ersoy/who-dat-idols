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
        updateVolumeSliderBackground(); // Add this call here to update background on change
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

    // Update volume slider background based on value
    function updateVolumeSliderBackground() {
        const value = volumeControl.value * 100;
        volumeControl.style.background = `linear-gradient(to right, #1ed760 0%, #1ed760 ${value}%, #555 ${value}%, #555 100%)`;
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
            updateVolumeSliderBackground();
        }, 100);
    });

    videoPlayer.addEventListener('play', function() {
        volumeControl.value = videoPlayer.volume;
        updateVolumeIcon(videoPlayer.volume);
        updateVolumeSliderBackground();
        playButton.innerHTML = '<i class="fas fa-pause"></i>';
    });

    // Hide spinner when video starts playing
    videoPlayer.addEventListener('playing', function() {
        this.classList.add('playing');
    });

    videoPlayer.addEventListener('pause', function() {
        playButton.innerHTML = '<i class="fas fa-play"></i>';
    });

    // Add play/pause button with animation
    const videoWrapper = document.querySelector('.video-wrapper');
    const playButton = document.createElement('div');
    playButton.className = 'play-pause-button';
    playButton.innerHTML = '<i class="fas fa-play"></i>';
    videoWrapper.appendChild(playButton);

    playButton.addEventListener('click', function() {
        if (videoPlayer.paused) {
            videoPlayer.play();
            this.innerHTML = '<i class="fas fa-pause"></i>';
        } else {
            videoPlayer.pause();
            this.innerHTML = '<i class="fas fa-play"></i>';
        }
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

    // Comments functionality
    const commentForm = document.getElementById('commentForm');
    const commentsList = document.getElementById('comments-list');
    const commentText = document.getElementById('commentText');

    // Load comments from local storage (this would be replaced with API calls in production)
    function loadComments() {
        const videoId = new URLSearchParams(window.location.search).get('id');
        if (!videoId) return;

        const storageKey = `video_comments_${videoId}`;
        let comments = JSON.parse(localStorage.getItem(storageKey) || '[]');

        if (comments.length === 0) {
            commentsList.innerHTML = '<div class="no-comments">Bu video için henüz yorum yapılmamış. İlk yorumu siz yapın!</div>';
            return;
        }

        // Sort comments by date (newest first)
        comments.sort((a, b) => new Date(b.date) - new Date(a.date));

        // Render comments
        commentsList.innerHTML = '';
        comments.forEach(comment => {
            const commentElement = createCommentElement(comment);
            commentsList.appendChild(commentElement);
        });
    }

    // Create a single comment element
    function createCommentElement(comment) {
        const commentCard = document.createElement('div');
        commentCard.className = 'comment-card';

        const formattedDate = new Date(comment.date).toLocaleDateString('tr-TR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });

        commentCard.innerHTML = `
            <div class="comment-header">
                <div class="user-avatar">
                    <i class="fas fa-user"></i>
                </div>
                <div class="comment-user">${comment.username || 'Misafir'}</div>
                <div class="comment-date">${formattedDate}</div>
            </div>
            <div class="comment-content">${comment.text}</div>
        `;

        return commentCard;
    }

    // Handle comment submission
    if (commentForm) {
        commentForm.addEventListener('submit', function(e) {
            e.preventDefault();

            const text = commentText.value.trim();
            if (!text) {
                // Animate the textarea to indicate error
                commentText.style.borderColor = '#ff3860';
                setTimeout(() => {
                    commentText.style.borderColor = '#333';
                }, 1000);
                return;
            }

            const videoId = new URLSearchParams(window.location.search).get('id');
            if (!videoId) return;

            const newComment = {
                id: Date.now(),
                text: text,
                username: 'Kullanıcı', // In a real app, get from logged in user
                date: new Date().toISOString()
            };

            // Save to local storage (would be an API call in production)
            const storageKey = `video_comments_${videoId}`;
            let comments = JSON.parse(localStorage.getItem(storageKey) || '[]');
            comments.push(newComment);
            localStorage.setItem(storageKey, JSON.stringify(comments));

            // Clear form and reload comments
            commentText.value = '';

            // Add the new comment with animation
            const commentElement = createCommentElement(newComment);
            commentElement.style.opacity = '0';

            if (commentsList.querySelector('.no-comments')) {
                commentsList.innerHTML = '';
            }

            commentsList.insertBefore(commentElement, commentsList.firstChild);

            // Trigger animation
            setTimeout(() => {
                commentElement.style.opacity = '1';
            }, 10);
        });
    }

    // Initialize comments when page loads
    if (commentsList) {
        loadComments();
    }
});