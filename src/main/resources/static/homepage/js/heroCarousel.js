export function initHeroCarousel() {
    // Elements and state for carousel
    const heroSection = document.querySelector('.hero');
    const heroVideosContainer = document.querySelector('.hero-videos');
    const prevHeroBtn = document.querySelector('.prev-hero');
    const nextHeroBtn = document.querySelector('.next-hero');
    const heroIndicatorsContainer = document.querySelector('.hero-indicators');
    const stopTrailerBtn = document.getElementById('stopTrailerBtn');

    // Carousel state
    let currentHeroIndex = 0;
    let heroInterval = null;
    const autoPlayDuration = 30000; // 30 seconds

    // Fetch hero data from API
    async function fetchHeroData() {
        try {
            const response = await fetch('/api/hero/videos');
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return await response.json();
        } catch (error) {
            console.error('Hero Section - Error:', error);
            return [];
        }
    }

    // Generate HTML for hero elements
    function createHeroElements(heroVideos) {
        const containers = [];
        const indicators = [];

        heroVideos.forEach((hero, index) => {
            // Create container element
            const container = document.createElement('div');
            container.className = `hero-video-container ${index === 0 ? 'active' : ''}`;
            container.dataset.index = index;

            // Generate container HTML
            container.innerHTML = `
                <video class="hero-bg-video" ${index === 0 ? 'autoplay' : ''} muted playsinline>
                    <source src="/media/video/${hero.ID}" type="video/mp4">
                    <img class="hero-bg" src="/media/image/${hero.ID}" alt="${hero.name}">
                </video>
                <div class="hero-overlay"></div>
                <div class="hero-content animate-fade-in">
                    <div class="hero-badge animate-slide-up">${hero.type || 'YENİ'}</div>
                    <h1 class="animate-slide-up">${hero.name}</h1>
                    <p class="animate-slide-up">${hero._content || 'İçerik açıklaması bulunmuyor'}</p>
                    <div class="hero-info animate-slide-up">
                        <div class="hero-stat"><i class="fas fa-film"></i> ${hero.category || 'Kategori'}</div>
                    </div>
                    <div class="hero-buttons animate-slide-up">
                        <button onclick="window.location.href='/watch?id=${hero.ID}'" class="pulse-button">
                            <i class="fas fa-play"></i> ŞİMDİ İZLE
                        </button>
                        <button class="glow-button trailer-btn" data-index="${index}">
                            <i class="fas fa-film"></i> FRAGMANI İZLE
                        </button>
                    </div>
                </div>
            `;
            containers.push(container);

            // Create indicator element
            const indicator = document.createElement('span');
            indicator.className = `hero-indicator ${index === 0 ? 'active' : ''}`;
            indicator.dataset.index = index;
            indicators.push(indicator);
        });

        return { containers, indicators };
    }

    // Initialize hero section with data from database
    async function initHeroDataSection() {
        try {
            // Fetch hero videos from API
            const heroVideos = await fetchHeroData();

            if (!heroVideos || heroVideos.length === 0) {
                console.error('No hero videos found');
                return;
            }

            // Clear existing content
            heroVideosContainer.innerHTML = '';
            heroIndicatorsContainer.innerHTML = '';

            // Create hero elements
            const { containers, indicators } = createHeroElements(heroVideos);

            // Add containers to DOM
            containers.forEach(container => {
                heroVideosContainer.appendChild(container);
            });

            // Add indicators to DOM
            indicators.forEach(indicator => {
                heroIndicatorsContainer.appendChild(indicator);
            });

            // Setup event listeners after elements are created
            setupEventListeners();
            startAutoPlay();

        } catch (error) {
            console.error('Failed to initialize hero section:', error);
        }
    }

    // Carousel functions
    function showHeroSlide(index) {
        const heroContainers = document.querySelectorAll('.hero-video-container');
        const heroIndicators = document.querySelectorAll('.hero-indicator');

        // Handle index wrapping
        if (index < 0) index = heroContainers.length - 1;
        if (index >= heroContainers.length) index = 0;

        // Update current index
        currentHeroIndex = index;

        // Remove active class from all containers and reset videos
        heroContainers.forEach(container => {
            container.classList.remove('active');
            const video = container.querySelector('video');
            if (video) {
                video.pause();
                video.currentTime = 0;
            }
        });

        // Remove active class from all indicators
        heroIndicators.forEach(indicator => {
            indicator.classList.remove('active');
        });

        // Add active class to current container and indicator
        heroContainers[index].classList.add('active');
        heroIndicators[index].classList.add('active');

        // Play the video for current slide
        const currentVideo = heroContainers[index].querySelector('video');
        if (currentVideo) {
            currentVideo.play().catch(e => console.log('Auto-play prevented:', e));
        }

        // Reset auto-play timer
        resetAutoPlay();
    }

    function startAutoPlay() {
        heroInterval = setInterval(() => {
            showHeroSlide(currentHeroIndex + 1);
        }, autoPlayDuration);
    }

    function resetAutoPlay() {
        clearInterval(heroInterval);
        startAutoPlay();
    }

    function setupEventListeners() {
        const heroIndicators = document.querySelectorAll('.hero-indicator');

        // Navigation buttons
        prevHeroBtn.addEventListener('click', () => {
            showHeroSlide(currentHeroIndex - 1);
        });

        nextHeroBtn.addEventListener('click', () => {
            showHeroSlide(currentHeroIndex + 1);
        });

        // Indicator clicks
        heroIndicators.forEach(indicator => {
            indicator.addEventListener('click', () => {
                const index = parseInt(indicator.dataset.index);
                showHeroSlide(index);
            });
        });

        // Trailer buttons
        document.querySelectorAll('.trailer-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const index = parseInt(btn.dataset.index);
                playTrailer(index);
            });
        });

        // Stop trailer button
        stopTrailerBtn.addEventListener('click', stopTrailer);
    }

    function playTrailer(index) {
        const heroContainers = document.querySelectorAll('.hero-video-container');
        const video = heroContainers[index].querySelector('video');

        if (video) {
            // Unmute and play video
            video.muted = false;
            video.currentTime = 0;
            video.play();

            // Show stop button
            stopTrailerBtn.style.display = 'flex';

            // Pause autoplay while trailer is playing
            clearInterval(heroInterval);
        }
    }

    function stopTrailer() {
        const activeContainer = document.querySelector('.hero-video-container.active');
        const video = activeContainer.querySelector('video');

        if (video) {
            // Mute and reset video
            video.muted = true;
            video.currentTime = 0;
            video.play();

            // Hide stop button
            stopTrailerBtn.style.display = 'none';

            // Resume autoplay
            startAutoPlay();
        }
    }

    // Initialize hero section with data
    initHeroDataSection();
}