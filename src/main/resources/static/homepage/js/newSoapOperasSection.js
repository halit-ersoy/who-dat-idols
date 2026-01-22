import { handleImageSkeleton } from '../../elements/userLogged.js';

export function initNewSoapOperasSection() {
    const viewAllBtn = document.querySelector('.new-soap-operas .view-all');
    const newSoapOperasAllSection = document.getElementById('new-soap-operas-all');
    const newSoapOperasAllGrid = document.querySelector('.new-soap-operas-all-grid');
    const loadMoreBtn = document.getElementById('load-more-new-soap-operas');
    const closeAllBtn = document.querySelector('#new-soap-operas-all .close-all-btn');
    const newSoapOperasCarousel = document.querySelector('.new-soap-operas .carousel');

    if (!viewAllBtn || !newSoapOperasAllSection || !newSoapOperasAllGrid || !loadMoreBtn || !closeAllBtn || !newSoapOperasCarousel) {
        console.error('New Soap Operas Section: Gerekli elementler bulunamadı.');
        return;
    }

    let allSoapOperas = [];
    let currentItemsLoaded = 0;
    const itemsPerLoad = 20;

    async function fetchSoapOperas(day = 20) {
        try {
            const response = await fetch(`/api/soapoperas/recent?day=${day}`);
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
      <a href="${item.videoUrl}" class="card new-soap-operas-item" style="animation-delay: ${index * 0.05}s">
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
        const endIndex = Math.min(startIndex + count, allSoapOperas.length);
        for (let i = startIndex; i < endIndex; i++) {
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = createSoapOperaItemHTML(allSoapOperas[i], i - startIndex);
            const card = tempDiv.firstElementChild;
            handleImageSkeleton(card.querySelector('img'));
            fragment.appendChild(card);
        }
        newSoapOperasAllGrid.appendChild(fragment);
        currentItemsLoaded = endIndex;
        loadMoreBtn.classList.toggle('hidden', currentItemsLoaded >= allSoapOperas.length);
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

    // Başlangıçta carousel dolduruluyor
    (async () => {
        allSoapOperas = await fetchSoapOperas();
        populateCarousel(allSoapOperas);
    })();

    viewAllBtn.addEventListener('click', async (e) => {
        e.preventDefault();
        newSoapOperasAllGrid.innerHTML = '';
        currentItemsLoaded = 0;
        newSoapOperasAllSection.classList.remove('hidden');
        newSoapOperasAllSection.scrollIntoView({ behavior: 'smooth' });

        if (allSoapOperas.length > 0) {
            loadItems(0, itemsPerLoad);
        } else {
            allSoapOperas = await fetchSoapOperas();
            loadItems(0, itemsPerLoad);
        }
    });

    loadMoreBtn.addEventListener('click', () => {
        loadItems(currentItemsLoaded, itemsPerLoad);
    });

    closeAllBtn.addEventListener('click', () => {
        newSoapOperasAllSection.classList.add('hidden');
    });
}
