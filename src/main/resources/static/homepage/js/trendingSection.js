// Trending "View All" functionality
export function initTrendingSection() {
    // Sample trending items data (in practice, this would come from your backend)
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

    const viewAllBtn = document.querySelector('.trending .view-all');
    const trendingAllSection = document.getElementById('trending-all');
    const trendingAllGrid = document.querySelector('.trending-all-grid');
    const loadMoreBtn = document.getElementById('load-more-trending');
    const closeAllBtn = document.querySelector('.close-all-btn');

    let currentItemsLoaded = 0;
    const itemsPerLoad = 20;

    // Create single trending item HTML
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

    // Load items in batches
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

        // Hide load more button if all items loaded
        if (currentItemsLoaded >= trendingItems.length) {
            loadMoreBtn.classList.add('hidden');
        }
    }

    // View All button click event
    viewAllBtn.addEventListener('click', function(e) {
        e.preventDefault();

        // Reset grid and counter
        trendingAllGrid.innerHTML = '';
        currentItemsLoaded = 0;

        // Show the section
        trendingAllSection.classList.remove('hidden');

        // Scroll to the section
        trendingAllSection.scrollIntoView({ behavior: 'smooth' });

        // Load initial items
        loadItems(0, itemsPerLoad);

        // Show load more button if needed
        if (trendingItems.length > itemsPerLoad) {
            loadMoreBtn.classList.remove('hidden');
        } else {
            loadMoreBtn.classList.add('hidden');
        }
    });

    // Load More button click event
    loadMoreBtn.addEventListener('click', function() {
        loadItems(currentItemsLoaded, itemsPerLoad);
    });

    // Close button click event
    closeAllBtn.addEventListener('click', function() {
        trendingAllSection.classList.add('hidden');
    });
}