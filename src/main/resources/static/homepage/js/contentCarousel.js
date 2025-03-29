export function initContentCarousels() {
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

            setTimeout(() => {
                updateCarouselNavButtons(carousel);
            }, 500);
        });
    });

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

    document.querySelectorAll('.carousel').forEach(carousel => {
        updateCarouselNavButtons(carousel);
        carousel.addEventListener('scroll', function () {
            updateCarouselNavButtons(this);
        });
    });
}
