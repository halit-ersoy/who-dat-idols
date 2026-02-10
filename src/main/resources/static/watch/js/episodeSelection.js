// episodeSelection.js
import { handleImageSkeleton } from '../../elements/userLogged.js';

export async function initEpisodeSelection(videoId) {
    const section = document.querySelector('.episode-selection-section');
    const toggle = document.querySelector('.episode-toggle');
    const container = document.querySelector('.episode-selection-container');
    const seasonTabs = document.getElementById('seasonTabs');
    const episodesGrid = document.getElementById('episodesGrid');

    if (!section || !toggle || !container || !seasonTabs || !episodesGrid) return;

    let allEpisodes = [];
    let currentSeason = 1;
    let isLoaded = false;

    // Toggle drawer
    toggle.addEventListener('click', async () => {
        toggle.classList.toggle('active');
        container.classList.toggle('open');

        // Load episodes on first open
        if (!isLoaded && container.classList.contains('open')) {
            await fetchAndRenderEpisodes();
            isLoaded = true;
        }
    });

    // Auto-open if it's a series ID
    await checkAutoOpen();

    async function checkAutoOpen() {
        try {
            let response = await fetch(`/api/soapoperas/${videoId}/episodes`);
            if (response.ok) {
                const eps = await response.json();
                if (eps && eps.length > 0) {
                    allEpisodes = eps;
                    renderSeasonTabs();
                    renderEpisodes(allEpisodes[0].seasonNumber);
                    // Auto-expand drawer for series
                    toggle.classList.add('active');
                    container.classList.add('open');
                    isLoaded = true;
                    // Show section for series
                    section.style.display = 'block';
                    return;
                }
            }

            // No episodes found - likely a movie, hide the section
            section.style.display = 'none';
        } catch (err) {
            console.error('Auto-open check failed:', err);
            // On error, hide the section
            section.style.display = 'none';
        }
    }

    async function fetchAndRenderEpisodes() {
        if (!videoId) return;
        episodesGrid.innerHTML = '<div class="loading-episodes"><i class="fas fa-spinner fa-spin"></i> Bölümler yükleniyor...</div>';

        try {
            let idToFetch = videoId;
            let response = await fetch(`/api/soapoperas/${idToFetch}/episodes`);
            let data = [];

            if (response.ok) {
                data = await response.json();
            }

            // If empty or error, try fetching via parent
            if (!response.ok || data.length === 0) {
                const parentRes = await fetch(`/api/soapoperas/episode/${videoId}/parent`);
                if (parentRes.ok) {
                    const parent = await parentRes.json();
                    idToFetch = parent.id;
                    const secondRes = await fetch(`/api/soapoperas/${idToFetch}/episodes`);
                    if (secondRes.ok) data = await secondRes.json();
                }
            }

            if (data.length === 0) {
                episodesGrid.innerHTML = '<div class="no-episodes">Bu içerik için bölüm listesi bulunamadı.</div>';
                section.style.display = 'none';
                return;
            }

            allEpisodes = data;
            section.style.display = 'block';
            renderSeasonTabs();

            // Find current episode's season if possible, otherwise first season
            const currentEp = allEpisodes.find(ep => ep.id === videoId);
            currentSeason = currentEp ? currentEp.seasonNumber : allEpisodes[0].seasonNumber;

            // Update active tab in UI
            document.querySelectorAll('.season-tab').forEach(t => {
                if (parseInt(t.dataset.season) === currentSeason) t.classList.add('active');
                else t.classList.remove('active');
            });

            renderEpisodes(currentSeason);
        } catch (error) {
            console.error('Episode Selection Error:', error);
            episodesGrid.innerHTML = `<div class="error-message">Hata: Bölümler yüklenemedi.</div>`;
        }
    }

    function renderSeasonTabs() {
        const seasons = [...new Set(allEpisodes.map(ep => ep.seasonNumber))].sort((a, b) => a - b);
        seasonTabs.innerHTML = '';

        seasons.forEach(season => {
            const tab = document.createElement('div');
            tab.className = 'season-tab';
            if (season === currentSeason) tab.classList.add('active');
            tab.dataset.season = season;
            tab.textContent = `${season}. Sezon`;

            tab.onclick = () => {
                currentSeason = season;
                document.querySelectorAll('.season-tab').forEach(t => t.classList.remove('active'));
                tab.classList.add('active');
                renderEpisodes(season);
            };
            seasonTabs.appendChild(tab);
        });
    }

    function renderEpisodes(season) {
        episodesGrid.innerHTML = '';
        const filtered = allEpisodes.filter(ep => ep.seasonNumber === season);

        filtered.forEach(ep => {
            const card = document.createElement('div');
            card.className = 'episode-card';
            if (ep.id === videoId) card.classList.add('active');

            const mins = ep.duration || 0;
            const durationStr = mins > 0 ? `${mins} dk` : 'Bilinmiyor';

            card.innerHTML = `
                <div class="episode-thumbnail">
                    <img src="/media/image/${ep.id}" alt="${ep.name}">
                    <div class="episode-thumbnail-icon"><i class="fas fa-film"></i></div>
                    <div class="episode-duration">${durationStr}</div>
                </div>
                <div class="episode-info">
                    <div class="episode-number-label">${ep.seasonNumber}. Sezon ${ep.episodeNumber}. Bölüm</div>
                    <div class="episode-name-label">${ep.name}</div>
                </div>
            `;

            card.onclick = () => {
                window.location.href = `/watch?id=${ep.id}`;
            };

            const img = card.querySelector('img');
            const icon = card.querySelector('.episode-thumbnail-icon');

            img.addEventListener('error', () => {
                img.style.display = 'none';
                icon.style.display = 'flex';
            });

            img.addEventListener('load', () => {
                img.style.display = 'block';
                icon.style.display = 'none';
            });

            handleImageSkeleton(img);
            episodesGrid.appendChild(card);
        });
    }
}
