import { initVideoControls } from './videoControls.js';
import { initListModal } from './listModal.js';
import { initCommentsSection } from './comments.js';
import { initContentDetails } from './contentDetails.js';

document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const videoId   = urlParams.get('id');

    initVideoControls(videoId);
    initListModal();
    initCommentsSection(videoId);
    initContentDetails(videoId);
});
