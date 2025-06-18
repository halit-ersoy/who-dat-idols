export function initWeeklyBestSection() {
    const weeklyBestContainer = document.querySelector('.weekly-best-grid');
    const movieToggle = document.getElementById('weekly-movies-toggle');
    const tvToggle = document.getElementById('weekly-tv-toggle');
    const toggleContainer = document.querySelector('.weekly-best-toggle');

    if (!weeklyBestContainer || !movieToggle || !tvToggle || !toggleContainer) {
        console.error('Weekly Best Section: Gerekli elementler bulunamadı.');
        return;
    }

    // Hem filmler hem de diziler verisini cache'lemek için nesne oluşturuluyor.
    let weeklyData = {movies: null, tv: null};
    let currentMode = 'tv'; // Diziler seçili başlangıç durumu
    let isLoading = false;

    // Set initial toggle state
    updateToggleState();

    // Sayfa yüklendiğinde her iki veri setini de çekiyoruz, ancak dizileri önce render ediyoruz
    fetchAndCache('tv').then(() => {
        renderWeeklyBest('tv');
    });
    fetchAndCache('movies');

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
        // Geçiş animasyonu için fade-out ekliyoruz
        weeklyBestContainer.classList.add('fade-out');
        setTimeout(() => {
            renderWeeklyBest(mode);
        }, 300);
    }

    function updateToggleState() {
        movieToggle.classList.toggle('active', currentMode === 'movies');
        tvToggle.classList.toggle('active', currentMode === 'tv');

        if (currentMode === 'tv') {
            toggleContainer.classList.remove('tv-active');
        } else {
            toggleContainer.classList.add('tv-active');
        }
    }

    function fetchAndCache(type) {
        const endpoint = type === 'movies' ? '/api/weekly-best/movies' : '/api/weekly-best/tv';
        return fetch(endpoint)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Network response was not ok (status: ${response.status})`);
                }
                return response.json();
            })
            .then(data => {
                weeklyData[type] = data;
            })
            .catch(error => {
                console.error(`Weekly Best Section - ${type} verisi alınırken hata:`, error);
            });
    }

    function renderWeeklyBest(type) {
        const data = weeklyData[type];
        if (!data) {
            // Veri cache'de yoksa loading spinner gösterip tekrar fetch yapıyoruz.
            weeklyBestContainer.innerHTML = '<div class="loading-spinner"></div>';
            fetchAndCache(type).then(() => {
                renderCards(weeklyData[type]);
            });
        } else {
            renderCards(data);
        }
        isLoading = false;
    }

    function renderCards(data) {
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