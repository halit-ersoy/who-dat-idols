export function initNewContentSection() {
    const viewAllBtn = document.querySelector('.new-content .view-all');
    const newContentAllSection = document.getElementById('new-content-all');
    const newContentAllGrid = document.querySelector('.new-content-all-grid');
    const loadMoreBtn = document.getElementById('load-more-new-content');
    const closeAllBtn = document.querySelector('.new-content-all-section .close-all-btn');
    const newCarousel = document.getElementById('new-carousel');

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

    function createNewContentItemHTML(item, index) {
        return `
      <a href="${item.videoUrl}" class="card new-content-item" style="animation-delay: ${index * 0.05}s">
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
            const itemHTML = createNewContentItemHTML(allMovies[i], i - startIndex);
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = itemHTML;
            fragment.appendChild(tempDiv.firstElementChild);
        }
        newContentAllGrid.appendChild(fragment);
        currentItemsLoaded = endIndex;
        if (currentItemsLoaded >= allMovies.length) {
            loadMoreBtn.classList.add('hidden');
        }
    }

    function populateCarousel(movies) {
        // Clear existing content
        newCarousel.innerHTML = '';

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
            newCarousel.appendChild(tempDiv.firstElementChild);
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
        newContentAllGrid.innerHTML = '';
        currentItemsLoaded = 0;
        newContentAllSection.classList.remove('hidden');
        newContentAllSection.scrollIntoView({ behavior: 'smooth' });

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
        newContentAllSection.classList.add('hidden');
    });
}