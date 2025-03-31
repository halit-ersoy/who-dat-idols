import { initHeaderScroll } from './headerScroll.js';
import { initHeroCarousel } from './heroCarousel.js';
import { initContentCarousels } from './contentCarousel.js';
import { initNewSoapOperasSection } from './newSoapOperasSection.js';
import { initNewMoviesSection } from './newMoviesSection.js';
import { initWeeklyBestSection } from './weeklyBestSection.js';
import { initLogin } from './login.js';

window.addEventListener('load', () => {
    const loadingScreen = document.getElementById('loading-screen');
    if (loadingScreen) {
        loadingScreen.style.display = 'none';
    }
});

document.addEventListener('DOMContentLoaded', () => {
    initHeaderScroll();
    initHeroCarousel();
    initContentCarousels();
    initNewSoapOperasSection();
    initNewMoviesSection();
    initWeeklyBestSection();
    initLogin();
});
