export function initNewMoviesSection() {
    const viewAllBtn = document.querySelector('.new-movies .view-all');
    const newMoviesAllSection = document.getElementById('new-movies-all');
    const newMoviesAllGrid = document.querySelector('.new-movies-all-grid');
    const loadMoreBtn = document.getElementById('load-more-new-movies');
    const closeAllBtn = document.querySelector('.new-movies-all-section .close-all-btn');
    const newMoviesCarousel = document.getElementById('new-movies-carousel');

    let allMovies = [];
    let currentItemsLoaded = 0;
    const itemsPerLoad = 40;

    // Fetch movie data from the backend
    function fetchMovies(limit = 10) {
        return fetch(`/api/movies/recent?limit=${limit}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            });
    }

    function createNewMoviesItemHTML(item, index) {
        return `
      <a href="${item.videoUrl}" class="card new-movies-item" style="animation-delay: ${index * 0.05}s">
        <div class="card-image-container">
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
            const itemHTML = createNewMoviesItemHTML(allMovies[i], i - startIndex);
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = itemHTML;
            fragment.appendChild(tempDiv.firstElementChild);
        }
        newMoviesAllGrid.appendChild(fragment);
        currentItemsLoaded = endIndex;
        if (currentItemsLoaded >= allMovies.length) {
            loadMoreBtn.classList.add('hidden');
        }
    }

    function populateCarousel(movies) {
        // Clear existing content
        newMoviesCarousel.innerHTML = '';

        // Add first 14 movies to carousel
        movies.slice(0, 14).forEach(movie => {
            const itemHTML = `
                <a href="${movie.videoUrl}" class="card">
                    <div class="card-image-container">
                        <img src="${movie.thumbnailUrl}" alt="${movie.title}">
                        <div class="play-icon"><i class="fas fa-play"></i></div>
                    </div>
                    <div class="card-content">
                        <p class="card-title">${movie.title}</p>
                        <p class="card-info">${movie.info}</p>
                    </div>
                </a>
            `;
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = itemHTML;
            newMoviesCarousel.appendChild(tempDiv.firstElementChild);
        });
    }

    // Initialize the section
    fetchMovies(50).then(movies => {
        allMovies = movies;
        populateCarousel(movies);
    }).catch(error => {
        console.error('Error fetching movies:', error);
    });

    viewAllBtn.addEventListener('click', function(e) {
        e.preventDefault();
        newMoviesAllGrid.innerHTML = '';
        currentItemsLoaded = 0;
        newMoviesAllSection.classList.remove('hidden');
        newMoviesAllSection.scrollIntoView({ behavior: 'smooth' });

        // If we already have movies, load them immediately
        if (allMovies.length > 0) {
            loadItems(0, itemsPerLoad);
            if (allMovies.length > itemsPerLoad) {
                loadMoreBtn.classList.remove('hidden');
            } else {
                loadMoreBtn.classList.add('hidden');
            }
        } else {
            // Otherwise fetch them first
            fetchMovies(50).then(movies => {
                allMovies = movies;
                loadItems(0, itemsPerLoad);
                if (allMovies.length > itemsPerLoad) {
                    loadMoreBtn.classList.remove('hidden');
                } else {
                    loadMoreBtn.classList.add('hidden');
                }
            }).catch(error => {
                console.error('Error fetching movies:', error);
            });
        }
    });

    loadMoreBtn.addEventListener('click', function() {
        loadItems(currentItemsLoaded, itemsPerLoad);
    });

    closeAllBtn.addEventListener('click', function() {
        newMoviesAllSection.classList.add('hidden');
    });
}