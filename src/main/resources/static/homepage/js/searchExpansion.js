export function initSearchExpansion() {
    const searchInput = document.querySelector('.search input');
    const searchContainer = document.querySelector('.search');
    const searchButton = document.querySelector('.search-button');

    // Expand on input focus
    searchInput.addEventListener('focus', () => {
        searchContainer.classList.add('expanded');
    });

    // Collapse on blur but only if not clicking search button
    searchInput.addEventListener('blur', (e) => {
        if (e.relatedTarget !== searchButton) {
            setTimeout(() => {
                searchContainer.classList.remove('expanded');
            }, 200);
        }
    });

    // For mobile: expand on button click
    if (searchButton) {
        searchButton.addEventListener('click', (e) => {
            if (window.innerWidth <= 576 && !searchContainer.classList.contains('expanded')) {
                e.preventDefault();
                searchContainer.classList.add('expanded');
                searchInput.focus();
            }
        });
    }
}

// Add this to searchExpansion.js or create a new file

export function initHeaderInteractions() {
    // Mobile menu toggle
    const mobileMenuToggle = document.getElementById('mobile-menu-toggle');
    const navMenu = document.querySelector('.nav-menu');

    if (mobileMenuToggle) {
        mobileMenuToggle.addEventListener('click', () => {
            navMenu.classList.toggle('active');
        });
    }

    // Mobile dropdown toggles
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        const link = item.querySelector('.nav-link');
        if (link) {
            link.addEventListener('click', (e) => {
                if (window.innerWidth <= 992) {
                    e.preventDefault();
                    item.classList.toggle('active');
                }
            });
        }
    });

    // Search results display
    const searchInput = document.getElementById('search-input');
    const searchResults = document.getElementById('search-results');
    let searchTimeout;

    if (searchInput && searchResults) {
        searchInput.addEventListener('input', () => {
            const query = searchInput.value.trim();

            clearTimeout(searchTimeout);

            if (query.length < 2) {
                searchResults.innerHTML = '';
                searchResults.classList.remove('active');
                return;
            }

            searchTimeout = setTimeout(async () => {
                try {
                    const response = await fetch(`/api/search?q=${encodeURIComponent(query)}`);
                    const data = await response.json();

                    displaySearchResults(data, searchResults);
                } catch (error) {
                    console.error('Search error:', error);
                }
            }, 300);
        });

        searchInput.addEventListener('focus', () => {
            if (searchInput.value.trim().length >= 2) {
                searchResults.classList.add('active');
            }
        });

        searchInput.addEventListener('blur', (e) => {
            if (!searchResults.contains(e.relatedTarget)) {
                setTimeout(() => {
                    searchResults.classList.remove('active');
                }, 200);
            }
        });
    }

    function displaySearchResults(results, container) {
        console.log('Displaying search results:', results);
        container.innerHTML = '';

        if (!Array.isArray(results) || results.length === 0) {
            container.innerHTML = '<div class="no-results">Sonuç bulunamadı</div>';
            container.classList.add('active');
            return;
        }

        results.slice(0, 10).forEach(item => {
            const resultItem = document.createElement('div');
            resultItem.className = 'search-result-item';

            // Standardize keys (handling potential casing issues from backend)
            const id = item.ID || item.id || item.Id;
            const name = item.Name || item.name || item.NAME || 'İsimsiz';
            const type = item.Type || item.type || item.TYPE || '';
            const category = item.Category || item.category || item.CATEGORY || '';
            const year = item.Year || item.year || item.YEAR;

            const typeLabel = type === 'Movie' ? 'Film' : (type === 'SoapOpera' ? 'Dizi' : type);
            const yearInfo = year ? `(${year})` : '';

            resultItem.innerHTML = `
                <div class="result-info">
                    <div class="result-name">${name} ${yearInfo}</div>
                    <div class="result-meta">${typeLabel} • ${category}</div>
                </div>
            `;

            if (id) {
                resultItem.addEventListener('click', () => {
                    window.location.href = `/watch?id=${id}`;
                });
            }

            container.appendChild(resultItem);
        });

        container.classList.add('active');
    }

    // Highlight current page in navigation
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.nav-link');

    navLinks.forEach(link => {
        const href = link.getAttribute('href');
        if (href && currentPath.includes(href) && href !== '#') {
            link.classList.add('active');
        }
    });
}