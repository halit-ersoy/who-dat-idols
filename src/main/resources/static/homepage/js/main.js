import { initHeaderScroll } from './headerScroll.js?v=2';
import { initHeroCarousel } from './heroCarousel.js?v=2';
import { initContentCarousels } from './contentCarousel.js?v=2';
import { initNewSoapOperasSection } from './newSoapOperasSection.js?v=2';
import { initNewMoviesSection } from './newMoviesSection.js?v=2';
import { initWeeklyBestSection } from './weeklyBestSection.js?v=2';
import { initLogin } from './login.js?v=2';
import { initRegister } from './register.js?v=2';
import { initForgotPass } from './forgot-pass.js?v=2';
import { initFeaturedContent } from "./featuredContent.js?v=2";
import { initLoadedEpisodesSection } from "./loadedEpisodesSection.js?v=2";
import { initHeaderInteractions, initSearchExpansion } from "./searchExpansion.js?v=2";
import { initCalendar } from "./calendar.js?v=2";
import { initNotifications } from "./notifications.js?v=2";
import { initFeedback } from "./feedback.js?v=2";
import { initMessagingManager } from "./messagingManager.js?v=2";

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
    initFeedback();
    initMessagingManager();
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