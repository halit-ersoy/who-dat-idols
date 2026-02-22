import { handleImageSkeleton } from '../../elements/userLogged.js';

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
    let currentTrailerButton = null; // Store reference to the current trailer button
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

            const category = hero.category ? hero.category.replace(/,$/, '') : 'Kategori';

            // Generate container HTML
            container.innerHTML = `
                <video class="hero-bg-video" ${index === 0 ? 'autoplay' : ''} muted playsinline>
                    <source src="/media/video/${hero.ID}" type="video/mp4">
                    <div class="img-skeleton">
                        <img class="hero-bg" src="/media/image/${hero.ID}" alt="${hero.name}">
                    </div>
                </video>
                <div class="hero-overlay"></div>
                <div class="hero-content animate-fade-in">
                    <div class="hero-badge animate-slide-up">${hero.type || 'YENİ'}</div>
                    <h1 class="animate-slide-up">${hero.name}</h1>
                    <p class="animate-slide-up">${hero._content || 'İçerik açıklaması bulunmuyor'}</p>
                    <div class="hero-info animate-slide-up">
                        <div class="hero-stat"><i class="fas fa-film"></i> ${category}</div>
                    </div>
                    <div class="hero-buttons animate-slide-up">
                        <button onclick="window.location.href='${hero.slug ? '/' + hero.slug : '/' + hero.ReferenceId}'" class="pulse-button">
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
                // Handle skeleton for the background image
                const img = container.querySelector('.hero-bg');
                handleImageSkeleton(img);
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
                video.currentTime = 0; // Reset time when changing slides
                video.muted = true;
            }

            // Reset any trailer buttons that might have been changed
            resetTrailerButtons(container);
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
            // Check if the video is already playing or about to play
            const playPromise = currentVideo.play();
            if (playPromise !== undefined) {
                playPromise.catch(e => {
                    if (e.name !== 'AbortError') {
                        // console.log('Auto-play prevented:', e);
                    }
                });
            }
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

    function resetTrailerButtons(container) {
        // Find the trailer button in this container
        const trailerBtn = container.querySelector('.trailer-btn');
        if (trailerBtn) {
            // Reset button text and icon to original state
            trailerBtn.innerHTML = '<i class="fas fa-film"></i> FRAGMANI İZLE';
            trailerBtn.classList.remove('watching-trailer');
        }
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
                // If button is currently in "stop" mode, call stopTrailer
                if (btn.classList.contains('watching-trailer')) {
                    stopTrailer();
                } else {
                    // Otherwise play the trailer
                    const index = parseInt(btn.dataset.index);
                    playTrailer(index, btn);
                }
            });
        });
    }

    function playTrailer(index, button) {
        const heroContainers = document.querySelectorAll('.hero-video-container');
        const video = heroContainers[index].querySelector('video');

        if (video) {
            // Store reference to the current button
            currentTrailerButton = button;

            // Change button to "STOP" mode
            button.innerHTML = '<i class="fas fa-pause"></i> İZLEMEYİ DURDUR';
            button.classList.add('watching-trailer');

            // Unmute and play video without resetting position
            video.muted = false;
            video.play();

            // Pause autoplay while trailer is playing
            clearInterval(heroInterval);
        }
    }

    function stopTrailer() {
        const activeContainer = document.querySelector('.hero-video-container.active');
        const video = activeContainer.querySelector('video');

        if (video) {
            // Mute video but don't reset position
            video.muted = true;
            video.play();

            // Reset the trailer button
            if (currentTrailerButton) {
                currentTrailerButton.innerHTML = '<i class="fas fa-film"></i> FRAGMANI İZLE';
                currentTrailerButton.classList.remove('watching-trailer');
                currentTrailerButton = null;
            }

            // Resume autoplay
            startAutoPlay();
        }
    }

    // Initialize hero section with data
    initHeroDataSection();
}