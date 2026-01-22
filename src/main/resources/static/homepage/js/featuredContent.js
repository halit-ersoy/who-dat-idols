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
    let featuredData = {movies: null, tv: null};
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
        sg: 'Singapur'
    };

    // Set initial toggle state
    updateToggleState();

    const contentMovie = [
        {
            title: "Silent Horizon",
            season: null,
            episode: null,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/movie1/200/200",
            country: "kr"
        },
        {
            title: "Crimson Night",
            season: null,
            episode: null,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/movie2/200/200",
            country: "jp"
        },
        {
            title: "The Endless Road",
            season: null,
            episode: null,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/movie3/200/200",
            country: "th"
        },
        {
            title: "Forgotten Realm",
            season: null,
            episode: null,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/movie4/200/200",
            country: "cn"
        },
        {
            title: "Mirage of Time",
            season: null,
            episode: null,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/movie5/200/200",
            country: "tw"
        },
        {
            title: "Shadow of the Titan",
            season: null,
            episode: null,
            isNew: true,
            isFinal: true,
            image: "https://picsum.photos/seed/movie6/200/200",
            country: "ph"
        },
        {
            title: "Echoes of Silence",
            season: null,
            episode: null,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/movie7/200/200",
            country: "id"
        },
        {
            title: "Wings of Dawn",
            season: null,
            episode: null,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/movie8/200/200",
            country: "my"
        },
        {
            title: "City of Glass",
            season: null,
            episode: null,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/movie9/200/200",
            country: "sg"
        },
        {
            title: "Beyond the Stars",
            season: null,
            episode: null,
            isNew: true,
            isFinal: true,
            image: "https://picsum.photos/seed/movie10/200/200",
            country: "kr"
        },
        {
            title: "Whispering Woods",
            season: null,
            episode: null,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/movie11/200/200",
            country: "jp"
        },
        {
            title: "Valley of Dreams",
            season: null,
            episode: null,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/movie12/200/200",
            country: "th"
        },
        {
            title: "Lost in Translation",
            season: null,
            episode: null,
            isNew: false,
            isFinal: false,
            image: "https://picsum.photos/seed/movie13/200/200",
            country: "cn"
        },
        {
            title: "Midnight Sun",
            season: null,
            episode: null,
            isNew: false,
            isFinal: false,
            image: "https://picsum.photos/seed/movie14/200/200",
            country: "tw"
        },
        {
            title: "Neon Skies",
            season: null,
            episode: null,
            isNew: false,
            isFinal: true,
            image: "https://picsum.photos/seed/movie15/200/200",
            country: "ph"
        },
        {
            title: "Broken Compass",
            season: null,
            episode: null,
            isNew: false,
            isFinal: false,
            image: "https://picsum.photos/seed/movie16/200/200",
            country: "id"
        },
        {
            title: "Hollow Heart",
            season: null,
            episode: null,
            isNew: false,
            isFinal: false,
            image: "https://picsum.photos/seed/movie17/200/200",
            country: "my"
        },
        {
            title: "Rivers of Gold",
            season: null,
            episode: null,
            isNew: false,
            isFinal: false,
            image: "https://picsum.photos/seed/movie18/200/200",
            country: "sg"
        },
        {
            title: "Glass Bridges",
            season: null,
            episode: null,
            isNew: false,
            isFinal: false,
            image: "https://picsum.photos/seed/movie19/200/200",
            country: "kr"
        },
        {
            title: "Crystal Caves",
            season: null,
            episode: null,
            isNew: false,
            isFinal: true,
            image: "https://picsum.photos/seed/movie20/200/200",
            country: "jp"
        }
    ];

    const contentSoapOpera = [
        {
            title: "Legends United",
            season: 1,
            episode: 1,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/tv1/200/200",
            country: "kr"
        },
        {
            title: "Broken Tales",
            season: 1,
            episode: 4,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/tv2/200/200",
            country: "jp"
        },
        {
            title: "Mystic Falls",
            season: 2,
            episode: 3,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/tv3/200/200",
            country: "th"
        },
        {
            title: "Quantum Leap",
            season: 3,
            episode: 10,
            isNew: true,
            isFinal: true,
            image: "https://picsum.photos/seed/tv4/200/200",
            country: "cn"
        },
        {
            title: "Steel Horizon",
            season: 1,
            episode: 12,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/tv5/200/200",
            country: "tw"
        },
        {
            title: "Burning Skies",
            season: 4,
            episode: 8,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/tv6/200/200",
            country: "ph"
        },
        {
            title: "Ocean's Secret",
            season: 2,
            episode: 15,
            isNew: true,
            isFinal: true,
            image: "https://picsum.photos/seed/tv7/200/200",
            country: "id"
        },
        {
            title: "Viral",
            season: 3,
            episode: 6,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/tv8/200/200",
            country: "my"
        },
        {
            title: "Deep State",
            season: 5,
            episode: 2,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/tv9/200/200",
            country: "sg"
        },
        {
            title: "Rising Dawn",
            season: 1,
            episode: 22,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/tv10/200/200",
            country: "kr"
        },
        {
            title: "Night Watch",
            season: 2,
            episode: 14,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/tv11/200/200",
            country: "jp"
        },
        {
            title: "Starlight",
            season: 3,
            episode: 9,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/tv12/200/200",
            country: "th"
        },
        {
            title: "Eternal Quest",
            season: 4,
            episode: 20,
            isNew: false,
            isFinal: true,
            image: "https://picsum.photos/seed/tv13/200/200",
            country: "cn"
        },
        {
            title: "Phantom Calls",
            season: 1,
            episode: 8,
            isNew: false,
            isFinal: false,
            image: "https://picsum.photos/seed/tv14/200/200",
            country: "tw"
        },
        {
            title: "Golden Guardians",
            season: 2,
            episode: 10,
            isNew: false,
            isFinal: false,
            image: "https://picsum.photos/seed/tv15/200/200",
            country: "ph"
        },
        {
            title: "Shadow Ops",
            season: 3,
            episode: 11,
            isNew: false,
            isFinal: false,
            image: "https://picsum.photos/seed/tv16/200/200",
            country: "id"
        },
        {
            title: "Cyber Frontier",
            season: 5,
            episode: 7,
            isNew: false,
            isFinal: false,
            image: "https://picsum.photos/seed/tv17/200/200",
            country: "my"
        },
        {
            title: "Lost Colony",
            season: 4,
            episode: 5,
            isNew: false,
            isFinal: true,
            image: "https://picsum.photos/seed/tv18/200/200",
            country: "sg"
        },
        {
            title: "Midnight Code",
            season: 2,
            episode: 18,
            isNew: false,
            isFinal: false,
            image: "https://picsum.photos/seed/tv19/200/200",
            country: "kr"
        },
        {
            title: "Celestial Wars",
            season: 3,
            episode: 13,
            isNew: false,
            isFinal: false,
            image: "https://picsum.photos/seed/tv20/200/200",
            country: "jp"
        }
    ];

    // İçerikleri ata
    featuredData.tv = contentSoapOpera;
    featuredData.movies = contentMovie;

    // İlk içerik render
    renderFeaturedContent('tv');

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
                    </div>
                    <div class="item-details">
                        <div class="item-header">
                            <div class="title-container">
                                <span class="title">${item.title}</span>
                            </div>
                            ${item.isFinal ? '<span class="badge final-badge">Final</span>' : ''}
                        </div>
                        <div class="item-info">
                            ${
                    type === 'movies'
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
                    window.location.href = `/watch?title=${encodeURIComponent(item.title)}`;
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
