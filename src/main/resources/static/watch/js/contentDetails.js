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

        // SEO: Update Meta Description and Canonical
        if (data.plot) {
            const metaDescription = document.querySelector('meta[name="description"]');
            if (metaDescription) metaDescription.setAttribute('content', data.plot.substring(0, 160));
        }

        const canonicalLink = document.getElementById('canonicalLink');
        if (canonicalLink && data.slug) {
            canonicalLink.setAttribute('href', `https://whodatidols.com/watch/${data.slug}`);
        }

        // SEO: Structured Data (JSON-LD)
        const structuredDataEl = document.getElementById('structuredData');
        if (structuredDataEl) {
            // Sanitize description: strip control characters and trim to 500 chars for JSON-LD safety
            const sanitizeText = (text) => {
                if (!text) return undefined;
                return text
                    .replace(/[\u0000-\u001F\u007F]/g, ' ')  // strip control chars (newlines, tabs, etc.)
                    .replace(/\s+/g, ' ')                      // collapse whitespace
                    .trim()
                    .substring(0, 500);
            };

            // Convert "120 dk" → "PT120M" (ISO 8601 duration)
            const formatDuration = (dur) => {
                if (!dur) return undefined;
                const match = dur.match(/(\d+)/);
                return match ? `PT${match[1]}M` : undefined;
            };

            const baseUrl = 'https://whodatidols.com';
            const slug = data.slug || id;
            const contentUrl = `${baseUrl}/${slug}`;

            // Main content schema (Movie or Episode)
            const contentSchema = {
                "@context": "https://schema.org",
                "@type": data.type === 'movie' ? "Movie" : "Episode",
                "name": data.title,
                "url": contentUrl,
                "description": sanitizeText(data.plot),
                "image": `${baseUrl}/media/image/${id}`,
                "datePublished": data.year ? String(data.year) : undefined,
                "duration": formatDuration(data.duration)
            };

            if (data.type === 'episode' && data.season) {
                contentSchema.partOfSeason = {
                    "@type": "CreativeWorkSeason",
                    "seasonNumber": data.season
                };
            }

            // BreadcrumbList schema — 2 levels: Ana Sayfa → İçerik
            // All content lives at /{slug}, no category pages exist
            const breadcrumbItems = [
                { "@type": "ListItem", "position": 1, "name": "Ana Sayfa", "item": baseUrl },
                { "@type": "ListItem", "position": 2, "name": data.title, "item": contentUrl }
            ];

            const breadcrumbSchema = {
                "@context": "https://schema.org",
                "@type": "BreadcrumbList",
                "itemListElement": breadcrumbItems
            };

            // Emit both schemas as a JSON-LD array
            structuredDataEl.textContent = JSON.stringify([contentSchema, breadcrumbSchema]);
        }

        const posterImg = document.getElementById('contentPoster');
        // Use the generic image endpoint we know works
        posterImg.src = `/media/image/${id}`;
        handleImageSkeleton(posterImg);

        // Rating - placeholder for now as logic isn't in details endpoint yet
        // document.getElementById('contentRating').textContent = data.rating || "N/A";

        document.getElementById('releaseYear').textContent = data.year;
        document.getElementById('contentDuration').textContent = data.duration;
        document.getElementById('contentLanguage').textContent = data.language;

        const countryName = getCountryName(data.country);
        const countryEl = document.getElementById('contentCountry');
        if (countryEl) {
            if (countryName) {
                countryEl.textContent = countryName;
                countryEl.style.display = 'inline';
                // Previous separator
                if (countryEl.previousElementSibling && countryEl.previousElementSibling.classList.contains('separator')) {
                    countryEl.previousElementSibling.style.display = 'inline';
                }
            } else {
                countryEl.style.display = 'none';
                // Hide separator if no country
                if (countryEl.previousElementSibling && countryEl.previousElementSibling.classList.contains('separator')) {
                    countryEl.previousElementSibling.style.display = 'none';
                }
            }
        }

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

        // Broadcast seriesId for other modules (like listModal)
        if (data.seriesId) {
            document.body.dataset.seriesId = data.seriesId;
            const event = new CustomEvent('contentDetailsLoaded', { detail: { seriesId: data.seriesId } });
            document.dispatchEvent(event);
        }

    } catch (error) {
        console.error('Content details loading error:', error);
        document.getElementById('contentTitle').innerText = "Detaylar yüklenemedi";
    }
}

function getCountryName(code) {
    if (!code) return null;
    const countries = {
        'kr': 'Güney Kore',
        'jp': 'Japonya',
        'th': 'Tayland',
        'cn': 'Çin',
        'tw': 'Tayvan',
        'ph': 'Filipinler',
        'id': 'Endonezya',
        'my': 'Malezya',
        'sg': 'Singapur'
    };
    return countries[code.toLowerCase()] || code;
}
