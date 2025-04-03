export function initHeroCarousel() {
    const heroContainers = document.querySelectorAll('.hero-video-container');
    const heroIndicators = document.querySelectorAll('.hero-indicator');
    const prevHeroBtn = document.querySelector('.prev-hero');
    const nextHeroBtn = document.querySelector('.next-hero');

    let currentHeroIndex = 0;
    let heroInterval = null;
    const autoPlayDuration = 30000;

    function showHeroSlide(index) {
        const totalSlides = heroContainers.length;
        index = (index + totalSlides) % totalSlides;

        // Tüm container'lardan active sınıfı kaldır, videoları resetle
        heroContainers.forEach(container => {
            container.classList.remove('active');
            const video = container.querySelector('video');
            if (video) {
                video.pause();
                video.currentTime = 0;
            }
        });

        // Yeni aktif slide'ı ata
        const activeContainer = heroContainers[index];
        activeContainer.classList.add('active');

        // Videoyu oynat
        const activeVideo = activeContainer.querySelector('video');
        if (activeVideo) {
            const playPromise = activeVideo.play();
            if (playPromise !== undefined) {
                playPromise.catch(error => {
                    console.error('HeroCarousel: Video oynatma hatası:', error);
                });
            }
        }

        // Indicator güncelle
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

    // İlk slide'ı göster + autoplay başlat
    showHeroSlide(0);
    startHeroAutoplay();

    /***************************************************
     *  FRAGMANI İZLE - İZLEMEYİ DURDUR KONTROL KODLARI
     ***************************************************/
    const fragmanButtons = document.querySelectorAll(".glow-button");
    const stopTrailerBtn = document.getElementById("stopTrailerBtn");
    const heroNavButtons = document.querySelectorAll(".hero-nav");

    // Aktif fragman container & video referansı
    let activeHeroContainer = null;
    let activeVideo = null;

    // FRAGMANI İZLE
    fragmanButtons.forEach((btn) => {
        btn.addEventListener("click", function() {
            const container = btn.closest(".hero-video-container");
            if (!container) return;

            // Yazıları gizle
            const heroContent = container.querySelector(".hero-content");
            if (heroContent) {
                heroContent.style.display = "none";
            }

            // Sağ-sol okları gizle
            heroNavButtons.forEach(nav => nav.style.display = "none");

            // Videonun sesi aç ve LOOP aktif
            const video = container.querySelector("video");
            if (video) {
                video.muted = false;
                video.loop = true;  // DÖNGÜYE AL
                video.play().catch(err => console.error("Video oynatılamadı:", err));
            }

            // Autoplay devre dışı bırak (30sn geçişi durdur)
            clearInterval(heroInterval);

            // Indicators gizle
            heroIndicators.forEach(indicator => {
                indicator.style.display = "none";
            });

            // "İZLEMEYİ DURDUR" butonunu göster
            stopTrailerBtn.style.display = "inline-block";

            // Aktif fragman kaydı
            activeHeroContainer = container;
            activeVideo = video;
        });
    });

    // İZLEMEYİ DURDUR
    stopTrailerBtn.addEventListener("click", function() {
        if (!activeHeroContainer) return;

        // Yazıları geri getir
        const heroContent = activeHeroContainer.querySelector(".hero-content");
        if (heroContent) {
            heroContent.style.display = "";
        }

        // Sağ-sol okları geri getir
        heroNavButtons.forEach(nav => nav.style.display = "");

        // Videoyu sessize al (arka planda oynayabilir) & loop'u kapatmak isterseniz:
        if (activeVideo) {
            activeVideo.muted = true;
            activeVideo.loop = false; // Döngüyü kapatmak isterseniz
        }

        // Autoplay tekrar başlasın
        startHeroAutoplay();

        // Indicators geri gelsin
        heroIndicators.forEach(indicator => {
            indicator.style.display = "";
        });

        // "İZLEMEYİ DURDUR" butonunu gizle
        stopTrailerBtn.style.display = "none";

        // Artık aktif fragman yok
        activeHeroContainer = null;
        activeVideo = null;
    });
}
