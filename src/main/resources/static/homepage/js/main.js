// Main.js - Entry point for HomePage scripts
import { initHeaderScroll } from './headerScroll.js';
import { initHeroCarousel } from './heroCarousel.js';
import { initContentCarousels } from './contentCarousel.js';
import { initTrendingSection } from './trendingSection.js';
import { initNewContentSection } from './newContentSection.js';

document.addEventListener('DOMContentLoaded', function() {
    // Initialize all page components
    initHeaderScroll();
    initHeroCarousel();
    initContentCarousels();
    initTrendingSection();
    initNewContentSection();
});