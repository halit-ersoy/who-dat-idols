// Header background change on scroll
window.addEventListener('scroll', function () {
    if (window.scrollY > 50) {
        document.getElementById('header').classList.add('scrolled');
    } else {
        document.getElementById('header').classList.remove('scrolled');
    }
});

// Hero Video Carousel
document.addEventListener('DOMContentLoaded', function() {
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

    // Initialize carousels
    // Set up click handlers for all carousel navigation buttons
    document.querySelectorAll('.carousel-nav').forEach(button => {
        button.addEventListener('click', function () {
            const carouselId = this.getAttribute('data-carousel');
            const carousel = document.getElementById(carouselId);
            const scrollAmount = carousel.clientWidth * 0.8;

            // Check if prev button and already at the start
            if (this.classList.contains('prev-btn') && carousel.scrollLeft <= 0) {
                carousel.scrollLeft = 0;
                return;
            }

            // Check if next button and already at the end
            if (this.classList.contains('next-btn') &&
                carousel.scrollLeft + carousel.clientWidth >= carousel.scrollWidth - 10) {
                return;
            }

            if (this.classList.contains('prev-btn')) {
                carousel.scrollBy({
                    left: -scrollAmount,
                    behavior: 'smooth'
                });
            } else {
                carousel.scrollBy({
                    left: scrollAmount,
                    behavior: 'smooth'
                });
            }

            // Update button states
            updateCarouselNavButtons(carousel);
        });
    });

    // Function to update nav button states
    function updateCarouselNavButtons(carousel) {
        const carouselId = carousel.id;
        const prevBtn = document.querySelector(`.prev-btn[data-carousel="${carouselId}"]`);
        const nextBtn = document.querySelector(`.next-btn[data-carousel="${carouselId}"]`);

        if (carousel.scrollLeft <= 0) {
            prevBtn.classList.add('disabled');
        } else {
            prevBtn.classList.remove('disabled');
        }

        if (carousel.scrollLeft + carousel.clientWidth >= carousel.scrollWidth - 10) {
            nextBtn.classList.add('disabled');
        } else {
            nextBtn.classList.remove('disabled');
        }
    }

    // Initial check for all carousels
    document.querySelectorAll('.carousel').forEach(carousel => {
        updateCarouselNavButtons(carousel);

        // Listen for scroll events to update buttons
        carousel.addEventListener('scroll', function () {
            updateCarouselNavButtons(this);
        });
    });
});