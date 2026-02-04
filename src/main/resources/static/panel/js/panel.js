document.addEventListener('DOMContentLoaded', function () {

    // Sidebar navigation logic
    const navLinks = document.querySelectorAll('.admin-nav .nav-link');
    const sections = document.querySelectorAll('.admin-section');

    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const targetId = link.getAttribute('data-section');

            // Update active state in nav
            navLinks.forEach(l => l.classList.remove('active'));
            link.classList.add('active');

            // Show target section, hide others
            sections.forEach(section => {
                if (section.id === targetId) {
                    section.style.display = 'block';
                } else {
                    section.style.display = 'none';
                }
            });

            // Scroll to top of main content
            document.querySelector('.main-content').scrollTop = 0;
        });
    });

    // Helper: Update Poster Preview
    function updatePosterPreview(section, urlOrSource) {
        const preview = document.getElementById(`${section}PosterPreview`);
        if (!preview) return;

        if (!urlOrSource) {
            preview.innerHTML = '<i class="fas fa-image"></i>';
            return;
        }

        preview.innerHTML = `<img src="${urlOrSource}" alt="Poster Önizleme">`;
    }

    // Sayfa açılınca mevcut listeleri çek
    fetchHeroVideos();
    fetchMovies();
    fetchSeries();

    /* ===========================================================
       TOPLU SİLME (BULK DELETE) YÖNETİMİ
       =========================================================== */
    let selectedMovieIds = new Set();
    let selectedSeriesIds = new Set();
    const btnDeleteMovies = document.getElementById('btnDeleteSelectedMovies');
    const btnDeleteSeries = document.getElementById('btnDeleteSelectedSeries');
    const spanCountMovies = document.getElementById('countSelectedMovies');
    const spanCountSeries = document.getElementById('countSelectedSeries');
    const checkSelectAllMovies = document.getElementById('selectAllMovies');
    const checkSelectAllSeries = document.getElementById('selectAllSeries');

    // -- MOVIE LOGIC --
    if (checkSelectAllMovies) {
        checkSelectAllMovies.addEventListener('change', function () {
            const shouldSelectAll = this.checked;
            const checkboxes = document.querySelectorAll('.movie-checkbox');
            checkboxes.forEach(cb => {
                cb.checked = shouldSelectAll;
                toggleMovieSelection(cb.value, shouldSelectAll);
            });
        });
    }

    window.toggleMovieSelection = function (id, isChecked) {
        if (isChecked) selectedMovieIds.add(id);
        else selectedMovieIds.delete(id);
        updateBulkUI();
    };

    // -- SERIES LOGIC --
    if (checkSelectAllSeries) {
        checkSelectAllSeries.addEventListener('change', function () {
            const shouldSelectAll = this.checked;
            const checkboxes = document.querySelectorAll('.series-checkbox');
            checkboxes.forEach(cb => {
                cb.checked = shouldSelectAll;
                toggleSeriesSelection(cb.value, shouldSelectAll);
            });
        });
    }
    window.toggleSeriesSelection = function (id, isChecked) {
        if (isChecked) selectedSeriesIds.add(id);
        else selectedSeriesIds.delete(id);
        updateBulkUI();
    };

    function updateBulkUI() {
        // Movies
        spanCountMovies.innerText = selectedMovieIds.size;
        btnDeleteMovies.style.display = selectedMovieIds.size > 0 ? 'inline-block' : 'none';

        // Series
        spanCountSeries.innerText = selectedSeriesIds.size;
        btnDeleteSeries.style.display = selectedSeriesIds.size > 0 ? 'inline-block' : 'none';

        // Select All Checkbox state (Movies)
        if (checkSelectAllMovies) {
            const allMovies = document.querySelectorAll('.movie-checkbox');
            if (allMovies.length > 0 && selectedMovieIds.size === allMovies.length) checkSelectAllMovies.checked = true;
            else checkSelectAllMovies.checked = false;
        }

        // Select All Checkbox state (Series)
        if (checkSelectAllSeries) {
            const allSeries = document.querySelectorAll('.series-checkbox');
            if (allSeries.length > 0 && selectedSeriesIds.size === allSeries.length) checkSelectAllSeries.checked = true;
            else checkSelectAllSeries.checked = false;
        }
    }

    if (btnDeleteMovies) {
        btnDeleteMovies.addEventListener('click', function () {
            if (!confirm(`${selectedMovieIds.size} adet filmi silmek istediğinize emin misiniz?`)) return;

            fetch('/admin/delete-movies-bulk', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(Array.from(selectedMovieIds))
            }).then(res => res.text()).then(msg => {
                alert(msg);
                selectedMovieIds.clear();
                updateBulkUI();
                fetchMovies(); // Tabloyu yenile
            }).catch(err => alert("Hata: " + err));
        });
    }

    if (btnDeleteSeries) {
        btnDeleteSeries.addEventListener('click', function () {
            if (!confirm(`${selectedSeriesIds.size} adet diziyi (ve tüm bölümlerini) silmek istediğinize emin misiniz?`)) return;

            fetch('/admin/delete-series-bulk', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(Array.from(selectedSeriesIds))
            }).then(res => res.text()).then(msg => {
                alert(msg);
                selectedSeriesIds.clear();
                updateBulkUI();
                fetchSeries(); // Listeyi yenile
            }).catch(err => alert("Hata: " + err));
        });
    }

    /* ===========================================================
       HERO YÖNETİMİ
       =========================================================== */
    const heroForm = document.getElementById('heroForm');
    const heroSubmitBtn = document.getElementById('heroSubmitBtn');
    const heroSearchInput = document.getElementById('heroContentSearch');
    const heroSearchResults = document.getElementById('heroSearchResults');
    const selectedContentIdInput = document.getElementById('selectedContentId');
    const selectedContentTypeInput = document.getElementById('selectedContentType');
    const selectedContentDisplay = document.getElementById('selectedContentDisplay');

    // Archive search logic for Hero (DYNAMIC SEARCH)
    let searchTimeout;

    heroSearchInput.addEventListener('input', function () {
        const query = this.value.trim();

        clearTimeout(searchTimeout);

        if (query.length < 2) {
            heroSearchResults.innerHTML = '';
            heroSearchResults.style.display = 'none';
            return;
        }

        searchTimeout = setTimeout(async () => {
            try {
                const response = await fetch(`/api/search?q=${encodeURIComponent(query)}`);
                const data = await response.json();
                renderHeroSearchResults(data);
            } catch (error) {
                console.error('Search error:', error);
            }
        }, 300);
    });

    function renderHeroSearchResults(results) {
        heroSearchResults.innerHTML = '';
        if (results.length === 0) {
            heroSearchResults.innerHTML = '<div class="search-result-item">Sonuç bulunamadı</div>';
        } else {
            results.slice(0, 10).forEach(item => {
                const div = document.createElement('div');
                div.className = 'search-result-item';

                // API uses Name and Type (capitalized)
                const name = item.Name || item.name;
                const type = item.Type || item.type;
                const id = item.ID || item.id;

                div.innerHTML = `
                    <div class="result-info">
                        <span style="font-weight: 600;">${name}</span>
                        <span class="item-type">${type === 'Movie' ? 'Film' : 'Dizi'}</span>
                    </div>
                `;
                div.onclick = () => selectHeroContent({ id, name, type });
                heroSearchResults.appendChild(div);
            });
        }
        heroSearchResults.style.display = 'block';
    }

    function selectHeroContent(item) {
        selectedContentIdInput.value = item.id;
        selectedContentTypeInput.value = item.type;
        selectedContentDisplay.querySelector('strong').innerText = item.name + ' (' + (item.type === 'Movie' ? 'Film' : 'Dizi') + ')';
        selectedContentDisplay.style.display = 'flex';
        heroSearchResults.style.display = 'none';
        heroSearchInput.value = '';
    }

    document.querySelector('.btn-clear').onclick = () => {
        selectedContentIdInput.value = '';
        selectedContentTypeInput.value = '';
        selectedContentDisplay.style.display = 'none';
    };

    heroForm.addEventListener('submit', function (e) {
        e.preventDefault();
        const contentId = selectedContentIdInput.value;
        const type = selectedContentTypeInput.value;
        const file = document.getElementById('heroFile').files[0];

        if (!contentId) {
            alert("Lütfen arşivden bir içerik seçin!");
            return;
        }

        const formData = new FormData();
        formData.append('contentId', contentId);
        formData.append('type', type);
        formData.append('file', file);

        uploadDataWithProgress('/admin/add-hero', formData, 'heroForm', 'progressWrapperHero', 'progressBarHero', 'percentHero', () => {
            fetchHeroVideos();
            selectedContentDisplay.style.display = 'none';
            selectedContentIdInput.value = '';
            selectedContentTypeInput.value = '';
        });
    });

    function fetchHeroVideos() {
        fetch('/admin/hero-videos')
            .then(res => res.json())
            .then(heroes => {
                const tbody = document.querySelector('#heroTable tbody');
                tbody.innerHTML = '';
                heroes.forEach((hero, index) => {
                    const tr = document.createElement('tr');
                    tr.dataset.id = hero.ID || hero.id;
                    const name = hero.name || hero.Name || 'Adsız';
                    const type = hero.type || hero.Type || '-';
                    const id = hero.ID || hero.id;
                    const category = hero.category || hero.Category || '-';

                    tr.innerHTML = `
                        <td>
                            <div class="order-controls">
                                <button class="btn-order" onclick="moveHero(${index}, -1)"><i class="fas fa-chevron-up"></i></button>
                                <button class="btn-order" onclick="moveHero(${index}, 1)"><i class="fas fa-chevron-down"></i></button>
                            </div>
                        </td>
                        <td style="font-weight: 600;">${name}</td>
                        <td>${category}</td>
                        <td>${type}</td>
                        <td>
                            <button class="btn btn-sm btn-danger" onclick='deleteHero("${id}", "${name.replace(/'/g, "\\'")}")'><i class="fas fa-trash"></i> SİL</button>
                        </td>
                    `;
                    tbody.appendChild(tr);
                });
                window.currentHeroes = heroes;
            })
            .catch(err => console.error("Hero listesi hatası:", err));
    }

    window.moveHero = function (index, direction) {
        const heroes = window.currentHeroes;
        if (!heroes) return;

        const newIndex = index + direction;
        if (newIndex < 0 || newIndex >= heroes.length) return;

        // Swap elements
        const temp = heroes[index];
        heroes[index] = heroes[newIndex];
        heroes[newIndex] = temp;

        // Update UI immediately for responsiveness
        renderHeroesTable(heroes);

        // Send new order to server
        const idList = heroes.map(h => h.ID || h.id);
        fetch('/admin/update-hero-order', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(idList)
        }).then(res => {
            if (!res.ok) alert("Sıralama güncellenirken hata oluştu.");
        });
    };

    function renderHeroesTable(heroes) {
        const tbody = document.querySelector('#heroTable tbody');
        tbody.innerHTML = '';
        heroes.forEach((hero, index) => {
            const tr = document.createElement('tr');
            tr.dataset.id = hero.ID || hero.id;
            const name = hero.name || hero.Name || 'Adsız';
            const type = hero.type || hero.Type || '-';
            const id = hero.ID || hero.id;
            const category = hero.category || hero.Category || '-';

            tr.innerHTML = `
                <td>
                    <div class="order-controls">
                        <button class="btn-order" onclick="moveHero(${index}, -1)"><i class="fas fa-chevron-up"></i></button>
                        <button class="btn-order" onclick="moveHero(${index}, 1)"><i class="fas fa-chevron-down"></i></button>
                    </div>
                </td>
                <td style="font-weight: 600;">${name}</td>
                <td>${category}</td>
                <td>${type}</td>
                <td>
                    <button class="btn btn-sm btn-danger" onclick='deleteHero("${id}", "${name.replace(/'/g, "\\'")}")'><i class="fas fa-trash"></i> SİL</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    window.deleteHero = function (id, name) {
        if (!confirm(`'${name}' hero içeriğini silmek istediğinize emin misiniz?`)) return;
        fetch(`/admin/delete-hero?id=${id}`, { method: 'DELETE' })
            .then(res => {
                if (res.ok) { alert("Başarıyla silindi."); fetchHeroVideos(); }
                else alert("Hata oluştu.");
            });
    };

    /* ===========================================================
       FİLM YÖNETİMİ
       =========================================================== */
    const movieForm = document.getElementById('movieForm');
    const movieCancelBtn = document.getElementById('cancelMovieEditBtn');
    const movieSubmitBtn = document.getElementById('movieSubmitBtn');
    const movieFileInput = document.getElementById('movieFile');
    const movieImageInput = document.getElementById('movieImage');
    const movieIdInput = document.getElementById('movieId');
    const moviePosterUrlInput = document.getElementById('movieImageUrl');

    // Live preview for file selection
    movieImageInput.addEventListener('change', function () {
        if (this.files && this.files[0]) {
            const reader = new FileReader();
            reader.onload = (e) => updatePosterPreview('movie', e.target.result);
            reader.readAsDataURL(this.files[0]);
        }
    });

    // Live preview for URL input
    moviePosterUrlInput.addEventListener('input', function () {
        const url = this.value.trim();
        lastFetchedPosterUrl = url; // Manually entering a URL counts as the source
        updatePosterPreview('movie', url);
    });

    movieForm.addEventListener('submit', function (e) {
        e.preventDefault();
        const movieId = movieIdInput.value;
        const isEditMode = movieId !== "";

        if (isEditMode) {
            const formData = new FormData();
            formData.append('id', movieId);
            formData.append('name', document.getElementById('movieName').value);
            formData.append('category', document.getElementById('movieCategory').value);
            formData.append('content', document.getElementById('movieSummary').value);
            formData.append('year', document.getElementById('movieYear').value);
            formData.append('language', document.getElementById('movieLanguage').value);

            if (movieImageInput.files.length > 0) {
                formData.append('image', movieImageInput.files[0]);
            }
            if (movieFileInput.files.length > 0) {
                formData.append('file', movieFileInput.files[0]);
            }

            movieSubmitBtn.innerText = "GÜNCELLENİYOR...";
            movieSubmitBtn.disabled = true;

            fetch('/admin/update-movie', {
                method: 'POST',
                body: formData
            }).then(res => res.text()).then(msg => {
                alert(msg); resetMovieForm(); fetchMovies();
            }).finally(() => { movieSubmitBtn.disabled = false; });
        } else {
            const formData = new FormData();
            formData.append('name', document.getElementById('movieName').value);
            formData.append('category', document.getElementById('movieCategory').value);
            formData.append('summary', document.getElementById('movieSummary').value);
            formData.append('year', document.getElementById('movieYear').value);
            formData.append('language', document.getElementById('movieLanguage').value);
            if (movieFileInput.files.length > 0) formData.append('file', movieFileInput.files[0]);
            if (movieImageInput.files.length > 0) formData.append('image', movieImageInput.files[0]);
            if (lastFetchedPosterUrl) formData.append('imageUrl', lastFetchedPosterUrl);

            uploadDataWithProgress('/admin/add-movie', formData, 'movieForm', 'progressWrapperMovie', 'progressBarMovie', 'percentMovie', () => {
                fetchMovies();
            });
        }
    });

    function fetchMovies() {
        fetch('/admin/movies')
            .then(res => res.json())
            .then(movies => {
                window.currentMovies = movies;
                updateDashboardStats();
                const tbody = document.querySelector('#movieTable tbody');
                tbody.innerHTML = '';
                movies.forEach(movie => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td><input type="checkbox" class="movie-checkbox" value="${movie.id}" onchange="toggleMovieSelection('${movie.id}', this.checked)"></td>
                        <td style="font-weight: 600;">${movie.name}</td>
                        <td>${movie.year}</td>
                        <td style="color: var(--text-dim);">${movie.category}</td>
                        <td>
                            <button class="btn btn-sm btn-edit" onclick='editMovie(${JSON.stringify(movie).replace(/'/g, "&#39;")})'><i class="fas fa-edit"></i> DÜZENLE</button>
                            <button class="btn btn-sm btn-danger" onclick='deleteMovie("${movie.id}", "${movie.name.replace(/'/g, "\\'")}")'><i class="fas fa-trash"></i> SİL</button>
                        </td>
                    `;
                    tbody.appendChild(tr);
                });
            })
            .catch(err => console.error("Film listesi hatası:", err));
    }

    window.editMovie = function (movie) {
        movieIdInput.value = movie.id;
        document.getElementById('movieName').value = movie.name;
        document.getElementById('movieCategory').value = movie.category;
        document.getElementById('movieSummary').value = movie.content;
        document.getElementById('movieYear').value = movie.year;
        document.getElementById('movieLanguage').value = movie.language;

        movieSubmitBtn.innerText = "DEĞİŞİKLİKLERİ KAYDET";
        movieSubmitBtn.classList.remove('btn-primary');
        movieSubmitBtn.style.backgroundColor = "#ffc107";
        movieSubmitBtn.style.color = "#000";
        movieCancelBtn.style.display = "block";
        movieFileInput.removeAttribute('required');

        // Switch to movie section
        const movieLink = document.querySelector('.nav-link[data-section="movie-section"]');
        if (movieLink) movieLink.click();
    };

    function updateMovie(data) {
        movieSubmitBtn.innerText = "GÜNCELLENİYOR...";
        movieSubmitBtn.disabled = true;
        fetch('/admin/update-movie', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        }).then(res => res.text()).then(msg => {
            alert(msg); resetMovieForm(); fetchMovies();
        }).finally(() => { movieSubmitBtn.disabled = false; });
    }

    movieCancelBtn.addEventListener('click', resetMovieForm);
    function resetMovieForm() {
        movieForm.reset();
        movieIdInput.value = "";
        movieSubmitBtn.innerText = "FİLMİ KAYDET";
        movieSubmitBtn.classList.add('btn-primary');
        movieSubmitBtn.style.backgroundColor = "";
        movieSubmitBtn.style.color = "";
        movieCancelBtn.style.display = "none";
        movieFileInput.setAttribute('required', 'required');

        // Resim önizlemesi veya dosya adını temizlemek gerekirse buraya eklenebilir
        if (movieImageInput) movieImageInput.value = "";
        if (moviePosterUrlInput) moviePosterUrlInput.value = "";
        lastFetchedPosterUrl = "";
        updatePosterPreview('movie', null);
    }

    window.deleteMovie = function (id, name) {
        if (!confirm(`'${name}' filmini silmek istediğinize emin misiniz?`)) return;

        fetch(`/admin/delete-movie?id=${id}`, {
            method: 'DELETE'
        }).then(res => {
            if (res.ok) {
                alert("Film başarıyla silindi.");
                fetchMovies();
            } else {
                res.text().then(msg => alert("Hata: " + msg));
            }
        }).catch(err => alert("Silme işlemi sırasında hata oluştu: " + err));
    };


    /* ===========================================================
       DİZİ YÖNETİMİ
       =========================================================== */
    const seriesForm = document.getElementById('seriesForm');
    const seriesIdInput = document.getElementById('seriesId');
    const seriesCancelBtn = document.getElementById('cancelSeriesEditBtn');
    const seriesSubmitBtn = document.getElementById('seriesSubmitBtn');
    const seriesFileInput = document.getElementById('seriesFile');
    const seriesImageInput = document.getElementById('seriesImage');
    const seriesPosterUrlInput = document.getElementById('seriesImageUrl');

    // Live preview for file selection
    seriesImageInput.addEventListener('change', function () {
        if (this.files && this.files[0]) {
            const reader = new FileReader();
            reader.onload = (e) => updatePosterPreview('series', e.target.result);
            reader.readAsDataURL(this.files[0]);
        }
    });

    // Live preview for URL input
    seriesPosterUrlInput.addEventListener('input', function () {
        const url = this.value.trim();
        lastFetchedPosterUrl = url;
        updatePosterPreview('series', url);
    });

    // Series existence check
    const seriesNameInput = document.getElementById('seriesName');
    const seriesExistsWarning = document.createElement('div');
    seriesExistsWarning.style.color = 'var(--danger)';
    seriesExistsWarning.style.fontSize = '0.85rem';
    seriesExistsWarning.style.marginTop = '5px';
    seriesExistsWarning.style.display = 'none';
    seriesExistsWarning.innerHTML = '<i class="fas fa-exclamation-triangle"></i> Bu isimde bir dizi zaten mevcut!';
    seriesNameInput.parentNode.appendChild(seriesExistsWarning);

    let checkTimeout;
    seriesNameInput.addEventListener('input', function () {
        const name = this.value.trim();
        const mode = document.querySelector('input[name="seriesMode"]:checked').value;
        const sId = seriesIdInput.value;

        // Only check in "new" mode and when not editing
        if (mode !== 'new' || sId !== "" || name.length < 2) {
            seriesExistsWarning.style.display = 'none';
            return;
        }

        clearTimeout(checkTimeout);
        checkTimeout = setTimeout(async () => {
            try {
                const res = await fetch(`/admin/series/check?name=${encodeURIComponent(name)}`);
                const data = await res.json();
                if (data.exists) {
                    seriesExistsWarning.style.display = 'block';
                } else {
                    seriesExistsWarning.style.display = 'none';
                }
            } catch (e) { console.error("Check failed", e); }
        }, 500);
    });

    seriesForm.addEventListener('submit', async function (e) {
        e.preventDefault();
        const sId = seriesIdInput.value;
        const mode = document.querySelector('input[name="seriesMode"]:checked').value;

        // Block if series exists and we are in New mode
        if (sId === "" && mode === 'new') {
            const name = seriesNameInput.value.trim();
            try {
                const res = await fetch(`/admin/series/check?name=${encodeURIComponent(name)}`);
                const data = await res.json();
                if (data.exists) {
                    alert("Bu isimde bir dizi zaten mevcut! Lütfen 'Mevcut Seriye Ekle' modunu kullanın.");
                    seriesExistsWarning.style.display = 'block';
                    return;
                }
            } catch (e) { }
        }

        if (sId !== "") {
            const formData = new FormData();
            formData.append('id', sId);
            formData.append('name', document.getElementById('seriesName').value);
            formData.append('category', document.getElementById('seriesCategory').value);
            formData.append('content', document.getElementById('seriesSummary').value);
            formData.append('language', document.getElementById('seriesLanguage').value);

            if (seriesImageInput.files.length > 0) {
                formData.append('image', seriesImageInput.files[0]);
            }
            if (seriesFileInput.files.length > 0) {
                formData.append('file', seriesFileInput.files[0]);
            }

            seriesSubmitBtn.innerText = "GÜNCELLENİYOR...";
            seriesSubmitBtn.disabled = true;

            fetch('/admin/update-series', {
                method: 'POST',
                body: formData
            }).then(res => res.text()).then(msg => {
                alert(msg); resetSeriesForm(); fetchSeries();
            }).catch(err => alert("Hata: " + err))
                .finally(() => { seriesSubmitBtn.disabled = false; });
        } else {
            const formData = new FormData();
            const mode = document.querySelector('input[name="seriesMode"]:checked').value;
            const existingId = document.getElementById('selectedSeriesId').value;

            // Common fields for both modes
            formData.append('season', document.getElementById('seasonNum').value);
            formData.append('episode', document.getElementById('episodeNum').value);
            if (seriesFileInput.files.length > 0) formData.append('file', seriesFileInput.files[0]);

            if (mode === 'existing') {
                if (!existingId) return alert("Lütfen bir dizi seçin!");
                formData.append('existingSeriesId', existingId);
                // No need for name, category etc.
            } else {
                // New Series Mode
                formData.append('name', document.getElementById('seriesName').value);
                formData.append('category', document.getElementById('seriesCategory').value);
                formData.append('summary', document.getElementById('seriesSummary').value);
                formData.append('year', document.getElementById('seriesYear').value);
                formData.append('language', document.getElementById('seriesLanguage').value);
                if (seriesImageInput.files.length > 0) formData.append('image', seriesImageInput.files[0]);
                if (lastFetchedPosterUrl) formData.append('imageUrl', lastFetchedPosterUrl);
            }

            uploadDataWithProgress('/admin/add-series', formData, 'seriesForm', 'progressWrapperSeries', 'progressBarSeries', 'percentSeries', () => {
                fetchSeries();
            });
        }
    });

    function fetchSeries() {
        fetch('/admin/series')
            .then(res => res.json())
            .then(seriesList => {
                window.currentSeries = seriesList;
                updateDashboardStats(); // Update stats immediately after fetch

                const container = document.getElementById('seriesAccordion');
                const cardGrid = document.getElementById('seriesCardGrid');
                const searchFilter = document.getElementById('seriesSearchFilter');
                const selectedSeriesIdInput = document.getElementById('selectedSeriesId');

                container.innerHTML = '';
                cardGrid.innerHTML = '';

                if (seriesList.length === 0) {
                    container.innerHTML = '<div class="loading-state">Henüz hiç dizi yüklenmemiş.</div>';
                    return;
                }

                // Sort series by name
                seriesList.sort((a, b) => a.name.localeCompare(b.name, 'tr'));

                seriesList.forEach(series => {
                    // Populate Card Grid
                    const card = document.createElement('div');
                    card.className = 'series-card';
                    card.dataset.id = series.id;
                    card.dataset.name = series.name.toLowerCase();

                    // Correct poster path using MediaController endpoint
                    const posterPath = `/media/image/${series.id}`;

                    card.innerHTML = `
                        <img src="${posterPath}" class="series-card-poster" onerror="this.src='/images/placeholder.webp'">
                        <div class="series-card-info">
                            <div class="series-card-title">${series.name}</div>
                            <div class="series-card-meta">${series.year || '-'} â€¢ ${series.language || '-'}</div>
                        </div>
                    `;

                    card.onclick = () => {
                        document.querySelectorAll('.series-card').forEach(c => c.classList.remove('selected'));
                        card.classList.add('selected');
                        selectedSeriesIdInput.value = series.id;
                    };

                    cardGrid.appendChild(card);

                    // Populate Accordion
                    const episodes = parseEpisodesFromXML(series.xmlData, series.name, series.category, series.content, series.language, series.year);
                    const episodeCount = episodes.length;

                    const groupDiv = document.createElement('div');
                    groupDiv.className = 'series-group';

                    groupDiv.innerHTML = `
                        <div class="series-header" onclick="toggleAccordion(this)">
                            <div class="series-info" style="display: flex; align-items: center; gap: 10px;">
                                <input type="checkbox" class="series-checkbox" value="${series.id}" onclick="event.stopPropagation(); toggleSeriesSelection('${series.id}', this.checked)">
                                <h3>${series.name}</h3>
                                    ${series.category || 'Genel'} • ${series.language || '-'} • ${episodeCount} Bölüm
                                </div>
                            </div>
                            <div class="series-actions" onclick="event.stopPropagation()">
                                <button class="btn btn-sm btn-edit" onclick='editSeriesMetadata(${JSON.stringify(series).replace(/'/g, "&#39;")})'><i class="fas fa-edit"></i> DÜZENLE</button>
                                <button class="btn btn-sm btn-danger" onclick='deleteSeriesByName("${series.name}")'><i class="fas fa-trash"></i> SİL</button>
                            </div>
                        </div>
                        <div class="episodes-list">
                            ${episodes.length > 0 ? episodes.map(ep => `
                                <div class="episode-item">
                                    <div class="episode-title">
                                        <strong>Sezon ${ep.season} - Bölüm ${ep.episode}</strong>
                                    </div>
                                    <div class="episode-actions">
                                        <button class="btn btn-sm btn-edit" onclick='preloadEpisodeForm(${JSON.stringify(ep).replace(/'/g, "&#39;")})'><i class="fas fa-copy"></i> KOPYALA</button>
                                        <button class="btn btn-sm btn-danger" onclick='deleteEpisode("${ep.id}")'><i class="fas fa-trash"></i> SİL</button>
                                    </div>
                                </div>
                            `).join('') : '<div style="padding:15px; color:#666;">Bölüm bulunamadı.</div>'}
                        </div>
                    `;
                    container.appendChild(groupDiv);
                });
            })
            .catch(err => {
                console.error("Dizi listesi hatası:", err);
                document.getElementById('seriesAccordion').innerHTML = '<div class="loading-state" style="color:var(--danger);">Veriler alınırken bir hata oluştu.</div>';
            });

        // Add search filter logic
        const searchInput = document.getElementById('seriesSearchFilter');
        if (searchInput) {
            searchInput.oninput = function () {
                const query = this.value.toLowerCase().trim();
                const cards = document.querySelectorAll('.series-card');
                cards.forEach(card => {
                    const name = card.dataset.name;
                    if (name.includes(query)) {
                        card.classList.remove('hidden');
                    } else {
                        card.classList.add('hidden');
                    }
                });
            };
        }
    }

    window.toggleSeriesMode = function () {
        const mode = document.querySelector('input[name="seriesMode"]:checked').value;
        const existingWrapper = document.getElementById('existingSeriesSelectWrapper');
        const newFields = document.getElementById('newSeriesFields');

        if (mode === 'existing') {
            existingWrapper.style.display = 'block';
            newFields.style.display = 'none';
            // Refresh cards
            fetchSeries();
        } else {
            existingWrapper.style.display = 'none';
            newFields.style.display = 'block';
        }
    };

    function parseEpisodesFromXML(xmlString, seriesName, cat, summ, lang, year) {
        if (!xmlString) return [];
        try {
            const parser = new DOMParser();
            const xmlDoc = parser.parseFromString(xmlString, "text/xml");
            const seasons = Array.from(xmlDoc.getElementsByTagName("Season"));
            let allEpisodes = [];

            seasons.forEach(season => {
                const seasonNum = season.getAttribute("number");
                const eps = Array.from(season.getElementsByTagName("Episode"));
                eps.forEach(ep => {
                    allEpisodes.push({
                        id: ep.textContent,
                        season: seasonNum,
                        episode: ep.getAttribute("number"),
                        name: seriesName,
                        category: cat,
                        content: summ,
                        language: lang,
                        year: year
                    });
                });
            });

            return allEpisodes.sort((a, b) => {
                if (parseInt(a.season) === parseInt(b.season)) return parseInt(a.episode) - parseInt(b.episode);
                return parseInt(a.season) - parseInt(b.season);
            });
        } catch (e) {
            console.error("XML Parse Hatası:", e);
            return [];
        }
    }

    window.toggleAccordion = function (element) {
        const panel = element.nextElementSibling;
        const isActive = panel.classList.contains('active');

        // Close others
        document.querySelectorAll('.episodes-list').forEach(p => {
            p.classList.remove('active');
            p.style.maxHeight = null;
        });

        if (!isActive) {
            panel.classList.add('active');
            panel.style.maxHeight = "500px";
        }
    };

    window.editSeriesMetadata = function (series) {
        seriesIdInput.value = series.id;
        document.getElementById('seriesName').value = series.name;
        document.getElementById('seriesCategory').value = series.category;
        document.getElementById('seriesSummary').value = series.content || series._content;
        document.getElementById('seriesLanguage').value = series.language;

        setupSeriesEditMode("DİZİ BİLGİSİNİ GÜNCELLE");

        // Switch to series section
        const seriesLink = document.querySelector('.nav-link[data-section="series-section"]');
        if (seriesLink) seriesLink.click();
    };

    window.preloadEpisodeForm = function (ep) {
        document.getElementById('seriesName').value = ep.name;
        document.getElementById('seriesCategory').value = ep.category;
        document.getElementById('seriesSummary').value = ep.content;
        document.getElementById('seriesLanguage').value = ep.language;
        document.getElementById('seasonNum').value = ep.season;
        document.getElementById('episodeNum').value = ep.episode;

        alert("Bölüm bilgileri forma kopyalandı.");

        // Switch to series section
        const seriesLink = document.querySelector('.nav-link[data-section="series-section"]');
        if (seriesLink) seriesLink.click();
    };

    function setupSeriesEditMode(btnText) {
        seriesSubmitBtn.innerText = btnText;
        seriesSubmitBtn.classList.remove('btn-primary');
        seriesSubmitBtn.style.backgroundColor = "#ffc107";
        seriesSubmitBtn.style.color = "#000";
        seriesCancelBtn.style.display = "block";
        seriesFileInput.removeAttribute('required');
    }

    window.deleteEpisode = function (id) {
        if (!confirm("Bu bölümü silmek istediğinize emin misiniz?")) return;
        fetch('/admin/delete-episode?id=' + id, { method: 'DELETE' })
            .then(res => {
                if (res.ok) { fetchSeries(); }
                else alert("Hata oluştu.");
            });
    };

    window.deleteSeriesByName = function (name) {
        if (!confirm("'" + name + "' dizisine ait TÜM BÖLÜMLER silinecek. Emin misiniz?")) return;
        fetch('/admin/delete-series-by-name?name=' + encodeURIComponent(name), { method: 'DELETE' })
            .then(res => {
                if (res.ok) { fetchSeries(); }
                else alert("Silme işlemi başarısız oldu.");
            });
    };

    function updateSeries(data) {
        seriesSubmitBtn.innerText = "GÜNCELLENİYOR...";
        seriesSubmitBtn.disabled = true;
        fetch('/admin/update-series', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        }).then(res => res.text()).then(msg => {
            alert(msg); resetSeriesForm(); fetchSeries();
        }).catch(err => alert("Hata: " + err))
            .finally(() => { seriesSubmitBtn.disabled = false; });
    }

    seriesCancelBtn.addEventListener('click', resetSeriesForm);
    function resetSeriesForm() {
        seriesForm.reset();
        seriesIdInput.value = "";
        seriesSubmitBtn.innerText = "BÖLÜMÜ / DİZİYİ KAYDET";
        seriesSubmitBtn.classList.add('btn-primary');
        seriesSubmitBtn.style.backgroundColor = "";
        seriesSubmitBtn.style.color = "";
        seriesCancelBtn.style.display = "none";
        seriesFileInput.setAttribute('required', 'required');

        if (seriesImageInput) seriesImageInput.value = "";
        if (seriesPosterUrlInput) seriesPosterUrlInput.value = "";
        lastFetchedPosterUrl = "";
        updatePosterPreview('series', null);

        // Clear card selection
        document.querySelectorAll('.series-card').forEach(c => c.classList.remove('selected'));
        const selectedSeriesIdInput = document.getElementById('selectedSeriesId');
        if (selectedSeriesIdInput) selectedSeriesIdInput.value = "";
        const searchInput = document.getElementById('seriesSearchFilter');
        if (searchInput) searchInput.value = "";
    }

    /* ===========================================================
       ORTAK FONKSİYONLAR
       =========================================================== */
    function uploadDataWithProgress(url, formData, formId, wrapperId, barId, percentId, successCallback) {
        const xhr = new XMLHttpRequest();
        const wrapper = document.getElementById(wrapperId);
        const bar = document.getElementById(barId);
        const percentText = document.getElementById(percentId);
        const btn = document.querySelector(`#${formId} button[type="submit"]`);
        const originalBtnText = btn.innerText;

        const formElement = document.getElementById(formId);
        const fileInput = formElement.querySelector('input[type="file"]');

        if (fileInput && fileInput.files.length > 0) {
            const fileSizeMB = fileInput.files[0].size / (1024 * 1024);
            const currentHost = window.location.hostname;
            const currentPort = window.location.port;
            const isBackdoor = currentHost.includes('uploadozr9x0q3glr158beem49') || currentPort === '8443';

            if (fileSizeMB > 95 && !isBackdoor) {
                alert(
                    "âš ï¸ Cloudflare kalkanları 100MB üzerini engelliyor.\n" +
                    "Lütfen 'GİZLİ ARKA KAPI' adresine geçiş yapın:\n" +
                    "âž¡ï¸ https://uploadozr9x0q3glr158beem49.whodatidols.com:8443/admin/panel"
                );
                return;
            }
        }

        btn.disabled = true;
        btn.innerText = "YÜKLENİYOR...";
        wrapper.style.display = "block";
        bar.style.width = "0%";
        percentText.innerText = "0%";

        xhr.upload.addEventListener("progress", function (e) {
            if (e.lengthComputable) {
                const percentComplete = Math.round((e.loaded / e.total) * 100);
                bar.style.width = percentComplete + "%";
                percentText.innerText = percentComplete + "%";
                if (percentComplete > 99) {
                    percentText.innerText = "İşleniyor...";
                }
            }
        });

        xhr.addEventListener("load", function () {
            if (xhr.status === 200) {
                alert("İşlem Başarılı!\n" + xhr.responseText);
                document.getElementById(formId).reset();
                wrapper.style.display = "none";
                if (successCallback) successCallback();
            } else {
                alert("Hata Oluştu: " + xhr.statusText + "\n" + xhr.responseText);
            }
            btn.disabled = false;
            btn.innerText = originalBtnText;
        });

        xhr.addEventListener("error", function () {
            alert("Ağ Hatası!");
            btn.disabled = false;
            btn.innerText = originalBtnText;
        });

        xhr.open("POST", url);
        xhr.send(formData);
    }

    /* ===========================================================
       TVMaze AUTO-FETCH MANTIĞI
       =========================================================== */
    const tmdbModal = document.getElementById('tmdbModal');
    const tmdbResultsGrid = document.getElementById('tmdbResults');
    const closeModalBtn = document.querySelector('.close-modal');
    let currentFetchType = 'Movie';
    let lastFetchedPosterUrl = "";

    document.getElementById('fetchMovieBtn').addEventListener('click', () => {
        const query = document.getElementById('movieName').value.trim();
        if (!query) return alert("Lütfen aramak için bir film adı girin!");
        currentFetchType = 'Movie';
        searchTvMaze(query, 'Movie');
    });

    document.getElementById('fetchSeriesBtn').addEventListener('click', () => {
        const query = document.getElementById('seriesName').value.trim();
        if (!query) return alert("Lütfen aramak için bir dizi adı girin!");
        currentFetchType = 'Series';
        searchTvMaze(query, 'Series');
    });

    async function searchTvMaze(query, type) {
        console.log("Fetching TVMaze for:", query, "at URL:", `/public/api/tvmaze/search?query=${encodeURIComponent(query)}`);
        tmdbResultsGrid.innerHTML = '<div class="loading-state">TVMaze taranıyor...</div>';
        tmdbModal.style.display = 'block';

        try {
            const fetchUrl = `/public/api/tvmaze/search?query=${encodeURIComponent(query)}`;
            const response = await fetch(fetchUrl);
            console.log("TVMaze Response Status:", response.status);
            if (!response.ok) {
                const errorText = await response.text();
                console.error("TVMaze Response Error Body:", errorText);
                throw new Error(errorText || "Sunucu hatası: " + response.status);
            }
            const data = await response.json();
            console.log("TVMaze Data Received:", data);

            renderTvMazeResults(data, type);
        } catch (error) {
            console.error("TVMaze Search Fetch Exception:", error);
            tmdbResultsGrid.innerHTML = `<div class="loading-state" style="color:var(--danger);">Hata: Veri alınamadı (${error.message})</div>`;
        }
    }

    function renderTvMazeResults(results, type) {
        tmdbResultsGrid.innerHTML = '';
        if (!results || results.length === 0) {
            tmdbResultsGrid.innerHTML = '<div class="loading-state">Sonuç bulunamadı.</div>';
            return;
        }

        results.slice(0, 12).forEach(result => {
            const item = result.show;
            const div = document.createElement('div');
            div.className = 'tmdb-item';
            const title = item.name;
            const year = item.premiered ? item.premiered.substring(0, 4) : "-";
            const poster = item.image ? item.image.medium : 'https://via.placeholder.com/210x295?text=No+Poster';

            div.innerHTML = `
                <img src="${poster}" class="tmdb-poster" alt="${title}">
                <div class="tmdb-info">
                    <div class="tmdb-title">${title}</div>
                    <div class="tmdb-meta">${year}</div>
                </div>
            `;
            div.onclick = () => fetchTvMazeDetails(item.id, type);
            tmdbResultsGrid.appendChild(div);
        });
    }

    async function fetchTvMazeDetails(id, type) {
        tmdbResultsGrid.innerHTML = '<div class="loading-state">Detaylar getiriliyor...</div>';
        try {
            const response = await fetch(`/public/api/tvmaze/details?id=${id}`);
            const data = await response.json();
            populateFormWithTvMazeData(data, type);
            tmdbModal.style.display = 'none';
        } catch (error) {
            alert("Detaylar alınırken hata oluştu.");
        }
    }

    function populateFormWithTvMazeData(data, type) {
        // Store poster URL for submission
        lastFetchedPosterUrl = data.image ? data.image.original : "";

        // TVMaze summary comes with HTML tags, let's strip them
        const tempDiv = document.createElement("div");
        tempDiv.innerHTML = data.summary || "";
        const cleanSummary = tempDiv.textContent || tempDiv.innerText || "";

        if (type === 'Movie') {
            document.getElementById('movieName').value = data.name;
            document.getElementById('movieYear').value = data.premiered ? data.premiered.substring(0, 4) : "2025";
            document.getElementById('movieSummary').value = cleanSummary;
            document.getElementById('movieCategory').value = data.genres ? data.genres.join(', ') : "";
            document.getElementById('movieImageUrl').value = lastFetchedPosterUrl;
            updatePosterPreview('movie', lastFetchedPosterUrl);

            // Language matching
            const langMap = { 'Korean': 'Korece', 'English': 'İngilizce', 'Japanese': 'Japonca', 'Turkish': 'Türkçe' };
            document.getElementById('movieLanguage').value = langMap[data.language] || 'İngilizce';
        } else {
            document.getElementById('seriesName').value = data.name;
            document.getElementById('seriesYear').value = data.premiered ? data.premiered.substring(0, 4) : "2025";
            document.getElementById('seriesSummary').value = cleanSummary;
            document.getElementById('seriesCategory').value = data.genres ? data.genres.join(', ') : "";
            document.getElementById('seriesImageUrl').value = lastFetchedPosterUrl;
            updatePosterPreview('series', lastFetchedPosterUrl);

            const langMap = { 'Korean': 'Korece', 'Turkish': 'Türkçe', 'English': 'İngilizce', 'Japanese': 'Japonca' };
            document.getElementById('seriesLanguage').value = langMap[data.language] || 'Korece';
        }
    }

    closeModalBtn.onclick = () => { tmdbModal.style.display = 'none'; };
    window.onclick = (e) => { if (e.target == tmdbModal) tmdbModal.style.display = 'none'; };

    /**
     * DASHBOARD STATS CALCULATOR
     * Calculates totals from window.currentMovies and window.currentSeries
     */
    function updateDashboardStats() {
        // Movies
        const movieCount = window.currentMovies ? window.currentMovies.length : 0;
        const movieEl = document.getElementById('statTotalMovies');
        if (movieEl) movieEl.innerText = movieCount;

        // Series
        const seriesCount = window.currentSeries ? window.currentSeries.length : 0;
        const seriesEl = document.getElementById('statTotalSeries');
        if (seriesEl) seriesEl.innerText = seriesCount;

        // Episodes
        let episodeCount = 0;
        if (window.currentSeries) {
            window.currentSeries.forEach(s => {
                // Parse XML to count episodes
                const episodes = parseEpisodesFromXML(s.xmlData, "", "", "", "", "");
                episodeCount += episodes.length;
            });
        }
        const epEl = document.getElementById('statTotalEpisodes');
        if (epEl) epEl.innerText = episodeCount;
    }
});
