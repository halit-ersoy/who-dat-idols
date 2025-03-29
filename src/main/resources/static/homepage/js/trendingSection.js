// Trending "View All" functionality
export function initTrendingSection() {
    // Generate sample trending items data (in practice, data would come from the backend)
    const trendingItems = Array.from({ length: 60 }, (_, i) => ({
        id: i + 1,
        title: `Trending Title ${i + 1}`,
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
    const viewAllBtn = document.querySelector('.trending .view-all');
    const trendingAllSection = document.getElementById('trending-all');
    const trendingAllGrid = document.querySelector('.trending-all-grid');
    const loadMoreBtn = document.getElementById('load-more-trending');
    const closeAllBtn = document.querySelector('.close-all-btn');

    let currentItemsLoaded = 0;
    const itemsPerLoad = 20;

    // Creates HTML for a single trending item card
    function createTrendingItemHTML(item, index) {
        return `
      <a href="/watch?id=${item.id}" class="card trending-item" style="animation-delay: ${index * 0.05}s">
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

    // Loads a batch of trending items starting from a given index
    function loadItems(startIndex, count) {
        const fragment = document.createDocumentFragment();
        const endIndex = Math.min(startIndex + count, trendingItems.length);

        for (let i = startIndex; i < endIndex; i++) {
            const itemHTML = createTrendingItemHTML(trendingItems[i], i - startIndex);
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = itemHTML;
            fragment.appendChild(tempDiv.firstElementChild);
        }

        trendingAllGrid.appendChild(fragment);
        currentItemsLoaded = endIndex;

        // Hide "Load More" button if all items are loaded
        if (currentItemsLoaded >= trendingItems.length) {
            loadMoreBtn.classList.add('hidden');
        }
    }

    // "View All" button click event handler
    viewAllBtn.addEventListener('click', function (e) {
        e.preventDefault();

        trendingAllGrid.innerHTML = '';
        currentItemsLoaded = 0;

        trendingAllSection.classList.remove('hidden');
        trendingAllSection.scrollIntoView({ behavior: 'smooth' });

        loadItems(0, itemsPerLoad);

        if (trendingItems.length > itemsPerLoad) {
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
        trendingAllSection.classList.add('hidden');
    });
}
