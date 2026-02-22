export function initSimilarContent(videoId) {
    const container = document.getElementById('recommendationCards');
    const prevBtn = document.getElementById('recPrevBtn');
    const nextBtn = document.getElementById('recNextBtn');

    if (!container) return;

    fetch(`/api/video/similar?id=${videoId}`)
        .then(response => response.json())
        .then(data => {
            renderRecommendations(data, container);
            setupCarouselNavigation(container, prevBtn, nextBtn);
        })
        .catch(error => {
            console.error('Error fetching similar content:', error);
            container.innerHTML = '<p class="error-text">Benzer içerikler yüklenemedi.</p>';
        });
}

function setupCarouselNavigation(container, prevBtn, nextBtn) {
    if (!prevBtn || !nextBtn) return;

    const updateArrows = () => {
        if (container.scrollLeft <= 0) {
            prevBtn.classList.add('disabled');
        } else {
            prevBtn.classList.remove('disabled');
        }

        if (container.scrollLeft + container.clientWidth >= container.scrollWidth - 10) {
            nextBtn.classList.add('disabled');
        } else {
            nextBtn.classList.remove('disabled');
        }
    };

    prevBtn.addEventListener('click', () => {
        const scrollAmount = container.clientWidth * 0.8;
        container.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
    });

    nextBtn.addEventListener('click', () => {
        const scrollAmount = container.clientWidth * 0.8;
        container.scrollBy({ left: scrollAmount, behavior: 'smooth' });
    });

    container.addEventListener('scroll', updateArrows);
    window.addEventListener('resize', updateArrows);

    // Initial check
    updateArrows();
}

function renderRecommendations(items, container) {
    if (!items || items.length === 0) {
        container.innerHTML = '<p class="empty-text">Henüz benzer bir içerik bulunmuyor.</p>';
        return;
    }

    container.innerHTML = '';
    items.forEach(item => {
        const card = document.createElement('div');
        card.className = 'rec-card';
        card.innerHTML = `
            <a href="/${item.slug || item.ID}" class="rec-link">
                <div class="rec-image img-skeleton">
                    <img src="/media/image/${item.ID}" alt="${item.Name}" onerror="this.src='https://picsum.photos/300/170?blur=10'">
                </div>
                <div class="rec-title" title="${item.Name}">${item.Name}</div>
                <div class="rec-meta">${item.Category ? item.Category.split(',')[0] : 'Detaylar'}</div>
            </a>
        `;
        container.appendChild(card);

        const img = card.querySelector('img');
        img.onload = () => card.querySelector('.rec-image').classList.remove('img-skeleton');
    });
}
