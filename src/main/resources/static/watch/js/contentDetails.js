// contentDetails.js
import { handleImageSkeleton } from '../../elements/userLogged.js';

export function initContentDetails(videoId) {
    const detailsToggle = document.querySelector('.details-toggle');
    const detailsContainer = document.querySelector('.content-details-container');

    detailsToggle?.addEventListener('click', () => {
        detailsToggle.classList.toggle('active');
        detailsContainer.classList.toggle('open');
    });

    if (videoId) loadContentDetails(videoId);
}

async function loadContentDetails(id) {
    try {
        const response = await fetch(`/api/video/details?id=${id}`);
        if (!response.ok) throw new Error('Details not found');
        const data = await response.json();

        // Update Title - Both in content details and the top video header
        const titleEl = document.getElementById('title');
        const contentTitleEl = document.getElementById('contentTitle');
        if (titleEl) titleEl.innerText = data.title;
        if (contentTitleEl) contentTitleEl.innerText = data.title;

        // Update document title
        document.title = `${data.title} - Who Dat Idols?`;

        const posterImg = document.getElementById('contentPoster');
        // Use the generic image endpoint we know works
        posterImg.src = `/media/image/${id}`;
        handleImageSkeleton(posterImg);

        // Rating - placeholder for now as logic isn't in details endpoint yet
        // document.getElementById('contentRating').textContent = data.rating || "N/A";

        document.getElementById('releaseYear').textContent = data.year;
        document.getElementById('contentDuration').textContent = data.duration;
        document.getElementById('contentLanguage').textContent = data.language;
        document.getElementById('contentPlot').textContent = data.plot;

        // Season/Episode/Navigation Visibility (Only for episodes)
        const seasonSection = document.getElementById('seasonSection');
        const episodeNav = document.getElementById('episodeNav');
        const episodeDrawer = document.getElementById('episodeSection');

        if (data.type === 'episode') {
            if (seasonSection) seasonSection.style.display = 'block';
            if (episodeNav) episodeNav.style.display = 'flex';
            if (episodeDrawer) episodeDrawer.style.display = 'block';

            document.getElementById('seasonNumber').textContent = `Sezon ${data.season}`;
            document.getElementById('episodeNumber').textContent = `Bölüm ${data.episode}`;
            document.getElementById('totalEpisodes').style.display = 'none'; // Hide total for now
        } else {
            if (seasonSection) seasonSection.style.display = 'none';
            if (episodeNav) episodeNav.style.display = 'none';
            if (episodeDrawer) episodeDrawer.style.display = 'none';
        }

        const genresEl = document.getElementById('genreTags');
        genresEl.innerHTML = '';
        if (data.genres) {
            data.genres.forEach(g => {
                const span = document.createElement('span');
                span.className = 'genre-tag';
                span.textContent = g.trim();
                genresEl.appendChild(span);
            });
        }

        // Cast - placeholder or need to fetch separately? 
        // For now clear it or leave empty as API doesn't return cast yet
        const castList = document.getElementById('castList');
        castList.innerHTML = '<p style="color:#aaa; font-size:0.9em;">Oyuncu bilgisi bulunamadı.</p>';

    } catch (error) {
        console.error('Content details loading error:', error);
        document.getElementById('contentTitle').innerText = "Detaylar yüklenemedi";
    }
}
