export function initHeroCarousel() {
    const heroContainers = document.querySelectorAll('.hero-video-container');
    const heroIndicators = document.querySelectorAll('.hero-indicator');
    const prevHeroBtn = document.querySelector('.prev-hero');
    const nextHeroBtn = document.querySelector('.next-hero');
    let currentHeroIndex = 0;
    let heroInterval;
    const autoPlayDuration = 30000;

    function showHeroSlide(index) {
        if (index < 0) {
            index = heroContainers.length - 1;
        } else if (index >= heroContainers.length) {
            index = 0;
        }
        heroContainers.forEach(container => {
            container.classList.remove('active');
            const video = container.querySelector('video');
            video.pause();
            video.currentTime = 0;
        });
        heroContainers[index].classList.add('active');
        const activeVideo = heroContainers[index].querySelector('video');
        activeVideo.play();
        heroIndicators.forEach(indicator => indicator.classList.remove('active'));
        heroIndicators[index].classList.add('active');
        currentHeroIndex = index;
    }

    function startHeroAutoplay() {
        clearInterval(heroInterval);
        heroInterval = setInterval(() => {
            showHeroSlide(currentHeroIndex + 1);
        }, autoPlayDuration);
    }

    prevHeroBtn.addEventListener('click', () => {
        showHeroSlide(currentHeroIndex - 1);
        startHeroAutoplay();
    });

    nextHeroBtn.addEventListener('click', () => {
        showHeroSlide(currentHeroIndex + 1);
        startHeroAutoplay();
    });

    heroIndicators.forEach((indicator, index) => {
        indicator.addEventListener('click', () => {
            showHeroSlide(index);
            startHeroAutoplay();
        });
    });

    showHeroSlide(0);
    startHeroAutoplay();

    document.addEventListener('visibilitychange', () => {
        if (document.hidden) {
            clearInterval(heroInterval);
        } else {
            startHeroAutoplay();
        }
    });
}
