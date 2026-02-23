import { handleImageSkeleton } from '../../elements/userLogged.js';

export function initFeaturedContent() {
    const featuredGrid = document.querySelector('.featured-grid');
    const movieToggle = document.getElementById('featured-movies-toggle');
    const tvToggle = document.getElementById('featured-tv-toggle');
    const toggleContainer = document.querySelector('.featured-toggle');

    if (!featuredGrid) return;
    if (!movieToggle || !tvToggle || !toggleContainer) {
        console.error('Featured Content Section: Gerekli elementler bulunamadı.');
        return;
    }

    // Hem filmler hem de diziler verisini cache'lemek için nesne oluşturuluyor.
    let featuredData = { movies: null, tv: null };
    let currentMode = 'tv'; // Diziler seçili başlangıç durumu
    let isLoading = false;

    // Ülke kodları → isimleri sözlüğü. render fonksiyonundan önce tanımlı olmalı!
    const countryNames = {
        kr: 'Güney Kore',
        jp: 'Japonya',
        th: 'Tayland',
        cn: 'Çin',
        tw: 'Tayvan',
        ph: 'Filipinler',
        id: 'Endonezya',
        my: 'Malezya',
        sg: 'Singapur',
        be: 'Belçika',
        us: 'USA',
        ch: 'İsviçre',
        hk: 'Hong Kong',
        ca: 'Kanada',
        es: 'İspanya',
        in: 'Hindistan',
        vn: 'Vietnam',
        kh: 'Kamboçya',
        ee: 'Estonya',
        de: 'Almanya',
        fr: 'Fransa',
        hr: 'Hırvatistan',
        nl: 'Hollanda',
        gb: 'UK',
        at: 'Avusturya',
        it: 'İtalya'
    };

    // Set initial toggle state
    updateToggleState();

    // API'den veri çek
    fetch('/api/featured-content')
        .then(response => response.json())
        .then(data => {
            featuredData.movies = data.movies;
            featuredData.tv = data.tv;
            renderFeaturedContent(currentMode);
        })
        .catch(err => {
            console.error('Error fetching featured content:', err);
            featuredGrid.innerHTML = '<div class="error-message">Veri yüklenemedi.</div>';
        });

    // Toggle event'leri
    movieToggle.addEventListener('click', () => {
        if (currentMode !== 'movies' && !isLoading) {
            switchMode('movies');
        }
    });

    tvToggle.addEventListener('click', () => {
        if (currentMode !== 'tv' && !isLoading) {
            switchMode('tv');
        }
    });

    function switchMode(mode) {
        isLoading = true;
        currentMode = mode;
        updateToggleState();
        featuredGrid.classList.add('fade-out');
        setTimeout(() => {
            renderFeaturedContent(mode);
        }, 300);
    }

    function updateToggleState() {
        movieToggle.classList.toggle('active', currentMode === 'movies');
        tvToggle.classList.toggle('active', currentMode === 'tv');
        toggleContainer.classList.toggle('movies-active', currentMode === 'movies');
    }

    function renderFeaturedContent(type) {
        const data = featuredData[type];
        featuredGrid.innerHTML = '';

        if (data && data.length > 0) {
            data.forEach(item => {
                const el = document.createElement('div');
                el.className = 'featured-item';
                el.innerHTML = `
                    <div class="item-thumb img-skeleton">
                        <img src="${item.image}" alt="${item.title}">
                        ${item.isFinal ? '<span class="badge final-badge">Final</span>' : ''}
                    </div>
                    <div class="item-details">
                        <div class="item-header">
                            <div class="title-container">
                                <span class="title">${item.title}</span>
                            </div>
                        </div>
                        <div class="item-info">
                            ${type === 'movies'
                        ? '<span>Film</span>'
                        : `<span>Sezon ${item.season} Bölüm ${item.episode}</span>`
                    }
                            <span
                                class="country-flag ${item.country}"
                                title="${countryNames[item.country] || ''}"
                            ></span>
                            ${item.isNew ? '<span class="new-episode">• Yeni</span>' : ''}
                        </div>
                    </div>
                `;
                handleImageSkeleton(el.querySelector('img'));
                el.addEventListener('click', () => {
                    window.location.href = `/${item.id}`;
                });
                featuredGrid.appendChild(el);
            });

            // Fade-in animasyonu
            setTimeout(() => {
                featuredGrid.classList.remove('fade-out');
                featuredGrid.classList.add('fade-in');
                setTimeout(() => {
                    featuredGrid.classList.remove('fade-in');
                }, 500);
            }, 100);
        } else {
            featuredGrid.innerHTML = '<div class="error-message">Veri bulunamadı.</div>';
        }

        isLoading = false;
    }
}
