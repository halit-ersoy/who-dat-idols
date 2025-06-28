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
import {initLoadedEpisodesSection} from "./loadedEpisodesSection.js";
import {initHeaderInteractions, initSearchExpansion} from "./searchExpansion.js";
import {initCalendar} from "./calendar.js";

window.addEventListener('load', () => {
    const loadingScreen = document.getElementById('loading-screen');
    if (loadingScreen) {
        loadingScreen.style.display = 'none';
    }
});

// Then add this to the DOMContentLoaded callback function
document.addEventListener('DOMContentLoaded', () => {
    initHeaderScroll();
    initSearchExpansion();
    initHeaderInteractions();
    initHeroCarousel();
    initContentCarousels();
    initFeaturedContent();
    initNewSoapOperasSection();
    initNewMoviesSection();
    initWeeklyBestSection();
    initLoadedEpisodesSection(); // Add this line
    initLogin();
    initRegister();
    initForgotPass();
    initCalendar();
});