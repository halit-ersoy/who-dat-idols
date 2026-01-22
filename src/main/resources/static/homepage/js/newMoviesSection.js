import { handleImageSkeleton } from '../../elements/userLogged.js';

export function initNewMoviesSection() {
    const viewAllBtn = document.querySelector('.new-movies .view-all');
    const newMoviesAllSection = document.getElementById('new-movies-all');
    const newMoviesAllGrid = document.querySelector('.new-movies-all-grid');
    const loadMoreBtn = document.getElementById('load-more-new-movies');
    const closeAllBtn = document.querySelector('.new-movies-all-section .close-all-btn');
    const newMoviesCarousel = document.getElementById('new-movies-carousel');

    if (!viewAllBtn || !newMoviesAllSection || !newMoviesAllGrid || !loadMoreBtn || !closeAllBtn || !newMoviesCarousel) {
        console.error('New Movies Section: Gerekli elementler bulunamadı.');
        return;
    }

    let allMovies = [];
    let currentItemsLoaded = 0;
    const itemsPerLoad = 40;

    async function fetchMovies(limit = 10) {
        try {
            const response = await fetch(`/api/movies/recent?limit=${limit}`);
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return await response.json();
        } catch (error) {
            console.error('Movies Section - Hata:', error);
            return [];
        }
    }

    function createNewMoviesItemHTML(item, index) {
        return `
      <a href="${item.videoUrl}" class="card new-movies-item" style="animation-delay: ${index * 0.05}s">
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
        const endIndex = Math.min(startIndex + count, allMovies.length);
        for (let i = startIndex; i < endIndex; i++) {
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = createNewMoviesItemHTML(allMovies[i], i - startIndex);
            const card = tempDiv.firstElementChild;
            handleImageSkeleton(card.querySelector('img'));
            fragment.appendChild(card);
        }
        newMoviesAllGrid.appendChild(fragment);
        currentItemsLoaded = endIndex;
        loadMoreBtn.classList.toggle('hidden', currentItemsLoaded >= allMovies.length);
    }

    function populateCarousel(movies) {
        newMoviesCarousel.innerHTML = '';
        movies.slice(0, 14).forEach(movie => {
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = `
                <a href="${movie.videoUrl}" class="card">
                    <div class="card-image-container img-skeleton">
                        <img src="${movie.thumbnailUrl}" alt="${movie.title}">
                        <div class="play-icon"><i class="fas fa-play"></i></div>
                    </div>
                    <div class="card-content">
                        <p class="card-title">${movie.title}</p>
                        <p class="card-info">${movie.info}</p>
                    </div>
                </a>
            `;
            const card = tempDiv.firstElementChild;
            handleImageSkeleton(card.querySelector('img'));
            newMoviesCarousel.appendChild(card);
        });
    }

    (async () => {
        allMovies = await fetchMovies(50);
        populateCarousel(allMovies);
    })();

    viewAllBtn.addEventListener('click', async (e) => {
        e.preventDefault();
        newMoviesAllGrid.innerHTML = '';
        currentItemsLoaded = 0;
        newMoviesAllSection.classList.remove('hidden');
        newMoviesAllSection.scrollIntoView({ behavior: 'smooth' });

        if (allMovies.length > 0) {
            loadItems(0, itemsPerLoad);
        } else {
            allMovies = await fetchMovies(50);
            loadItems(0, itemsPerLoad);
        }
    });

    loadMoreBtn.addEventListener('click', () => {
        loadItems(currentItemsLoaded, itemsPerLoad);
    });

    closeAllBtn.addEventListener('click', () => {
        newMoviesAllSection.classList.add('hidden');
    });
}
