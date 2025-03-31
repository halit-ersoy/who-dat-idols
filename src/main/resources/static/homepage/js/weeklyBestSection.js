// Updated initWeeklyBestSection function in weeklyBestSection.js
export function initWeeklyBestSection() {
    const weeklyBestContainer = document.querySelector('.weekly-best-grid');
    const movieToggle = document.getElementById('weekly-movies-toggle');
    const tvToggle = document.getElementById('weekly-tv-toggle');
    const toggleContainer = document.querySelector('.weekly-best-toggle');
    let currentMode = 'movies'; // Default mode
    let isLoading = false;

    // Initialize with movies
    fetchWeeklyBest('movies');

    // Add event listeners for toggle buttons
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

        // Add animation to content
        weeklyBestContainer.classList.add('fade-out');

        setTimeout(() => {
            fetchWeeklyBest(mode);
        }, 300);
    }

    function updateToggleState() {
        // Update active state
        movieToggle.classList.toggle('active', currentMode === 'movies');
        tvToggle.classList.toggle('active', currentMode === 'tv');

        // Update toggle container for sliding effect
        if (currentMode === 'tv') {
            toggleContainer.classList.add('tv-active');
        } else {
            toggleContainer.classList.remove('tv-active');
        }
    }

    function fetchWeeklyBest(type) {
        // Show loading state
        weeklyBestContainer.innerHTML = '<div class="loading-spinner"></div>';

        // Fetch data from API
        const endpoint = type === 'movies' ? '/api/weekly-best/movies' : '/api/weekly-best/tv';

        fetch(endpoint)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Network response was not ok (status: ${response.status})`);
                }
                return response.json();
            })
            .then(data => {
                // Clear loading state
                weeklyBestContainer.innerHTML = '';

                // Create cards for each item
                if (data && data.length > 0) {
                    data.forEach((item, index) => {
                        const card = createWeeklyCard(item, index + 1);
                        weeklyBestContainer.appendChild(card);
                    });

                    // Add animation for content appearing
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
                isLoading = false;
            })
            .catch(error => {
                console.error('Error fetching weekly best:', error);
                weeklyBestContainer.innerHTML = '<div class="error-message">Veriler yüklenirken bir hata oluştu.</div>';
                isLoading = false;
            });
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