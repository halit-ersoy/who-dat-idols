import { handleImageSkeleton } from '../../elements/userLogged.js';

export function initNewSeriesSection() {
    const viewAllBtn = document.querySelector('.new-series .view-all');
    const newSeriesAllSection = document.getElementById('new-series-all');
    const newSeriesAllGrid = document.querySelector('.new-series-all-grid');
    const loadMoreBtn = document.getElementById('load-more-new-series');
    const closeAllBtn = document.querySelector('#new-series-all .close-all-btn');
    const newSeriesCarousel = document.querySelector('.new-series .carousel');

    if (!viewAllBtn || !newSeriesAllSection || !newSeriesAllGrid || !loadMoreBtn || !closeAllBtn || !newSeriesCarousel) {
        console.error('New Series Section: Gerekli elementler bulunamadı.');
        return;
    }

    let allSeries = [];
    let currentItemsLoaded = 0;
    const itemsPerLoad = 20;

    async function fetchSeries(day = 20) {
        try {
            const response = await fetch(`/api/series/recent?day=${day}`);
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return await response.json();
        } catch (error) {
            console.error('Series Section - Hata:', error);
            return [];
        }
    }

    function createSeriesItemHTML(item, index) {
        return `
      <a href="${item.videoUrl}" class="card new-series-item" style="animation-delay: ${index * 0.05}s">
        <div class="card-image-container img-skeleton">
          <img src="${item.thumbnailUrl}" alt="${item.title}">
          <div class="play-icon"><i class="fas fa-play"></i></div>
        </div>
        <div class="card-content">
          <p class="card-title">${item.title}</p>
          <p class="card-info">${item.info}</p>
        </div>
      </a>
    `;
    }

    function loadItems(startIndex, count) {
        const fragment = document.createDocumentFragment();
        const endIndex = Math.min(startIndex + count, allSeries.length);
        for (let i = startIndex; i < endIndex; i++) {
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = createSeriesItemHTML(allSeries[i], i - startIndex);
            const card = tempDiv.firstElementChild;
            handleImageSkeleton(card.querySelector('img'));
            fragment.appendChild(card);
        }
        newSeriesAllGrid.appendChild(fragment);
        currentItemsLoaded = endIndex;
        loadMoreBtn.classList.toggle('hidden', currentItemsLoaded >= allSeries.length);
    }

    function populateCarousel(seriesList) {
        newSeriesCarousel.innerHTML = '';
        seriesList.slice(0, 14).forEach(seriesItem => {
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = `
                <a href="${seriesItem.videoUrl}" class="card">
                    <div class="card-image-container img-skeleton">
                        <img src="${seriesItem.thumbnailUrl}" alt="${seriesItem.title}">
                        <div class="play-icon"><i class="fas fa-play"></i></div>
                    </div>
                    <div class="card-content">
                        <p class="card-title">${seriesItem.title}</p>
                        <p class="card-info">${seriesItem.info}</p>
                    </div>
                </a>
            `;
            const card = tempDiv.firstElementChild;
            handleImageSkeleton(card.querySelector('img'));
            newSeriesCarousel.appendChild(card);
        });
    }

    // Başlangıçta carousel dolduruluyor
    (async () => {
        allSeries = await fetchSeries();
        populateCarousel(allSeries);
    })();

    viewAllBtn.addEventListener('click', async (e) => {
        e.preventDefault();
        newSeriesAllGrid.innerHTML = '';
        currentItemsLoaded = 0;
        newSeriesAllSection.classList.remove('hidden');
        newSeriesAllSection.scrollIntoView({ behavior: 'smooth' });

        if (allSeries.length > 0) {
            loadItems(0, itemsPerLoad);
        } else {
            allSeries = await fetchSeries();
            loadItems(0, itemsPerLoad);
        }
    });

    loadMoreBtn.addEventListener('click', () => {
        loadItems(currentItemsLoaded, itemsPerLoad);
    });

    closeAllBtn.addEventListener('click', () => {
        newSeriesAllSection.classList.add('hidden');
    });
}
