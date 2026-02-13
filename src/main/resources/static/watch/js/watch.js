import { initVideoControls } from './videoControls.js';
import { initListModal } from './listModal.js';
import { initCommentsSection } from './comments.js';
import { initContentDetails } from './contentDetails.js';
import { initEpisodeSelection } from './episodeSelection.js';
import { initSimilarContent } from './similarContent.js';
import { handleImageSkeleton } from '../../elements/userLogged.js';

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const videoId = urlParams.get('id');

    initVideoControls(videoId);
    initListModal();
    initCommentsSection(videoId);
    initContentDetails(videoId);
    initEpisodeSelection(videoId);
    initSimilarContent(videoId);

    // Recommendations items are static in HTML for now (placeholder)
    document.querySelectorAll('.rec-card .rec-image img').forEach(img => {
        handleImageSkeleton(img);
    });
});
