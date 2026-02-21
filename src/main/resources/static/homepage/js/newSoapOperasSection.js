import { handleImageSkeleton } from '../../elements/userLogged.js';

export function initNewSoapOperasSection() {
    const viewAllBtn = document.querySelector('.new-soap-operas .view-all');
    const newSoapOperasAllSection = document.getElementById('new-soap-operas-all');
    const newSoapOperasAllGrid = document.querySelector('.new-soap-operas-all-grid');
    const closeAllBtn = document.querySelector('#new-soap-operas-all .close-all-btn');
    const newSoapOperasCarousel = document.querySelector('.new-soap-operas .carousel');

    if (!viewAllBtn || !newSoapOperasAllSection || !newSoapOperasAllGrid || !closeAllBtn || !newSoapOperasCarousel) {
        console.error('New Soap Operas Section: Gerekli elementler bulunamadı.');
        return;
    }

    let allSoapOperas = [];
    let currentItemsLoaded = 0;
    const itemsPerLoad = 20;

    async function fetchSoapOperas(day = 20) {
        try {
            const response = await fetch(`/api/series/recent?day=${day}`);
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return await response.json();
        } catch (error) {
            console.error('Soap Operas Section - Hata:', error);
            return [];
        }
    }

    function createSoapOperaItemHTML(item, index) {
        return `
      <a href="${item.videoUrl}" class="card new-soap-operas-item" style="animation-delay: ${index * 0.02}s">
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

    let filteredSoapOperas = [];
    let isSearchActive = false;
    let currentPage = 1;
    const itemsPerPage = 18;

    function renderPage(page) {
        const dataToUse = isSearchActive ? filteredSoapOperas : allSoapOperas;
        const totalItems = dataToUse.length;
        const totalPages = Math.ceil(totalItems / itemsPerPage);

        // Ensure page is within bounds
        if (page < 1) page = 1;
        if (page > totalPages && totalPages > 0) page = totalPages;

        currentPage = page;

        const startIndex = (currentPage - 1) * itemsPerPage;
        const endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        newSoapOperasAllGrid.innerHTML = '';
        const fragment = document.createDocumentFragment();

        for (let i = startIndex; i < endIndex; i++) {
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = createSoapOperaItemHTML(dataToUse[i], i);
            const card = tempDiv.firstElementChild;
            handleImageSkeleton(card.querySelector('img'));
            fragment.appendChild(card);
        }
        newSoapOperasAllGrid.appendChild(fragment);

        setupPagination(totalPages);

        // Scroll to top of section when changing pages
        if (newSoapOperasAllSection.classList.contains('hidden') === false) {
            const headerOffset = 100;
            const elementPosition = newSoapOperasAllSection.getBoundingClientRect().top;
            const offsetPosition = elementPosition + window.pageYOffset - headerOffset;
            window.scrollTo({
                top: offsetPosition,
                behavior: "smooth"
            });
        }
    }

    function setupPagination(totalPages) {
        // Remove existing pagination if any
        const existingPag = document.querySelector('.new-soap-operas-pagination');
        if (existingPag) existingPag.remove();

        if (totalPages <= 1) return;

        const paginationContainer = document.createElement('div');
        paginationContainer.className = 'new-soap-operas-pagination pagination-container';

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

        // Logic for smart pagination
        // Always show first, last, current, and neighbors
        const range = 2; // how many pages before/after current
        let pagesToShow = [];

        // Always add 1
        pagesToShow.push(1);

        for (let i = currentPage - range; i <= currentPage + range; i++) {
            if (i > 1 && i < totalPages) {
                pagesToShow.push(i);
            }
        }

        // Always add last page
        if (totalPages > 1) {
            pagesToShow.push(totalPages);
        }

        // Sort and remove duplicates just in case
        pagesToShow = [...new Set(pagesToShow)].sort((a, b) => a - b);

        // Build UI with ellipses
        let lastPageAdded = 0;
        for (const page of pagesToShow) {
            if (lastPageAdded > 0 && page - lastPageAdded > 1) {
                paginationContainer.appendChild(createEllipsis());
            }
            paginationContainer.appendChild(createButton(page, page === currentPage));
            lastPageAdded = page;
        }

        newSoapOperasAllSection.appendChild(paginationContainer);
    }

    const searchInput = document.getElementById('search-soap-operas-all');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            const term = e.target.value.toLowerCase().trim();
            if (term === '') {
                isSearchActive = false;
                filteredSoapOperas = [];
            } else {
                isSearchActive = true;
                filteredSoapOperas = allSoapOperas.filter(item =>
                    item.title.toLowerCase().includes(term) ||
                    (item.info && item.info.toLowerCase().includes(term))
                );
            }
            currentPage = 1;
            renderPage(currentPage);
        });
    }

    function populateCarousel(soapOperas) {
        newSoapOperasCarousel.innerHTML = '';
        soapOperas.slice(0, 14).forEach(soapOpera => {
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = `
                <a href="${soapOpera.videoUrl}" class="card">
                    <div class="card-image-container img-skeleton">
                        <img src="${soapOpera.thumbnailUrl}" alt="${soapOpera.title}">
                        <div class="play-icon"><i class="fas fa-play"></i></div>
                    </div>
                    <div class="card-content">
                        <p class="card-title">${soapOpera.title}</p>
                        <p class="card-info">${soapOpera.info}</p>
                    </div>
                </a>
            `;
            const card = tempDiv.firstElementChild;
            handleImageSkeleton(card.querySelector('img'));
            newSoapOperasCarousel.appendChild(card);
        });
    }

    (async () => {
        allSoapOperas = await fetchSoapOperas(0);
        populateCarousel(allSoapOperas);
    })();

    viewAllBtn.addEventListener('click', async (e) => {
        e.preventDefault();
        newSoapOperasAllGrid.innerHTML = '';
        currentPage = 1;
        newSoapOperasAllSection.classList.remove('hidden');
        newSoapOperasAllSection.scrollIntoView({ behavior: 'smooth' });

        if (allSoapOperas.length > 0) {
            renderPage(currentPage);
        } else {
            allSoapOperas = await fetchSoapOperas(0);
            renderPage(currentPage);
        }
    });

    closeAllBtn.addEventListener('click', () => {
        newSoapOperasAllSection.classList.add('hidden');
        const existingPag = document.querySelector('.new-soap-operas-pagination');
        if (existingPag) existingPag.remove();
    });
}
