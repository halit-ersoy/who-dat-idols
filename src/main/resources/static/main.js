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

    // Trending "View All" functionality
    // Sample trending items data (in practice, this would come from your backend)
    const trendingItems = Array.from({ length: 60 }, (_, i) => ({
        id: i + 1,
        title: `Trending Title ${i + 1}`,
        year: 2023 - Math.floor(i / 10),
        genre: ['Romantik', 'Komedi', 'Drama', 'Aksiyon', 'Fantastik'][i % 5],
        duration: `${40 + i % 20} dk`,
        image: [
            'https://i.ibb.co/ZM7SxXh/bl-1.jpg',
            'https://i.ibb.co/hRMtw0k/bl-2.jpg',
            'https://i.ibb.co/jzy6zDm/bl-3.jpg',
            'https://i.ibb.co/y5QfMxk/kdrama-1.jpg',
            'https://i.ibb.co/Ntf5Rkx/kdrama-2.jpg',
            'https://i.ibb.co/YB0BnF3/jdrama-1.jpg',
            'https://i.ibb.co/M2BgHph/cdrama-1.jpg',
            'https://i.ibb.co/gW6K4T1/mutya-card.jpg'
        ][i % 8]
    }));

    const viewAllBtn = document.querySelector('.trending .view-all');
    const trendingAllSection = document.getElementById('trending-all');
    const trendingAllGrid = document.querySelector('.trending-all-grid');
    const loadMoreBtn = document.getElementById('load-more-trending');
    const closeAllBtn = document.querySelector('.close-all-btn');

    let currentItemsLoaded = 0;
    const itemsPerLoad = 20;

    // Create single trending item HTML
    function createTrendingItemHTML(item, index) {
        return `
            <a href="/watch?id=${item.id}" class="card trending-item" style="animation-delay: ${index * 0.05}s">
                <div class="card-image-container">
                    <img src="${item.image}" alt="${item.title}">
                    <div class="play-icon"><i class="fas fa-play"></i></div>
                </div>
                <div class="card-content">
                    <p class="card-title">${item.title}</p>
                    <p class="card-info">${item.year} • ${item.genre} • ${item.duration}</p>
                </div>
            </a>
        `;
    }

    // Load items in batches
    function loadItems(startIndex, count) {
        const fragment = document.createDocumentFragment();
        const endIndex = Math.min(startIndex + count, trendingItems.length);

        for (let i = startIndex; i < endIndex; i++) {
            const itemHTML = createTrendingItemHTML(trendingItems[i], i - startIndex);
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = itemHTML;
            fragment.appendChild(tempDiv.firstElementChild);
        }

        trendingAllGrid.appendChild(fragment);
        currentItemsLoaded = endIndex;

        // Hide load more button if all items loaded
        if (currentItemsLoaded >= trendingItems.length) {
            loadMoreBtn.classList.add('hidden');
        }
    }

    // View All button click event
    viewAllBtn.addEventListener('click', function(e) {
        e.preventDefault();

        // Reset grid and counter
        trendingAllGrid.innerHTML = '';
        currentItemsLoaded = 0;

        // Show the section
        trendingAllSection.classList.remove('hidden');

        // Scroll to the section
        trendingAllSection.scrollIntoView({ behavior: 'smooth' });

        // Load initial items
        loadItems(0, itemsPerLoad);

        // Show load more button if needed
        if (trendingItems.length > itemsPerLoad) {
            loadMoreBtn.classList.remove('hidden');
        } else {
            loadMoreBtn.classList.add('hidden');
        }
    });

    // Load more button click event
    loadMoreBtn.addEventListener('click', function() {
        loadItems(currentItemsLoaded, itemsPerLoad);

        // Add loading animation
        this.classList.add('loading');
        setTimeout(() => {
            this.classList.remove('loading');
        }, 800);
    });

    // Close all button click event
    closeAllBtn.addEventListener('click', function() {
        trendingAllSection.classList.add('hidden');

        // Scroll back to trending section
        document.querySelector('.trending').scrollIntoView({ behavior: 'smooth' });

        // After animation completes, clear the grid
        setTimeout(() => {
            trendingAllGrid.innerHTML = '';
            currentItemsLoaded = 0;
        }, 500);
    });
});