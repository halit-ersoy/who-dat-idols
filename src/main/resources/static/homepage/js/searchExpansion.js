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

    if (searchInput && searchResults) {
        searchInput.addEventListener('focus', () => {
            // This would typically fetch results from API
            searchResults.classList.add('active');
        });

        searchInput.addEventListener('blur', (e) => {
            // Don't hide if clicking on results
            if (!searchResults.contains(e.relatedTarget)) {
                setTimeout(() => {
                    searchResults.classList.remove('active');
                }, 200);
            }
        });
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