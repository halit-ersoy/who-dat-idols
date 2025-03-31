export function initHeroCarousel() {
    const heroContainers = document.querySelectorAll('.hero-video-container');
    if (!heroContainers.length) {
        console.error('HeroCarousel: Hiçbir hero video container bulunamadı.');
        return;
    }
    const heroIndicators = document.querySelectorAll('.hero-indicator');
    const prevHeroBtn = document.querySelector('.prev-hero');
    const nextHeroBtn = document.querySelector('.next-hero');

    let currentHeroIndex = 0;
    let heroInterval = null;
    const autoPlayDuration = 30000;

    function showHeroSlide(index) {
        const totalSlides = heroContainers.length;
        // Normalize index: mod işlemi ile geçerli aralığa çekiyoruz
        index = (index + totalSlides) % totalSlides;

        // Tüm container'lardan active sınıfını kaldır ve varsa videoları resetle
        heroContainers.forEach(container => {
            container.classList.remove('active');
            const video = container.querySelector('video');
            if (video) {
                video.pause();
                video.currentTime = 0;
            }
        });

        // Seçilen slide'a active sınıfını ekle
        const activeContainer = heroContainers[index];
        activeContainer.classList.add('active');

        // Videoyu oynat ve hata varsa yakala
        const activeVideo = activeContainer.querySelector('video');
        if (activeVideo) {
            const playPromise = activeVideo.play();
            if (playPromise !== undefined) {
                playPromise.catch(error => {
                    console.error('HeroCarousel: Video oynatma hatası:', error);
                });
            }
        }

        // Eğer indicator'lar mevcutsa güncelle
        if (heroIndicators.length === totalSlides) {
            heroIndicators.forEach(indicator => indicator.classList.remove('active'));
            if (heroIndicators[index]) {
                heroIndicators[index].classList.add('active');
            }
        }
        currentHeroIndex = index;
    }

    function startHeroAutoplay() {
        clearInterval(heroInterval);
        heroInterval = setInterval(() => {
            showHeroSlide(currentHeroIndex + 1);
        }, autoPlayDuration);
    }

    if (prevHeroBtn) {
        prevHeroBtn.addEventListener('click', () => {
            showHeroSlide(currentHeroIndex - 1);
            startHeroAutoplay();
        });
    }

    if (nextHeroBtn) {
        nextHeroBtn.addEventListener('click', () => {
            showHeroSlide(currentHeroIndex + 1);
            startHeroAutoplay();
        });
    }

    if (heroIndicators.length) {
        heroIndicators.forEach((indicator, index) => {
            indicator.addEventListener('click', () => {
                showHeroSlide(index);
                startHeroAutoplay();
            });
        });
    }

    // İlk slide'ı göster ve autoplay başlat
    showHeroSlide(0);
    startHeroAutoplay();

    // Sayfa görünürlüğü değiştiğinde autoplay'i durdurup yeniden başlat
    document.addEventListener('visibilitychange', () => {
        if (document.hidden) {
            clearInterval(heroInterval);
        } else {
            startHeroAutoplay();
        }
    });
}
