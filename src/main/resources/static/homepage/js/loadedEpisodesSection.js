export function initLoadedEpisodesSection() {
    const carousel = document.getElementById('upcoming-episodes-carousel');
    const translatedToggle = document.getElementById('upcoming-translated-toggle');
    const newToggle = document.getElementById('upcoming-new-toggle');
    const toggleContainer = document.querySelector('.upcoming-episodes-toggle');

    // To track and clear timers when switching tabs
    let countdownTimers = [];

    if (!carousel || !translatedToggle || !newToggle || !toggleContainer) {
        console.error('Upcoming Episodes Section: Required elements not found.');
        return;
    }

    // Cache for both translated and upcoming data
    let episodesData = {translated: null, upcoming: null};
    let currentMode = 'translated';
    let isLoading = false;

    // Fetch and cache data for translated episodes
    fetchTranslatedData().then(() => {
        renderCarousel('translated');
    });

    // Add event listeners for toggle buttons
    translatedToggle.addEventListener('click', () => {
        if (currentMode !== 'translated' && !isLoading) {
            switchMode('translated');
        }
    });

    newToggle.addEventListener('click', () => {
        if (currentMode !== 'upcoming' && !isLoading) {
            switchMode('upcoming');
        }
    });

    function switchMode(mode) {
        isLoading = true;
        currentMode = mode;
        updateToggleState();
        carousel.style.opacity = '0.5';

        // Clear all existing countdown timers
        clearAllCountdowns();

        setTimeout(() => {
            renderCarousel(mode);
        }, 300);
    }

    function clearAllCountdowns() {
        countdownTimers.forEach(timer => clearInterval(timer));
        countdownTimers = [];
    }

    function updateToggleState() {
        translatedToggle.classList.toggle('active', currentMode === 'translated');
        newToggle.classList.toggle('active', currentMode === 'upcoming');

        if (currentMode === 'upcoming') {
            toggleContainer.classList.add('toggle-right');
        } else {
            toggleContainer.classList.remove('toggle-right');
        }
    }

    function fetchTranslatedData() {
        return fetch('/api/saved/translated')
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Network response was not ok (status: ${response.status})`);
                }
                return response.json();
            })
            .then(data => {
                episodesData.translated = data;
            })
            .catch(error => {
                console.error('Error fetching translated episodes:', error);
            });
    }

    function fetchUpcomingData() {
        return fetch('/api/saved/loaded')
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Network response was not ok (status: ${response.status})`);
                }
                return response.json();
            })
            .then(data => {
                episodesData.upcoming = data;
                return data;
            })
            .catch(error => {
                console.error('Error fetching loaded episodes:', error);
                return [];
            });
    }

    function renderCarousel(mode) {
        if (mode === 'translated') {
            const data = episodesData.translated;
            if (!data) {
                carousel.innerHTML = '<div class="upcoming-loading-spinner"></div>';
                fetchTranslatedData().then(() => {
                    populateCarousel(episodesData.translated);
                });
            } else {
                populateCarousel(data);
            }
        } else {
            // Upcoming episodes section
            carousel.innerHTML = '<div class="upcoming-loading-spinner"></div>';
            if (episodesData.upcoming) {
                populateUpcomingCarousel(episodesData.upcoming);
            } else {
                fetchUpcomingData().then(data => {
                    populateUpcomingCarousel(data);
                });
            }
        }
        isLoading = false;
    }

    function getStatusPercentage(status) {
        // If status is already a number, use it directly
        if (!isNaN(parseFloat(status))) {
            return parseFloat(status);
        }

        // Convert text statuses to percentage values
        switch(String(status).toLowerCase()) {
            case 'çevrildi':
            case 'hazır':
            case 'completed':
            case 'tamamlandı':
                return 100;
            case 'in progress':
            case 'devam ediyor':
            case 'çevriliyor':
                return 60;
            case 'started':
            case 'başlandı':
                return 30;
            case 'pending':
            case 'beklemede':
                return 15;
            case 'not started':
            case 'başlanmadı':
                return 0;
            default:
                // Try to extract percentage from text if possible
                const percentMatch = String(status).match(/(\d+)%/);
                if (percentMatch && percentMatch[1]) {
                    return parseFloat(percentMatch[1]);
                }
                // Default fallback
                return 0;
        }
    }

    function formatDate(dateString) {
        try {
            const date = new Date(dateString);
            return date.toLocaleDateString('tr-TR', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric'
            });
        } catch (error) {
            console.error('Error formatting date:', error);
            return dateString;
        }
    }

    function calculateTimeRemaining(targetDate) {
        const now = new Date().getTime();
        const target = new Date(targetDate).getTime();
        const difference = target - now;

        // If the date is in the past
        if (difference <= 0) {
            return { isPast: true };
        }

        // Calculate time units
        const days = Math.floor(difference / (1000 * 60 * 60 * 24));
        const hours = Math.floor((difference % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((difference % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((difference % (1000 * 60)) / 1000);

        return {
            isPast: false,
            days: days,
            hours: hours,
            minutes: minutes,
            seconds: seconds
        };
    }

    function populateUpcomingCarousel(episodes) {
        clearAllCountdowns();
        carousel.innerHTML = '';

        if (episodes && episodes.length > 0) {
            episodes.forEach((episode, index) => {
                const formattedDate = formatDate(episode.datetime);
                const episodeId = `upcoming-countdown-${index}`;
                const timeRemaining = calculateTimeRemaining(episode.datetime);

                let countdownHtml;
                if (timeRemaining.isPast) {
                    countdownHtml = `<div class="upcoming-countdown">Çok Yakında</div>`;
                } else {
                    countdownHtml = `
                        <div class="upcoming-countdown" id="${episodeId}">
                            <span class="upcoming-countdown-unit"><span class="upcoming-countdown-value">${timeRemaining.days}</span>g</span>
                            <span class="upcoming-countdown-unit"><span class="upcoming-countdown-value">${timeRemaining.hours}</span>s</span>
                            <span class="upcoming-countdown-unit"><span class="upcoming-countdown-value">${timeRemaining.minutes}</span>d</span>
                            <span class="upcoming-countdown-unit"><span class="upcoming-countdown-value">${timeRemaining.seconds}</span>sn</span>
                        </div>
                    `;
                }

                carousel.innerHTML += `
                    <div class="upcoming-card">
                        <div class="upcoming-card-image">
                            <img src="/media/image/${episode.ID}" alt="${episode.name}">
                            <div class="upcoming-play-icon">
                                <i class="fas fa-play"></i>
                            </div>
                        </div>
                        <div class="upcoming-card-content">
                            <h3 class="upcoming-card-title">${episode.name}</h3>
                            ${countdownHtml}
                        </div>
                    </div>
                `;
            });

            // After adding cards to the DOM, set up the countdown timers
            episodes.forEach((episode, index) => {
                const timeRemaining = calculateTimeRemaining(episode.datetime);
                if (!timeRemaining.isPast) {
                    const countdownElement = document.getElementById(`upcoming-countdown-${index}`);
                    if (countdownElement) {
                        const timer = setInterval(() => {
                            updateCountdown(countdownElement, episode.datetime);
                        }, 1000);
                        countdownTimers.push(timer);
                    }
                }
            });

            setTimeout(() => {
                carousel.style.opacity = '1';
            }, 100);

            // Add navigation functionality
            setupCarouselNavigation();
        } else {
            carousel.innerHTML = '<div class="upcoming-error-message">Veri bulunamadı.</div>';
        }
    }

    function updateCountdown(element, targetDate) {
        const timeRemaining = calculateTimeRemaining(targetDate);

        // If the date has passed during countdown, switch to "Çok Yakında"
        if (timeRemaining.isPast) {
            element.innerHTML = 'Çok Yakında';
            return;
        }

        // Update countdown values
        const valueElements = element.querySelectorAll('.upcoming-countdown-value');
        valueElements[0].textContent = timeRemaining.days;
        valueElements[1].textContent = timeRemaining.hours;
        valueElements[2].textContent = timeRemaining.minutes;
        valueElements[3].textContent = timeRemaining.seconds;
    }

    function populateCarousel(episodes) {
        carousel.innerHTML = '';

        if (episodes && episodes.length > 0) {
            episodes.forEach(episode => {
                const statusPercentage = getStatusPercentage(episode.status);

                carousel.innerHTML += `
                    <div class="upcoming-card">
                        <div class="upcoming-card-image">
                            <img src="/media/image/${episode.ID}" alt="${episode.name}">
                            <div class="upcoming-play-icon">
                                <i class="fas fa-play"></i>
                            </div>
                        </div>
                        <div class="upcoming-card-content">
                            <h3 class="upcoming-card-title">${episode.name}</h3>
                            <div class="upcoming-progress-container">
                                <div class="upcoming-progress-bar" style="width: ${statusPercentage}%"></div>
                            </div>
                        </div>
                    </div>
                `;
            });

            setTimeout(() => {
                carousel.style.opacity = '1';
            }, 100);

            // Add navigation functionality
            setupCarouselNavigation();
        } else {
            carousel.innerHTML = '<div class="upcoming-error-message">Veri bulunamadı.</div>';
        }
    }

    function setupCarouselNavigation() {
        const prevBtn = document.querySelector('.upcoming-prev-btn');
        const nextBtn = document.querySelector('.upcoming-next-btn');

        if (prevBtn && nextBtn) {
            prevBtn.addEventListener('click', () => {
                carousel.scrollBy({left: -800, behavior: 'smooth'});
            });

            nextBtn.addEventListener('click', () => {
                carousel.scrollBy({left: 800, behavior: 'smooth'});
            });
        }
    }
}