export function initContentCarousels() {
    const navButtons = document.querySelectorAll('.carousel-nav');
    navButtons.forEach(button => {
        button.addEventListener('click', () => {
            const carouselId = button.getAttribute('data-carousel');
            const carousel = document.getElementById(carouselId);
            if (!carousel) return;
            const direction = button.classList.contains('prev-btn') ? -1 : 1;
            const scrollAmount = carousel.clientWidth * 0.8 * direction;
            carousel.scrollBy({left: scrollAmount, behavior: 'smooth'});
            setTimeout(() => updateCarouselNavButtons(carousel), 500);
        });
    });

    const updateCarouselNavButtons = (carousel) => {
        const carouselId = carousel.id;
        const prevBtn = document.querySelector(`.prev-btn[data-carousel="${carouselId}"]`);
        const nextBtn = document.querySelector(`.next-btn[data-carousel="${carouselId}"]`);
        if (prevBtn) {
            if (carousel.scrollLeft <= 0) {
                prevBtn.classList.add('disabled');
            } else {
                prevBtn.classList.remove('disabled');
            }
        }
        if (nextBtn) {
            if (carousel.scrollLeft + carousel.clientWidth >= carousel.scrollWidth - 10) {
                nextBtn.classList.add('disabled');
            } else {
                nextBtn.classList.remove('disabled');
            }
        }
    };

    const carousels = document.querySelectorAll('.carousel');
    carousels.forEach(carousel => {
        updateCarouselNavButtons(carousel);
        carousel.addEventListener('scroll', () => updateCarouselNavButtons(carousel));
    });
}
