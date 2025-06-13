import {initHeaderScroll} from './headerScroll.js';
import {initHeroCarousel} from './heroCarousel.js';
import {initContentCarousels} from './contentCarousel.js';
import {initNewSoapOperasSection} from './newSoapOperasSection.js';
import {initNewMoviesSection} from './newMoviesSection.js';
import {initWeeklyBestSection} from './weeklyBestSection.js';
import {initLogin} from './login.js';
import {initRegister} from './register.js';
import {initForgotPass} from './forgot-pass.js';
import {initFeaturedContent} from "./featuredContent.js";

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
    initFeaturedContent();
    initNewSoapOperasSection();
    initNewMoviesSection();
    initWeeklyBestSection();
    initLogin();
    initRegister();
    initForgotPass();
});
