export function initWeeklyBestSection() {
    const weeklyBestContainer = document.querySelector('.weekly-best-grid');
    const movieToggle = document.getElementById('weekly-movies-toggle');
    const tvToggle = document.getElementById('weekly-tv-toggle');
    const toggleContainer = document.querySelector('.weekly-best-toggle');

    if (!weeklyBestContainer || !movieToggle || !tvToggle || !toggleContainer) {
        console.error('Weekly Best Section: Gerekli elementler bulunamadı.');
        return;
    }

    let currentMode = 'movies'; // Varsayılan mod
    let isLoading = false;

    // İlk olarak filmlerle başlat
    fetchWeeklyBest('movies');

    movieToggle.addEventListener('click', () => {
        if (currentMode !== 'movies' && !isLoading) {
            switchMode('movies');
        }
    });

    tvToggle.addEventListener('click', () => {
        if (currentMode !== 'tv' && !isLoading) {
            switchMode('tv');
        }
    });

    function switchMode(mode) {
        isLoading = true;
        currentMode = mode;
        updateToggleState();

        // Animasyon ekle
        weeklyBestContainer.classList.add('fade-out');
        setTimeout(() => {
            fetchWeeklyBest(mode);
        }, 300);
    }

    function updateToggleState() {
        movieToggle.classList.toggle('active', currentMode === 'movies');
        tvToggle.classList.toggle('active', currentMode === 'tv');

        if (currentMode === 'tv') {
            toggleContainer.classList.add('tv-active');
        } else {
            toggleContainer.classList.remove('tv-active');
        }
    }

    async function fetchWeeklyBest(type) {
        weeklyBestContainer.innerHTML = '<div class="loading-spinner"></div>';
        const endpoint = type === 'movies' ? '/api/weekly-best/movies' : '/api/weekly-best/tv';

        try {
            const response = await fetch(endpoint);
            if (!response.ok) {
                throw new Error(`Network response was not ok (status: ${response.status})`);
            }
            const data = await response.json();
            weeklyBestContainer.innerHTML = '';

            if (data && data.length > 0) {
                data.forEach((item, index) => {
                    weeklyBestContainer.appendChild(createWeeklyCard(item, index + 1));
                });
                setTimeout(() => {
                    weeklyBestContainer.classList.remove('fade-out');
                    weeklyBestContainer.classList.add('fade-in');
                    setTimeout(() => {
                        weeklyBestContainer.classList.remove('fade-in');
                    }, 500);
                }, 100);
            } else {
                weeklyBestContainer.innerHTML = '<div class="error-message">Veri bulunamadı.</div>';
            }
        } catch (error) {
            console.error('Weekly Best Section - Hata:', error);
            weeklyBestContainer.innerHTML = '<div class="error-message">Veriler yüklenirken bir hata oluştu.</div>';
        } finally {
            isLoading = false;
        }
    }

    function createWeeklyCard(item, number) {
        const card = document.createElement('a');
        card.href = item.videoUrl;
        card.className = 'weekly-card';
        card.dataset.number = number;

        card.innerHTML = `
            <div class="card-image-container">
                <img src="${item.thumbnailUrl}" alt="${item.title}">
                <div class="play-icon"><i class="fas fa-play"></i></div>
            </div>
            <div class="card-content">
                <p class="card-title">${item.title}</p>
                <p class="card-info">${item.info}</p>
            </div>
        `;
        return card;
    }
}
