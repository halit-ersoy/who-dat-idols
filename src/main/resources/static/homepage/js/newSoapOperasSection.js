export function initNewSoapOperasSection() {
    const viewAllBtn = document.querySelector('.new-soap-operas .view-all');
    const newSoapOperasAllSection = document.getElementById('new-soap-operas-all');
    const newSoapOperasAllGrid = document.querySelector('.new-soap-operas-all-grid');
    const loadMoreBtn = document.getElementById('load-more-new-soap-operas');
    const closeAllBtn = document.querySelector('#new-soap-operas-all .close-all-btn');
    const newSoapOperasCarousel = document.querySelector('.new-soap-operas .carousel');

    let allSoapOperas = [];
    let currentItemsLoaded = 0;
    const itemsPerLoad = 20;

    // Fetch soap opera data from the backend
    function fetchSoapOperas(day = 20) {
        return fetch(`/api/soapoperas/recent?day=${day}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            });
    }

    function createSoapOperaItemHTML(item, index) {
        return `
      <a href="${item.videoUrl}" class="card new-soap-operas-item" style="animation-delay: ${index * 0.05}s">
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
        const endIndex = Math.min(startIndex + count, allSoapOperas.length);

        for (let i = startIndex; i < endIndex; i++) {
            const itemHTML = createSoapOperaItemHTML(allSoapOperas[i], i - startIndex);
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = itemHTML;
            fragment.appendChild(tempDiv.firstElementChild);
        }

        newSoapOperasAllGrid.appendChild(fragment);
        currentItemsLoaded = endIndex;

        if (currentItemsLoaded >= allSoapOperas.length) {
            loadMoreBtn.classList.add('hidden');
        }
    }

    function populateCarousel(soapOperas) {
        // Clear existing content (except for buttons)
        const existingItems = newSoapOperasCarousel.querySelectorAll('.card');
        existingItems.forEach(item => item.remove());

        // Add first 14 soap operas to carousel
        soapOperas.slice(0, 14).forEach(soapOpera => {
            const itemHTML = `
                <a href="${soapOpera.videoUrl}" class="card">
                    <div class="card-image-container">
                        <img src="${soapOpera.thumbnailUrl}" alt="${soapOpera.title}">
                        <div class="play-icon"><i class="fas fa-play"></i></div>
                    </div>
                    <div class="card-content">
                        <p class="card-title">${soapOpera.title}</p>
                        <p class="card-info">${soapOpera.info}</p>
                    </div>
                </a>
            `;
            newSoapOperasCarousel.innerHTML += itemHTML;
        });
    }

    // Initialize the section
    fetchSoapOperas().then(soapOperas => {
        allSoapOperas = soapOperas;
        populateCarousel(soapOperas);
    }).catch(error => {
        console.error('Error fetching soap operas:', error);
    });

    // "View All" button click event handler
    viewAllBtn.addEventListener('click', function(e) {
        e.preventDefault();

        newSoapOperasAllGrid.innerHTML = '';
        currentItemsLoaded = 0;

        newSoapOperasAllSection.classList.remove('hidden');
        newSoapOperasAllSection.scrollIntoView({ behavior: 'smooth' });

        // If we already have data, load it immediately
        if (allSoapOperas.length > 0) {
            loadItems(0, itemsPerLoad);
            if (allSoapOperas.length > itemsPerLoad) {
                loadMoreBtn.classList.remove('hidden');
            } else {
                loadMoreBtn.classList.add('hidden');
            }
        } else {
            // Otherwise fetch first
            fetchSoapOperas().then(soapOperas => {
                allSoapOperas = soapOperas;
                loadItems(0, itemsPerLoad);
                if (allSoapOperas.length > itemsPerLoad) {
                    loadMoreBtn.classList.remove('hidden');
                } else {
                    loadMoreBtn.classList.add('hidden');
                }
            }).catch(error => {
                console.error('Error fetching soap operas:', error);
            });
        }
    });

    // "Load More" button click event handler
    loadMoreBtn.addEventListener('click', function() {
        loadItems(currentItemsLoaded, itemsPerLoad);
    });

    // "Close" button click event handler
    closeAllBtn.addEventListener('click', function() {
        newSoapOperasAllSection.classList.add('hidden');
    });
}