// new-soap-operas "View All" functionality
export function initNewSoapOperasSection() {
    // Generate sample new-soap-operas items data (in practice, data would come from the backend)
    const newSoapOperasItems = Array.from({ length: 60 }, (_, i) => ({
        id: i + 1,
        title: `NewSoapOperas Title ${i + 1}`,
        year: 2023 - Math.floor(i / 10),
        genre: ['Romantik', 'Komedi', 'Drama', 'Aksiyon', 'Fantastik'][i % 5],
        duration: `${40 + i % 20} dk`,
        image: [
            'https://i.ibb.co/gW6K4T1/mutya-card.jpg',
            'https://i.ibb.co/ZM7SxXh/bl-1.jpg',
            'https://i.ibb.co/y5QfMxk/kdrama-1.jpg',
            'https://i.ibb.co/hRMtw0k/bl-2.jpg',
            'https://i.ibb.co/Ntf5Rkx/kdrama-2.jpg',
            'https://i.ibb.co/jzy6zDm/bl-3.jpg',
            'https://i.ibb.co/YB0BnF3/jdrama-1.jpg',
            'https://i.ibb.co/M2BgHph/cdrama-1.jpg'
        ][i % 8]
    }));

    // Select DOM elements
    const viewAllBtn = document.querySelector('.new-soap-operas .view-all');
    const newSoapOperasAllSection = document.getElementById('new-soap-operas-all');
    const newSoapOperasAllGrid = document.querySelector('.new-soap-operas-all-grid');
    const loadMoreBtn = document.getElementById('load-more-new-soap-operas');
    const closeAllBtn = document.querySelector('#new-soap-operas-all .close-all-btn');

    let currentItemsLoaded = 0;
    const itemsPerLoad = 20;

    // Creates HTML for a single new-soap-operas item card
    function createNewSoapOperasItemHTML(item, index) {
        return `
      <a href="/watch?id=${item.id}" class="card new-soap-operas-item" style="animation-delay: ${index * 0.05}s">
        <div class="card-image-container">
          <img src="${item.image}" alt="${item.title}">
          <div class="play-icon"><i class="fas fa-play"></i></div>
        </div>
        <div class="card-content">
          <p class="card-title">${item.title}</p>
          <p class="card-info">${item.year} • ${item.genre} • ${item.duration}</p>
        </div>
      </a>
    `;
    }

    // Loads a batch of new-soap-operas items starting from a given index
    function loadItems(startIndex, count) {
        const fragment = document.createDocumentFragment();
        const endIndex = Math.min(startIndex + count, newSoapOperasItems.length);

        for (let i = startIndex; i < endIndex; i++) {
            const itemHTML = createNewSoapOperasItemHTML(newSoapOperasItems[i], i - startIndex);
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = itemHTML;
            fragment.appendChild(tempDiv.firstElementChild);
        }

        newSoapOperasAllGrid.appendChild(fragment);
        currentItemsLoaded = endIndex;

        // Hide "Load More" button if all items are loaded
        if (currentItemsLoaded >= newSoapOperasItems.length) {
            loadMoreBtn.classList.add('hidden');
        }
    }

    // "View All" button click event handler
    viewAllBtn.addEventListener('click', function (e) {
        e.preventDefault();

        newSoapOperasAllGrid.innerHTML = '';
        currentItemsLoaded = 0;

        newSoapOperasAllSection.classList.remove('hidden');
        newSoapOperasAllSection.scrollIntoView({ behavior: 'smooth' });

        loadItems(0, itemsPerLoad);

        if (newSoapOperasItems.length > itemsPerLoad) {
            loadMoreBtn.classList.remove('hidden');
        } else {
            loadMoreBtn.classList.add('hidden');
        }
    });

    // "Load More" button click event handler
    loadMoreBtn.addEventListener('click', function () {
        loadItems(currentItemsLoaded, itemsPerLoad);
    });

    // "Close" button click event handler
    closeAllBtn.addEventListener('click', function () {
        newSoapOperasAllSection.classList.add('hidden');
    });
}