// Hero Video Carousel functionality
export function initHeroCarousel() {
    const heroContainers = document.querySelectorAll('.hero-video-container');
    const heroIndicators = document.querySelectorAll('.hero-indicator');
    const prevHeroBtn = document.querySelector('.prev-hero');
    const nextHeroBtn = document.querySelector('.next-hero');
    let currentHeroIndex = 0;
    let heroInterval;
    const autoPlayDuration = 30000; // 30 seconds

    // Function to show a specific hero slide
    function showHeroSlide(index) {
        // Handle index wrapping for looping
        if (index < 0) {
            index = heroContainers.length - 1;
        } else if (index >= heroContainers.length) {
            index = 0;
        }

        // Hide all slides and pause videos
        heroContainers.forEach(container => {
            container.classList.remove('active');
            const video = container.querySelector('video');
            video.pause();
            video.currentTime = 0;
        });

        // Show selected slide and play its video
        heroContainers[index].classList.add('active');
        const activeVideo = heroContainers[index].querySelector('video');
        activeVideo.play();

        // Update indicators
        heroIndicators.forEach(indicator => {
            indicator.classList.remove('active');
        });
        heroIndicators[index].classList.add('active');

        // Update current index
        currentHeroIndex = index;
    }

    // Initialize autoplay
    function startHeroAutoplay() {
        clearInterval(heroInterval);
        heroInterval = setInterval(() => {
            showHeroSlide(currentHeroIndex + 1);
        }, autoPlayDuration);
    }

    // Navigation event listeners
    prevHeroBtn.addEventListener('click', () => {
        showHeroSlide(currentHeroIndex - 1);
        startHeroAutoplay(); // Reset timer
    });

    nextHeroBtn.addEventListener('click', () => {
        showHeroSlide(currentHeroIndex + 1);
        startHeroAutoplay(); // Reset timer
    });

    // Indicator click events
    heroIndicators.forEach((indicator, index) => {
        indicator.addEventListener('click', () => {
            showHeroSlide(index);
            startHeroAutoplay(); // Reset timer
        });
    });

    // Initialize first slide and start autoplay
    showHeroSlide(0);
    startHeroAutoplay();

    // Pause autoplay when tab is not visible
    document.addEventListener('visibilitychange', () => {
        if (document.hidden) {
            clearInterval(heroInterval);
        } else {
            startHeroAutoplay();
        }
    });
}