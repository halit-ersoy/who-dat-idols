// Content carousels navigation functionality
export function initContentCarousels() {
    // Set up click handlers for all carousel navigation buttons
    document.querySelectorAll('.carousel-nav').forEach(button => {
        button.addEventListener('click', function () {
            const carouselId = this.getAttribute('data-carousel');
            const carousel = document.getElementById(carouselId);
            const direction = this.classList.contains('prev-btn') ? -1 : 1;
            const scrollAmount = carousel.clientWidth * 0.8 * direction;

            carousel.scrollBy({
                left: scrollAmount,
                behavior: 'smooth'
            });

            // Update button states after scrolling
            setTimeout(() => {
                updateCarouselNavButtons(carousel);
            }, 500);
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
}