import { initHeaderScroll } from './headerScroll.js';
import { initHeroCarousel } from './heroCarousel.js';
import { initContentCarousels } from './contentCarousel.js';
import { initNewSoapOperasSection } from './newSoapOperasSection.js';
import { initNewMoviesSection } from './newMoviesSection.js';
import { initWeeklyBestSection } from './weeklyBestSection.js';
import { initLogin } from './login.js';
import { initRegister } from './register.js';
import { initForgotPass } from './forgot-pass.js';
import { initFeaturedContent } from "./featuredContent.js";
import { initLoadedEpisodesSection } from "./loadedEpisodesSection.js";
import { initHeaderInteractions, initSearchExpansion } from "./searchExpansion.js";
import { initCalendar } from "./calendar.js";
import { initNotifications } from "./notifications.js";

window.addEventListener('load', () => {
    const loadingScreen = document.getElementById('loading-screen');
    if (loadingScreen) {
        loadingScreen.classList.add('fade-out');
        // Optional: remove from DOM after transition
        setTimeout(() => {
            loadingScreen.remove();
        }, 800);
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
    initNotifications();
    initAnnouncement();
});

function initAnnouncement() {
    const bar = document.getElementById('announcement-bar');
    const textSpan = document.getElementById('announcement-text');

    if (!bar || !textSpan) return;

    fetch('/api/settings/announcement')
        .then(res => res.json())
        .then(data => {
            if (data.active && data.text) {
                textSpan.textContent = data.text;
                bar.style.display = 'block';
            } else {
                bar.style.display = 'none';
            }
        })
        .catch(err => console.error('Failed to load announcement:', err));
}