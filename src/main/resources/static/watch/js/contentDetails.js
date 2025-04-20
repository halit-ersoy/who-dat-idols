// contentDetails.js
export function initContentDetails(videoId) {
    const detailsToggle    = document.querySelector('.details-toggle');
    const detailsContainer = document.querySelector('.content-details-container');

    detailsToggle?.addEventListener('click', () => {
        detailsToggle.classList.toggle('active');
        detailsContainer.classList.toggle('open');
    });

    if (videoId) loadContentDetails(videoId);
}

function loadContentDetails(id) {
    // Örnek statik veri; ihtiyaç halinde API çağrısı yapılabilir
    const data = {
        title: "Who Dat Idols - Grup Belgeseli",
        poster: "https://picsum.photos/400/600",
        rating: "9.2", year: "2024", duration: "45 dk", language: "Türkçe",
        plot: "K-Pop dünyasının yeni yıldızları 'Who Dat Idols' grubunun oluşum hikayesi, ...",
        genres: ["Belgesel","Müzik","Biyografi"],
        season: 1, episode: +id, totalEpisodes: 10,
        cast: [
            {name:"Ji-soo Park", role:"Kendisi", avatar:"https://picsum.photos/200"},
            {name:"Min-ho Lee", role:"Kendisi", avatar:"https://picsum.photos/201"},
            {name:"Jae-hyun Kim",role:"Kendisi", avatar:"https://picsum.photos/202"},
            {name:"Yuna Choi",role:"Anlatıcı",avatar:"https://picsum.photos/203"},
            {name:"Seo-jun Jung",role:"Yapımcı",avatar:"https://picsum.photos/204"}
        ]
    };

    document.getElementById('contentTitle').textContent     = data.title;
    document.getElementById('contentPoster').src           = data.poster;
    document.getElementById('contentRating').textContent   = data.rating;
    document.getElementById('releaseYear').textContent     = data.year;
    document.getElementById('contentDuration').textContent = data.duration;
    document.getElementById('contentLanguage').textContent = data.language;
    document.getElementById('contentPlot').textContent     = data.plot;
    document.getElementById('seasonNumber').textContent    = `Sezon ${data.season}`;
    document.getElementById('episodeNumber').textContent   = `Bölüm ${data.episode}`;
    document.getElementById('totalEpisodes').textContent   = `Toplam ${data.totalEpisodes} Bölüm`;

    const genresEl = document.getElementById('genreTags');
    genresEl.innerHTML = '';
    data.genres.forEach(g => {
        const span = document.createElement('span');
        span.className = 'genre-tag';
        span.textContent = g;
        genresEl.appendChild(span);
    });

    const castList = document.getElementById('castList');
    castList.innerHTML = '';
    data.cast.forEach(m => {
        const div = document.createElement('div');
        div.className = 'cast-member';
        div.innerHTML = `
      <div class="cast-avatar">
        <img src="${m.avatar}" alt="${m.name}">
      </div>
      <div class="cast-name">${m.name}</div>
      <div class="cast-role">${m.role}</div>
    `;
        castList.appendChild(div);
    });
}
