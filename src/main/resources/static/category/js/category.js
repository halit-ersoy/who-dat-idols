// category.js

document.addEventListener('DOMContentLoaded', () => {
    const path = window.location.pathname;
    let contentType = '';
    let seriesType = ''; // Dizi, Program, BL
    let pageTitle = '';

    // Determine context based on URL
    if (path.includes('/filmler')) {
        contentType = 'movie';
        pageTitle = 'Filmler';
        document.getElementById('filter-status-container').style.display = 'none'; // Hide status for movies
    } else if (path.includes('/diziler')) {
        contentType = 'series';
        seriesType = 'Dizi';
        pageTitle = 'Diziler';
        document.getElementById('filter-status-container').style.display = 'none';
    } else if (path.includes('/programlar')) {
        contentType = 'series';
        seriesType = 'Program';
        pageTitle = 'Programlar';
        document.getElementById('filter-status-container').style.display = 'none';
    }

    document.getElementById('page-title').textContent = pageTitle;
    document.title = `${pageTitle} - Who Dat Idols?`;

    // State
    const state = {
        categoryId: '',
        status: '',
        year: '',
        country: '',
        sort: 'newest',
        page: 1,
        size: 18,
        isLoading: false,
        hasMore: true
    };

    // Elements
    const els = {
        filterCat: document.getElementById('filter-category'),
        filterStatus: document.getElementById('filter-status'),
        filterYear: document.getElementById('filter-year'),
        filterCountry: document.getElementById('filter-country'),
        filterSort: document.getElementById('filter-sort'),
        resetBtn: document.getElementById('reset-filters-btn'),
        emptyResetBtn: document.getElementById('empty-state-reset'),
        grid: document.getElementById('results-grid'),
        loading: document.getElementById('loading-more'),
        emptyState: document.getElementById('empty-state'),
        loadMoreBtn: document.getElementById('load-more-btn'),
        paginationControls: document.getElementById('pagination-controls')
    };

    // Initialize
    async function init() {
        await fetchFilterOptions();
        loadContent(true);
        setupEventListeners();
    }

    async function fetchFilterOptions() {
        try {
            const res = await fetch(`/api/filters/options?type=${contentType}`);
            if (!res.ok) throw new Error('Failed to fetch options');
            const data = await res.json();

            // Populate Categories
            data.categories.forEach(cat => {
                const opt = document.createElement('option');
                opt.value = cat.id;
                opt.textContent = cat.name;
                els.filterCat.appendChild(opt);
            });

            // Populate Years
            data.years.forEach(year => {
                const opt = document.createElement('option');
                opt.value = year;
                opt.textContent = year;
                els.filterYear.appendChild(opt);
            });

            // Populate Countries
            data.countries.forEach(c => {
                const opt = document.createElement('option');
                opt.value = c.code;
                opt.textContent = c.name;
                els.filterCountry.appendChild(opt);
            });

        } catch (error) {
            console.error('Filter options error:', error);
        }
    }

    async function loadContent(reset = false) {
        if (state.isLoading) return;
        if (reset) {
            state.page = 1;
            state.hasMore = true;
            els.grid.innerHTML = '';
        }

        if (!state.hasMore) return;

        state.isLoading = true;
        els.loading.style.display = 'flex';
        els.emptyState.style.display = 'none';
        els.paginationControls.style.display = 'none';

        try {
            // Build URL
            let endpoint = contentType === 'movie' ? '/api/movies/filter' : '/api/series/filter';
            const params = new URLSearchParams({
                page: state.page,
                size: state.size,
                sort: state.sort
            });

            if (contentType === 'series' && seriesType) params.append('seriesType', seriesType);
            if (state.categoryId) params.append('categoryId', state.categoryId);
            if (state.status !== '') params.append('finalStatus', state.status);
            if (state.year) params.append('year', state.year);
            if (state.country) params.append('country', state.country);

            const url = `${endpoint}?${params.toString()}`;
            const res = await fetch(url);
            if (!res.ok) throw new Error('Failed to fetch content');

            const data = await res.json();

            renderCards(data.content);

            state.hasMore = state.page < data.totalPages;

            if (reset && data.content.length === 0) {
                els.emptyState.style.display = 'block';
            } else if (state.hasMore) {
                els.paginationControls.style.display = 'flex';
            }

            state.page++;
        } catch (error) {
            console.error('Error loading content:', error);
            if (reset) els.emptyState.style.display = 'block';
        } finally {
            state.isLoading = false;
            els.loading.style.display = 'none';
        }
    }

    function renderCards(items) {
        items.forEach(item => {
            const card = document.createElement('a');
            card.href = item.videoUrl || '#';
            card.className = 'media-card';

            // Reusing the badge logic optionally
            let badgesHTML = '';
            if (item.info) {
                badgesHTML = `<div class="card-info">${item.info}</div>`;
            }

            card.innerHTML = `
                <img src="${item.thumbnailUrl}" alt="${item.title}" loading="lazy" onerror="this.parentElement.classList.add('no-image'); this.style.display='none';">
                <div class="card-overlay">
                    ${badgesHTML}
                    <div class="card-title">${item.title}</div>
                </div>
            `;
            els.grid.appendChild(card);
        });
    }

    function setupEventListeners() {
        const handleFilterChange = () => {
            state.categoryId = els.filterCat.value;
            state.status = els.filterStatus.value;
            state.year = els.filterYear.value;
            state.country = els.filterCountry.value;
            state.sort = els.filterSort.value;
            loadContent(true);
        };

        els.filterCat.addEventListener('change', handleFilterChange);
        els.filterStatus.addEventListener('change', handleFilterChange);
        els.filterYear.addEventListener('change', handleFilterChange);
        els.filterCountry.addEventListener('change', handleFilterChange);
        els.filterSort.addEventListener('change', handleFilterChange);

        const handleReset = () => {
            els.filterCat.value = '';
            els.filterStatus.value = '';
            els.filterYear.value = '';
            els.filterCountry.value = '';
            els.filterSort.value = 'newest';
            handleFilterChange();
        };

        els.resetBtn.addEventListener('click', handleReset);
        els.emptyResetBtn.addEventListener('click', handleReset);

        els.loadMoreBtn.addEventListener('click', () => loadContent(false));
    }

    // Start
    init();
});
