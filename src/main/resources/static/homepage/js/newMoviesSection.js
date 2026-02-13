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
      <a href="${item.videoUrl}" class="card new-movies-item" style="animation-delay: ${index * 0.02}s">
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

    let filteredMovies = [];
    let isSearchActive = false;
    let currentPage = 1;
    const itemsPerPage = 18;

    function renderPage(page) {
        const dataToUse = isSearchActive ? filteredMovies : allMovies;
        const totalItems = dataToUse.length;
        const totalPages = Math.ceil(totalItems / itemsPerPage);

        // Ensure page is within bounds
        if (page < 1) page = 1;
        if (page > totalPages && totalPages > 0) page = totalPages;

        currentPage = page;

        const startIndex = (currentPage - 1) * itemsPerPage;
        const endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        newMoviesAllGrid.innerHTML = '';
        const fragment = document.createDocumentFragment();

        for (let i = startIndex; i < endIndex; i++) {
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = createNewMoviesItemHTML(dataToUse[i], i);
            const card = tempDiv.firstElementChild;
            handleImageSkeleton(card.querySelector('img'));
            fragment.appendChild(card);
        }
        newMoviesAllGrid.appendChild(fragment);

        setupPagination(totalPages);

        // Scroll to top of section when changing pages
        if (newMoviesAllSection.classList.contains('hidden') === false) {
            const headerOffset = 100;
            const elementPosition = newMoviesAllSection.getBoundingClientRect().top;
            const offsetPosition = elementPosition + window.pageYOffset - headerOffset;
            window.scrollTo({
                top: offsetPosition,
                behavior: "smooth"
            });
        }
    }

    function setupPagination(totalPages) {
        // Remove existing pagination if any
        const existingPag = document.querySelector('.new-movies-pagination');
        if (existingPag) existingPag.remove();

        if (totalPages <= 1) return;

        const paginationContainer = document.createElement('div');
        paginationContainer.className = 'new-movies-pagination pagination-container';

        const createButton = (page, isActive = false) => {
            const btn = document.createElement('button');
            btn.className = `pagination-btn ${isActive ? 'active' : ''}`;
            btn.innerText = page;
            btn.addEventListener('click', () => {
                if (page !== currentPage) renderPage(page);
            });
            return btn;
        };

        const createEllipsis = () => {
            const span = document.createElement('span');
            span.className = 'pagination-ellipsis';
            span.innerText = '...';
            span.style.color = '#fff';
            span.style.padding = '0 5px';
            return span;
        };

        const range = 2;
        let pagesToShow = [];

        pagesToShow.push(1);

        for (let i = currentPage - range; i <= currentPage + range; i++) {
            if (i > 1 && i < totalPages) {
                pagesToShow.push(i);
            }
        }

        if (totalPages > 1) {
            pagesToShow.push(totalPages);
        }

        pagesToShow = [...new Set(pagesToShow)].sort((a, b) => a - b);

        let lastPageAdded = 0;
        for (const page of pagesToShow) {
            if (lastPageAdded > 0 && page - lastPageAdded > 1) {
                paginationContainer.appendChild(createEllipsis());
            }
            paginationContainer.appendChild(createButton(page, page === currentPage));
            lastPageAdded = page;
        }

        newMoviesAllSection.appendChild(paginationContainer);
    }

    const searchInput = document.getElementById('search-movies-all');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            const term = e.target.value.toLowerCase().trim();
            if (term === '') {
                isSearchActive = false;
                filteredMovies = [];
            } else {
                isSearchActive = true;
                filteredMovies = allMovies.filter(item =>
                    item.title.toLowerCase().includes(term) ||
                    (item.info && item.info.toLowerCase().includes(term))
                );
            }
            currentPage = 1;
            renderPage(currentPage);
        });
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
        currentPage = 1;
        newMoviesAllSection.classList.remove('hidden');
        newMoviesAllSection.scrollIntoView({ behavior: 'smooth' });

        if (allMovies.length > 0) {
            renderPage(currentPage);
        } else {
            allMovies = await fetchMovies(50);
            renderPage(currentPage);
        }
    });

    closeAllBtn.addEventListener('click', () => {
        newMoviesAllSection.classList.add('hidden');
        const existingPag = document.querySelector('.new-movies-pagination');
        if (existingPag) existingPag.remove();
    });
}
