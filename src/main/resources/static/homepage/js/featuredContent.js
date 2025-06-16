export function initFeaturedContent() {
    const featuredGrid = document.querySelector('.featured-grid');
    if (!featuredGrid) return;
    featuredGrid.innerHTML = '';

    const content = [
        {
            title: "Second Shot at Love",
            type: "Dizi",
            season: 1,
            episode: 10,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/1/200/200",
            country: "kr"
        },
        {
            title: "Tastefully Yours",
            type: "Dizi",
            season: 1,
            episode: 10,
            isNew: false,
            isFinal: true,
            image: "https://picsum.photos/seed/2/200/200",
            country: "jp"
        },
        {
            title: "Good Boy",
            type: "Dizi",
            season: 1,
            episode: 6,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/3/200/200",
            country: "th"
        },
        {
            title: "Our Unwritten Seoul",
            type: "Dizi",
            season: 1,
            episode: 8,
            isNew: false,
            isFinal: false,
            image: "https://picsum.photos/seed/4/200/200",
            country: "kr"
        },
        {
            title: "Oh My Ghost Clients",
            type: "Dizi",
            season: 1,
            episode: 6,
            isNew: false,
            isFinal: false,
            image: "https://picsum.photos/seed/5/200/200",
            country: "cn"
        },
        {
            title: "The Haunted Palace",
            type: "Dizi",
            season: 1,
            episode: 16,
            isNew: false,
            isFinal: true,
            image: "https://picsum.photos/seed/6/200/200",
            country: "tw"
        },
        {
            title: "Law and the City",
            type: "Dizi",
            season: 1,
            episode: 2,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/7/200/200",
            country: "jp"
        },
        {
            title: "Mercy for None",
            type: "Dizi",
            season: 1,
            episode: 7,
            isNew: true,
            isFinal: true,
            image: "https://picsum.photos/seed/8/200/200",
            country: "kr"
        },
        {
            title: "Pump Up the Healthy Love",
            type: "Dizi",
            season: 1,
            episode: 12,
            isNew: false,
            isFinal: true,
            image: "https://picsum.photos/seed/9/200/200",
            country: "th"
        },
        {
            title: "Ayashii Partner",
            type: "Dizi",
            season: 1,
            episode: 6,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/10/200/200",
            country: "jp"
        },
        {
            title: "Spring of Youth",
            type: "Dizi",
            season: 1,
            episode: 7,
            isNew: true,
            isFinal: false,
            image: "https://picsum.photos/seed/11/200/200",
            country: "cn"
        },
        {
            title: "The Beginning After the End",
            type: "Anime",
            season: 1,
            episode: 11,
            isNew: false,
            isFinal: false,
            image: "https://picsum.photos/seed/12/200/200",
            country: "jp"
        },
        // ... diğer öğeler
    ];

    const countryNames = {
        'kr': 'Güney Kore',
        'jp': 'Japonya',
        'th': 'Tayland',
        'cn': 'Çin',
        'tw': 'Tayvan',
        'ph': 'Filipinler',
        'id': 'Endonezya',
        'my': 'Malezya',
        'sg': 'Singapur'
    };

    content.forEach(item => {
        const el = document.createElement('div');
        el.className = 'featured-item';
        el.innerHTML = `
      <div class="item-thumb">
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
          <span>${item.type} • S${item.season}B${item.episode}</span>
          <span class="country-flag ${item.country}" title="${countryNames[item.country] || ''}"></span>
          ${item.isNew ? '<span class="new-episode">• Yeni Bölüm</span>' : ''}
        </div>
      </div>
    `;
        el.addEventListener('click', () => {
            window.location.href = `/watch?title=${encodeURIComponent(item.title)}`;
        });
        featuredGrid.appendChild(el);
    });
}