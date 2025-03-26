// New Content "View All" functionality
export function initNewContentSection() {
    // Sample new content items data (in practice, this would come from your backend)
    const newContentItems = Array.from({ length: 60 }, (_, i) => ({
        id: i + 1,
        title: `New Content ${i + 1}`,
        year: 2024 - Math.floor(i / 15),
        genre: ['Romantik', 'Komedi', 'Drama', 'Aksiyon', 'Fantastik'][i % 5],
        duration: `${40 + i % 20} dk`,
        image: [
            'https://i.ibb.co/Ntf5Rkx/kdrama-2.jpg',
            'https://i.ibb.co/jzy6zDm/bl-3.jpg',
            'https://i.ibb.co/YB0BnF3/jdrama-1.jpg',
            'https://i.ibb.co/M2BgHph/cdrama-1.jpg',
            'https://i.ibb.co/gW6K4T1/mutya-card.jpg',
            'https://i.ibb.co/ZM7SxXh/bl-1.jpg',
            'https://i.ibb.co/y5QfMxk/kdrama-1.jpg',
            'https://i.ibb.co/hRMtw0k/bl-2.jpg'
        ][i % 8]
    }));

    const viewAllBtn = document.querySelector('.new-content .view-all');
    const newContentAllSection = document.getElementById('new-content-all');
    const newContentAllGrid = document.querySelector('.new-content-all-grid');
    const loadMoreBtn = document.getElementById('load-more-new-content');
    const closeAllBtn = document.querySelector('.new-content-all-section .close-all-btn');

    let currentItemsLoaded = 0;
    const itemsPerLoad = 20;

    // Create single new content item HTML
    function createNewContentItemHTML(item, index) {
        return `
            <a href="/watch?id=${item.id}" class="card new-content-item" style="animation-delay: ${index * 0.05}s">
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
        const endIndex = Math.min(startIndex + count, newContentItems.length);

        for (let i = startIndex; i < endIndex; i++) {
            const itemHTML = createNewContentItemHTML(newContentItems[i], i - startIndex);
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = itemHTML;
            fragment.appendChild(tempDiv.firstElementChild);
        }

        newContentAllGrid.appendChild(fragment);
        currentItemsLoaded = endIndex;

        // Hide load more button if all items loaded
        if (currentItemsLoaded >= newContentItems.length) {
            loadMoreBtn.classList.add('hidden');
        }
    }

    // View All button click event
    viewAllBtn.addEventListener('click', function(e) {
        e.preventDefault();

        // Reset grid and counter
        newContentAllGrid.innerHTML = '';
        currentItemsLoaded = 0;

        // Show the section
        newContentAllSection.classList.remove('hidden');

        // Scroll to the section
        newContentAllSection.scrollIntoView({ behavior: 'smooth' });

        // Load initial items
        loadItems(0, itemsPerLoad);

        // Show load more button if needed
        if (newContentItems.length > itemsPerLoad) {
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
        newContentAllSection.classList.add('hidden');
    });
}