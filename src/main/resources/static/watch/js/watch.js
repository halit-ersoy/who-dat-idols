document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const id = urlParams.get('id');

    // Video ve kontrol elemanlarını seçelim, mevcutluk kontrolü yapalım
    const videoPlayer = document.getElementById('videoPlayer');
    const videoSource = document.getElementById('videoSource');
    const volumeControl = document.getElementById('volumeControl');
    const backwardBtn = document.getElementById('backwardButton');
    const forwardBtn = document.getElementById('forwardButton');
    const playPauseWrapper = document.querySelector('.video-wrapper');
    const glowButton = document.querySelector('.glow-button');

    if (!videoPlayer || !videoSource || !volumeControl || !playPauseWrapper) {
        console.error('Gerekli video oynatıcı elementleri bulunamadı.');
        return;
    }

    // Kullanılacak sabitler
    const SKIP_SECONDS = 10;

    // Native volume kontrolünü gizlemek için stil ekle
    const style = document.createElement('style');
    style.textContent = `
    video::-webkit-media-controls-volume-slider,
    video::-webkit-media-controls-mute-button {
      display: none !important;
    }
  `;
    document.head.appendChild(style);

    // Volume kontrolü ve ilgili arayüz güncellemeleri
    volumeControl.addEventListener('input', function () {
        videoPlayer.volume = this.value;
        updateVolumeIcon(this.value);
        updateVolumeSliderBackground();
    });

    function updateVolumeIcon(volume) {
        const volumeIcon = volumeControl.previousElementSibling;
        if (!volumeIcon) return;
        if (volume == 0) {
            volumeIcon.className = 'fas fa-volume-mute';
        } else if (volume < 0.5) {
            volumeIcon.className = 'fas fa-volume-down';
        } else {
            volumeIcon.className = 'fas fa-volume-up';
        }
    }

    function updateVolumeSliderBackground() {
        const value = volumeControl.value * 100;
        volumeControl.style.background = `linear-gradient(to right, #1ed760 0%, #1ed760 ${value}%, #555 ${value}%, #555 100%)`;
    }

    // Video ileri/geri atlama fonksiyonu
    function skipVideo(seconds) {
        if (videoPlayer && !isNaN(videoPlayer.duration)) {
            if (seconds < 0) {
                videoPlayer.currentTime = Math.max(0, videoPlayer.currentTime + seconds);
            } else {
                videoPlayer.currentTime = Math.min(videoPlayer.duration, videoPlayer.currentTime + seconds);
            }
        }
    }

    // Skip butonlarının varlığını kontrol edip event ekleyelim
    if (backwardBtn) {
        backwardBtn.addEventListener('click', () => skipVideo(-SKIP_SECONDS));
    }
    if (forwardBtn) {
        forwardBtn.addEventListener('click', () => skipVideo(SKIP_SECONDS));
    }

    // Önceki/sonraki ok tuşlarının varsayılan davranışını devre dışı bırak
    window.addEventListener(
        'keydown',
        (e) => {
            if ((e.key === 'ArrowLeft' || e.key === 'ArrowRight') &&
                document.activeElement.tagName !== 'INPUT' &&
                document.activeElement.tagName !== 'TEXTAREA') {
                e.preventDefault();
            }
        },
        true
    );

    // Özel ok tuşu atlama işlemi
    document.addEventListener('keydown', (e) => {
        if (document.activeElement.tagName !== 'INPUT' && document.activeElement.tagName !== 'TEXTAREA') {
            if (e.key === 'ArrowLeft') {
                skipVideo(-SKIP_SECONDS);
                console.log(`Skipped back ${SKIP_SECONDS} seconds to: ${videoPlayer.currentTime}`);
            } else if (e.key === 'ArrowRight') {
                skipVideo(SKIP_SECONDS);
                console.log(`Skipped forward ${SKIP_SECONDS} seconds to: ${videoPlayer.currentTime}`);
            }
        }
    });

    // Volume kontrolü senkronizasyonu video yüklendiğinde
    videoPlayer.addEventListener('loadedmetadata', () => {
        setTimeout(() => {
            volumeControl.value = videoPlayer.volume;
            updateVolumeIcon(videoPlayer.volume);
            updateVolumeSliderBackground();
        }, 100);
    });

    // İzlenme sayısını arttıran fonksiyon (fetch hata kontrolü eklenmiş)
    function incrementViewCount(videoId) {
        fetch(`/api/video/increment-view?id=${videoId}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'}
        })
            .then(response => {
                if (!response.ok) {
                    console.error('Failed to increment view count');
                }
            })
            .catch(error => {
                console.error('Error incrementing view count:', error);
            });
    }

    let firstPlay = true;
    videoPlayer.addEventListener('play', () => {
        if (firstPlay && id) {
            incrementViewCount(id);
            firstPlay = false;
        }
        volumeControl.value = videoPlayer.volume;
        updateVolumeIcon(videoPlayer.volume);
        updateVolumeSliderBackground();
        if (playButton) {
            playButton.innerHTML = '<i class="fas fa-pause"></i>';
        }
    });

    videoPlayer.addEventListener('playing', function () {
        this.classList.add('playing');
    });

    videoPlayer.addEventListener('pause', function () {
        if (playButton) {
            playButton.innerHTML = '<i class="fas fa-play"></i>';
        }
    });

    // Play/Pause butonunu ekleyelim
    const playButton = document.createElement('div');
    playButton.className = 'play-pause-button';
    playButton.innerHTML = '<i class="fas fa-play"></i>';
    playPauseWrapper.appendChild(playButton);

    playButton.addEventListener('click', function () {
        if (videoPlayer.paused) {
            videoPlayer.play();
            this.innerHTML = '<i class="fas fa-pause"></i>';
        } else {
            videoPlayer.pause();
            this.innerHTML = '<i class="fas fa-play"></i>';
        }
    });

    // Video hata durumunda mesaj göster
    videoPlayer.addEventListener('error', () => {
        const titleEl = document.getElementById('title');
        const infoEl = document.getElementById('videoInfo');
        if (titleEl) titleEl.innerText = "Video yüklenirken hata oluştu.";
        if (infoEl) infoEl.innerText = "Video dosyası bulunamadı veya oynatılamıyor.";
    });

    // Video ID mevcutsa video kaynaklarını ve bölüm navigasyonunu ayarla
    if (id) {
        videoSource.src = `/api/video/stream?id=${id}`;
        videoPlayer.load();
        const titleEl = document.getElementById('title');
        const infoEl = document.getElementById('videoInfo');
        if (titleEl) titleEl.innerText = `Video ${id}`;
        if (infoEl) infoEl.innerText = `Video ID: ${id}`;

        const prevEpisodeBtn = document.getElementById('prevEpisode');
        const nextEpisodeBtn = document.getElementById('nextEpisode');

        if (prevEpisodeBtn) {
            prevEpisodeBtn.disabled = (parseInt(id) <= 1);
            prevEpisodeBtn.addEventListener('click', () => {
                if (parseInt(id) > 1) {
                    window.location.href = `/watch?id=${parseInt(id) - 1}`;
                }
            });
        }

        if (nextEpisodeBtn) {
            nextEpisodeBtn.addEventListener('click', () => {
                window.location.href = `/watch?id=${parseInt(id) + 1}`;
            });
        }
    } else {
        const titleEl = document.getElementById('title');
        const infoEl = document.getElementById('videoInfo');
        if (titleEl) titleEl.innerText = "Video bulunamadı";
        if (infoEl) infoEl.innerText = "Geçerli bir video ID'si belirtilmedi.";
        videoPlayer.style.display = 'none';
    }

    if (glowButton) {
        glowButton.addEventListener('click', () => {
            alert('İstek listesine eklendi!');
        });
    }

    // Disable HTML5 video player varsayılan ok tuşu kısayollarını engelle
    videoPlayer.addEventListener('keydown', (e) => {
        if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') {
            e.preventDefault();
            e.stopPropagation();
            return false;
        }
    }, true);

    // Yorumlar bölümü
    const commentForm = document.getElementById('commentForm');
    const commentsList = document.getElementById('comments-list');
    const commentText = document.getElementById('commentText');

    if (commentText) {
        const charCounter = document.createElement('div');
        charCounter.className = 'char-counter';
        commentText.parentNode.appendChild(charCounter);

        commentText.addEventListener('input', function () {
            const remaining = 500 - this.value.length;
            charCounter.textContent = `${this.value.length}/500`;
            charCounter.style.color = remaining < 0 ? '#ff3860' : '#888';
        });

        commentText.addEventListener('focus', function () {
            this.setAttribute('placeholder', 'Düşüncelerinizi paylaşın...');
        });
        commentText.addEventListener('blur', function () {
            this.setAttribute('placeholder', 'Bu dizi hakkında düşünceleriniz neler?');
        });
    }

    function createCommentElement(comment) {
        const commentCard = document.createElement('div');
        commentCard.className = 'comment-card';
        if (comment.spoiler) {
            commentCard.classList.add('spoiler-comment');
        }

        const delay = Math.random() * 0.5;
        commentCard.style.animationDelay = `${delay}s`;

        const formattedDate = new Date(comment.date).toLocaleDateString('tr-TR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });

        // Build comment HTML with spoiler handling
        let commentHTML = `
      <div class="comment-header">
        <div class="user-avatar">
          <i class="fas fa-user"></i>
        </div>
        <div class="comment-user">${comment.username || 'Misafir'}</div>
        <div class="comment-date">${formattedDate}</div>
      </div>`;

        if (comment.spoiler) {
            commentHTML += `
          <div class="spoiler-warning">
            <i class="fas fa-exclamation-triangle"></i> Spoiler içerir
            <button class="show-spoiler-btn">Göster</button>
          </div>
          <div class="comment-content hidden">${comment.text}</div>`;
        } else {
            commentHTML += `<div class="comment-content">${comment.text}</div>`;
        }

        commentHTML += `
      <div class="comment-actions">
        <button class="action-button like-btn" data-id="${comment.id}">
          <i class="far fa-heart"></i> <span>0</span>
        </button>
        <button class="action-button">
          <i class="far fa-comment"></i> Yanıtla
        </button>
      </div>`;

        commentCard.innerHTML = commentHTML;

        // Set up event handlers
        setTimeout(() => {
            const likeBtn = commentCard.querySelector('.like-btn');
            if (likeBtn) {
                // Existing like button functionality
                likeBtn.addEventListener('click', function() {
                    const icon = this.querySelector('i');
                    const count = this.querySelector('span');
                    if (icon.classList.contains('far')) {
                        icon.classList.remove('far');
                        icon.classList.add('fas', 'like-animation');
                        count.textContent = parseInt(count.textContent) + 1;
                        this.classList.add('liked');
                    } else {
                        icon.classList.remove('fas');
                        icon.classList.add('far');
                        count.textContent = parseInt(count.textContent) - 1;
                        this.classList.remove('liked');
                    }
                    setTimeout(() => {
                        icon.classList.remove('like-animation');
                    }, 500);
                });
            }

            // Add spoiler toggle functionality
            const showSpoilerBtn = commentCard.querySelector('.show-spoiler-btn');
            if (showSpoilerBtn) {
                showSpoilerBtn.addEventListener('click', function() {
                    const spoilerWarning = commentCard.querySelector('.spoiler-warning');
                    const commentContent = commentCard.querySelector('.comment-content');
                    spoilerWarning.style.display = 'none';
                    commentContent.classList.remove('hidden');
                });
            }
        }, 500);

        return commentCard;
    }

    function loadComments() {
        const videoId = new URLSearchParams(window.location.search).get('id');
        if (!videoId || !commentsList) return;

        fetch(`/api/video/comments?id=${videoId}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to load comments');
                }
                return response.json();
            })
            .then(comments => {
                const commentsTitle = document.querySelector('.comments-section h3');
                if (commentsTitle) {
                    const countSpan = document.createElement('span');
                    countSpan.className = 'comment-count';
                    countSpan.textContent = comments.length;
                    commentsTitle.appendChild(countSpan);
                }

                if (comments.length === 0) {
                    commentsList.innerHTML = '<div class="no-comments">Bu video için henüz yorum yapılmamış. İlk yorumu siz yapın!</div>';
                    return;
                }

                // Sort comments by date (newest first)
                comments.sort((a, b) => new Date(b.datetime) - new Date(a.datetime));

                commentsList.innerHTML = '';
                comments.forEach(comment => {
                    const commentElement = createCommentElement({
                        id: Date.now(), // Generate temporary ID for frontend
                        username: comment.nickname,
                        date: comment.datetime,
                        text: comment.text,
                        spoiler: comment.spoiler === 1
                    });
                    commentsList.appendChild(commentElement);
                });
            })
            .catch(error => {
                console.error('Error loading comments:', error);
                commentsList.innerHTML = '<div class="error-message">Yorumlar yüklenirken bir hata oluştu.</div>';
            });
    }

    if (commentForm) {
        commentForm.addEventListener('submit', function (e) {
            e.preventDefault();
            const text = commentText.value.trim();
            const MAX_CHARS = 500;

            if (!text) {
                commentText.style.borderColor = '#ff3860';
                setTimeout(() => commentText.style.borderColor = '#333', 1000);
                return;
            }

            if (text.length > MAX_CHARS) {
                commentText.style.borderColor = '#ff3860';
                commentText.classList.add('error-shake');
                setTimeout(() => {
                    commentText.style.borderColor = '#333';
                    commentText.classList.remove('error-shake');
                }, 1000);
                const charCounter = document.querySelector('.char-counter');
                if (charCounter) {
                    charCounter.style.color = '#ff3860';
                    charCounter.style.fontWeight = 'bold';
                    setTimeout(() => (charCounter.style.fontWeight = 'normal'), 2000);
                }
                alert(`Yorumunuz çok uzun! Lütfen ${MAX_CHARS} karakterden az olacak şekilde düzenleyin.`);
                return;
            }

            const videoId = new URLSearchParams(window.location.search).get('id');
            if (!videoId) return;

            // Send comment to server
            fetch(`/api/video/comment?id=${videoId}&spoiler=false`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'text/plain'
                },
                body: text,
                credentials: 'include' // Include cookies
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Failed to submit comment');
                    }
                    return response.text();
                })
                .then(() => {
                    // Create temporary comment element until page refresh
                    const tempComment = {
                        id: Date.now(),
                        text: text,
                        username: 'Kullanıcı', // Placeholder until refresh
                        date: new Date().toISOString()
                    };

                    commentText.value = '';
                    const commentElement = createCommentElement(tempComment);
                    commentElement.style.opacity = '0';

                    if (commentsList.querySelector('.no-comments')) {
                        commentsList.innerHTML = '';
                    }

                    commentsList.insertBefore(commentElement, commentsList.firstChild);
                    setTimeout(() => (commentElement.style.opacity = '1'), 10);

                    // Update comment count
                    const countSpan = document.querySelector('.comment-count');
                    if (countSpan) {
                        countSpan.textContent = parseInt(countSpan.textContent) + 1;
                    } else {
                        const commentsTitle = document.querySelector('.comments-section h3');
                        if (commentsTitle) {
                            const newCountSpan = document.createElement('span');
                            newCountSpan.className = 'comment-count';
                            newCountSpan.textContent = '1';
                            commentsTitle.appendChild(newCountSpan);
                        }
                    }
                })
                .catch(error => {
                    alert('Yorum gönderirken bir hata oluştu: ' + error.message);
                });
        });
    }

    if (commentsList) loadComments();

    const detailsToggle = document.querySelector('.details-toggle');
    const detailsContainer = document.querySelector('.content-details-container');
    if (detailsToggle && detailsContainer) {
        detailsToggle.addEventListener('click', function () {
            detailsToggle.classList.toggle('active');
            detailsContainer.classList.toggle('open');
        });
    }

    function loadContentDetails(videoId) {
        // Örnek veri; gerçek uygulamada API çağrısı yapılabilir
        const mockData = {
            title: "Who Dat Idols - Grup Belgeseli",
            poster: "https://picsum.photos/400/600",
            rating: "9.2",
            year: "2024",
            duration: "45 dk",
            language: "Türkçe",
            plot: "K-Pop dünyasının yeni yıldızları 'Who Dat Idols' grubunun oluşum hikayesi, kariyerlerindeki zorluklar ve başarıları konu alan bu belgesel, müzik endüstrisinin perde arkasını gözler önüne seriyor.",
            genres: ["Belgesel", "Müzik", "Biyografi"],
            season: 1,
            episode: videoId || 1,
            totalEpisodes: 10,
            cast: [
                {name: "Ji-soo Park", role: "Kendisi", avatar: "https://picsum.photos/200"},
                {name: "Min-ho Lee", role: "Kendisi", avatar: "https://picsum.photos/201"},
                {name: "Jae-hyun Kim", role: "Kendisi", avatar: "https://picsum.photos/202"},
                {name: "Yuna Choi", role: "Anlatıcı", avatar: "https://picsum.photos/203"},
                {name: "Seo-jun Jung", role: "Yapımcı", avatar: "https://picsum.photos/204"}
            ]
        };

        document.getElementById('contentTitle').textContent = mockData.title;
        document.getElementById('contentPoster').src = mockData.poster;
        document.getElementById('contentRating').textContent = mockData.rating;
        document.getElementById('releaseYear').textContent = mockData.year;
        document.getElementById('contentDuration').textContent = mockData.duration;
        document.getElementById('contentLanguage').textContent = mockData.language;
        document.getElementById('contentPlot').textContent = mockData.plot;
        document.getElementById('seasonNumber').textContent = `Sezon ${mockData.season}`;
        document.getElementById('episodeNumber').textContent = `Bölüm ${mockData.episode}`;
        document.getElementById('totalEpisodes').textContent = `Toplam ${mockData.totalEpisodes} Bölüm`;

        const genreTags = document.getElementById('genreTags');
        if (genreTags) {
            genreTags.innerHTML = '';
            mockData.genres.forEach(genre => {
                const tag = document.createElement('span');
                tag.className = 'genre-tag';
                tag.textContent = genre;
                genreTags.appendChild(tag);
            });
        }

        const castList = document.getElementById('castList');
        if (castList) {
            castList.innerHTML = '';
            mockData.cast.forEach(member => {
                const castMember = document.createElement('div');
                castMember.className = 'cast-member';
                castMember.innerHTML = `
          <div class="cast-avatar">
            <img src="${member.avatar}" alt="${member.name}">
          </div>
          <div class="cast-name">${member.name}</div>
          <div class="cast-role">${member.role}</div>
        `;
                castList.appendChild(castMember);
            });
        }
    }

    if (id) {
        loadContentDetails(id);
    }
});
