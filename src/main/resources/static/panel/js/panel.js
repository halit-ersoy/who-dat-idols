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

        preview.innerHTML = `<img src="${urlOrSource}" alt="Poster Önizleme" onerror="this.onerror=null; this.outerHTML='<i class=\'fas fa-image\'></i>'">`;
    }

    // Sayfa açılınca mevcut listeleri çek
    fetchHeroVideos();
    fetchMovies();
    fetchSeries();
    fetchViewStats();

    // Role-based UI initialization
    window.currentAdmin = null;
    fetch('/admin/me')
        .then(res => res.json())
        .then(user => {
            window.currentAdmin = user;
            const isSuperAdmin = user.roles.includes('ROLE_SUPER_ADMIN');

            // Hide user management link if not super admin
            if (!isSuperAdmin) {
                const userNavLink = document.querySelector('.nav-link[data-section="user-section"]');
                if (userNavLink) userNavLink.style.display = 'none';
            } else {
                fetchUsers();
            }

            // Update welcome message
            const welcomeH1 = document.querySelector('.welcome h1');
            if (welcomeH1) welcomeH1.innerText = `Hoş Geldin, ${user.username} 👋`;
        })
        .catch(err => console.error("Admin info error:", err));

    // Archive Search for Movies
    const movieSearchInput = document.getElementById('movieArchiveSearch');
    if (movieSearchInput) {
        movieSearchInput.addEventListener('input', function () {
            const query = this.value.toLowerCase().trim();
            const rows = document.querySelectorAll('#movieTable tbody tr');
            rows.forEach(row => {
                const name = row.cells[1].innerText.toLowerCase();
                row.style.display = name.includes(query) ? '' : 'none';
            });
        });
    }

    // Archive Search for Series
    const seriesArchiveSearchInput = document.getElementById('seriesArchiveSearch');
    if (seriesArchiveSearchInput) {
        seriesArchiveSearchInput.addEventListener('input', function () {
            const query = this.value.toLowerCase().trim();
            const groups = document.querySelectorAll('#seriesAccordion .series-group');
            groups.forEach(group => {
                const h3 = group.querySelector('h3');
                if (h3) {
                    const name = h3.innerText.toLowerCase();
                    group.style.display = name.includes(query) ? '' : 'none';
                }
            });
        });
    }

    /* ===========================================================
       CONTENT FILTER SWITCH (ARCHIVE)
       =========================================================== */
    const filterRadios = document.querySelectorAll('input[name="archiveFilter"]');
    const heroCard = document.getElementById('heroArchiveCard');
    const movieCard = document.getElementById('movieArchiveCard');
    const seriesCard = document.getElementById('seriesArchiveCard');

    if (filterRadios.length > 0 && heroCard && movieCard && seriesCard) {
        // Function to update visibility
        function updateArchiveView(selectedVal) {
            // Hide all
            heroCard.style.display = 'none';
            movieCard.style.display = 'none';
            seriesCard.style.display = 'none';

            // Show selected
            if (selectedVal === 'hero') {
                heroCard.style.display = 'block';
                // Trigger slider animation adjustment if needed
            } else if (selectedVal === 'movies') {
                movieCard.style.display = 'block';
            } else if (selectedVal === 'series') {
                seriesCard.style.display = 'block';
            }
        }

        // Add event listeners
        filterRadios.forEach(radio => {
            radio.addEventListener('change', function () {
                if (this.checked) {
                    updateArchiveView(this.value);
                }
            });
        });

        // Initialize view based on checked input
        const checkedRadio = document.querySelector('input[name="archiveFilter"]:checked');
        if (checkedRadio) {
            updateArchiveView(checkedRadio.value);
        }
    }

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
       TAKVİM YÖNETİMİ
       =========================================================== */
    const calendarForm = document.getElementById('calendarForm');

    if (calendarForm) {
        fetchCalendarEvents();

        calendarForm.addEventListener('submit', function (e) {
            e.preventDefault();
            const data = {
                dayOfWeek: document.getElementById('calendarDay').value,
                title: document.getElementById('calendarTitle').value,
                episode: document.getElementById('calendarEpisode').value,
                showTime: document.getElementById('calendarTime').value,
                sortOrder: 0
            };

            const btn = document.getElementById('addEventBtn');
            const originalText = btn.innerHTML;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> EKLENİYOR...';
            btn.disabled = true;

            fetch('/admin/calendar/add', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            }).then(res => {
                if (res.ok) {
                    calendarForm.reset();
                    fetchCalendarEvents();
                } else {
                    res.text().then(msg => alert("Hata: " + msg));
                }
            }).catch(err => alert("Hata: " + err))
                .finally(() => {
                    btn.innerHTML = originalText;
                    btn.disabled = false;
                });
        });
    }

    function fetchCalendarEvents() {
        fetch('/api/calendar')
            .then(res => res.json())
            .then(data => {
                const tbody = document.querySelector('#calendarTable tbody');
                if (!tbody) return;
                tbody.innerHTML = '';

                const daysOrder = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday'];
                const dayNames = {
                    'monday': 'Pazartesi', 'tuesday': 'Salı', 'wednesday': 'Çarşamba',
                    'thursday': 'Perşembe', 'friday': 'Cuma', 'saturday': 'Cumartesi', 'sunday': 'Pazar'
                };

                daysOrder.forEach(day => {
                    if (data[day] && data[day].length > 0) {
                        data[day].forEach(event => {
                            const tr = document.createElement('tr');
                            tr.innerHTML = `
                                <td>${dayNames[day]}</td>
                                <td>${event.time}</td>
                                <td style="font-weight: 600;">${event.title}</td>
                                <td>${event.episode}</td>
                                <td>
                                    <button class="btn btn-sm btn-danger" onclick='deleteCalendarEvent("${event.id}")'><i class="fas fa-trash"></i> SİL</button>
                                </td>
                            `;
                            tbody.appendChild(tr);
                        });
                    }
                });
            })
            .catch(err => console.error("Takvim verisi alınamadı:", err));
    }

    window.deleteCalendarEvent = function (id) {
        if (!confirm("Bu etkinliği silmek istediğinize emin misiniz?")) return;

        fetch(`/admin/calendar/delete?id=${id}`, { method: 'DELETE' })
            .then(res => {
                if (res.ok) fetchCalendarEvents();
                else res.text().then(msg => alert("Hata: " + msg));
            });
    };

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
    const heroContentInput = document.getElementById('heroContent'); // Textarea

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

                const isMovie = type && type.toLowerCase() === 'movie';

                div.innerHTML = `
                    <div class="result-info">
                        <span style="font-weight: 600;">${name}</span>
                        <span class="item-type">${isMovie ? 'Film' : 'Dizi'}</span>
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

        const isMovie = item.type && item.type.toLowerCase() === 'movie';

        selectedContentDisplay.querySelector('strong').innerText = item.name + ' (' + (isMovie ? 'Film' : 'Dizi') + ')';
        selectedContentDisplay.style.display = 'flex';
        heroSearchResults.style.display = 'none';
        heroSearchInput.value = '';

        // Pre-fill content if possible (Optional but good UX)
        // We can fetch the details to get the summary
        // For now, let's just leave it empty or clear it, user wants custom text
        // Or if you want to pre-fill:
        // fetchDetailsAndFill(item.id, item.type);
    }

    document.querySelector('.btn-clear').onclick = () => {
        selectedContentIdInput.value = '';
        selectedContentTypeInput.value = '';
        selectedContentDisplay.style.display = 'none';
        heroContentInput.value = '';
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
        formData.append('content', heroContentInput.value);
        formData.append('file', file);

        uploadDataWithProgress('/admin/add-hero', formData, 'heroForm', 'progressWrapperHero', 'progressBarHero', 'percentHero', () => {
            fetchHeroVideos();
            selectedContentDisplay.style.display = 'none';
            selectedContentIdInput.value = '';
            selectedContentTypeInput.value = '';
            heroContentInput.value = '';
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

                    const isFirst = index === 0;
                    const isLast = index === heroes.length - 1;

                    tr.innerHTML = `
                        <td>
                            <div class="order-controls">
                                <span class="order-index">${index + 1}</span>
                                <button class="btn-order" onclick="moveHero(${index}, -1)" title="Yukarı Taşı" style="${isFirst ? 'visibility:hidden' : ''}">
                                    <i class="fas fa-chevron-up"></i>
                                </button>
                                <button class="btn-order" onclick="moveHero(${index}, 1)" title="Aşağı Taşı" style="${isLast ? 'visibility:hidden' : ''}">
                                    <i class="fas fa-chevron-down"></i>
                                </button>
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

            const isFirst = index === 0;
            const isLast = index === heroes.length - 1;

            tr.innerHTML = `
                <td>
                    <div class="order-controls">
                        <span class="order-index">${index + 1}</span>
                        <button class="btn-order" onclick="moveHero(${index}, -1)" title="Yukarı Taşı" style="${isFirst ? 'visibility:hidden' : ''}">
                             <i class="fas fa-chevron-up"></i>
                        </button>
                        <button class="btn-order" onclick="moveHero(${index}, 1)" title="Aşağı Taşı" style="${isLast ? 'visibility:hidden' : ''}">
                             <i class="fas fa-chevron-down"></i>
                        </button>
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
            formData.append('summary', document.getElementById('movieSummary').value);
            formData.append('releaseYear', document.getElementById('movieYear').value);
            formData.append('language', document.getElementById('movieLanguage').value);
            formData.append('country', document.getElementById('movieCountry').value);

            if (movieImageInput.files.length > 0) {
                formData.append('image', movieImageInput.files[0]);
            }
            if (lastFetchedPosterUrl) {
                formData.append('imageUrl', lastFetchedPosterUrl);
            }
            if (movieFileInput.files.length > 0) {
                formData.append('file', movieFileInput.files[0]);
            }

            movieSubmitBtn.innerText = "GÜNCELLENİYOR...";
            movieSubmitBtn.disabled = true;

            fetch('/admin/update-movie', {
                method: 'POST',
                // Remove Content-Type header to allow browser to set boundary for FormData
                body: formData
            }).then(res => res.json()).then(async data => {
                const id = data.id || movieId;
                await saveExternalSources(id, 'movie');
                alert(data.message || "Film güncellendi.");
                resetMovieForm(); fetchMovies();
            }).finally(() => { movieSubmitBtn.disabled = false; });
        } else {
            const formData = new FormData();
            formData.append('name', document.getElementById('movieName').value);
            formData.append('category', document.getElementById('movieCategory').value);
            formData.append('summary', document.getElementById('movieSummary').value);
            formData.append('releaseYear', document.getElementById('movieYear').value);
            formData.append('language', document.getElementById('movieLanguage').value);
            formData.append('country', document.getElementById('movieCountry').value);
            if (movieFileInput.files.length > 0) formData.append('file', movieFileInput.files[0]);
            if (movieImageInput.files.length > 0) formData.append('image', movieImageInput.files[0]);
            if (lastFetchedPosterUrl) formData.append('imageUrl', lastFetchedPosterUrl);

            uploadDataWithProgress('/admin/add-movie', formData, 'movieForm', 'progressWrapperMovie', 'progressBarMovie', 'percentMovie', async (res) => {
                try {
                    const data = JSON.parse(res);
                    if (data.id) await saveExternalSources(data.id, 'movie');
                } catch (e) { console.error("Source save error:", e); }
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
                        <td>${movie.releaseYear}</td>
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
        document.getElementById('movieSummary').value = movie.summary;
        document.getElementById('movieYear').value = movie.releaseYear;

        // Country population
        document.getElementById('movieCountry').value = movie.country || 'kr';

        // Language matching (defensive)
        const languageSelect = document.getElementById('movieLanguage');
        const incomingLang = movie.language ? movie.language.trim() : "";

        let found = false;
        for (let i = 0; i < languageSelect.options.length; i++) {
            if (languageSelect.options[i].value === incomingLang) {
                languageSelect.selectedIndex = i;
                found = true;
                break;
            }
        }
        if (!found) languageSelect.value = "Türkçe"; // Default if not found

        // Update Poster Preview
        updatePosterPreview('movie', `/media/image/${movie.id}?t=${Date.now()}`);

        movieSubmitBtn.innerText = "DEĞİŞİKLİKLERİ KAYDET";
        movieSubmitBtn.classList.remove('btn-primary');
        movieSubmitBtn.style.backgroundColor = "#ffc107";
        movieSubmitBtn.style.color = "#000";
        movieCancelBtn.style.display = "block";
        movieFileInput.removeAttribute('required');

        // Switch to movie section
        const movieLink = document.querySelector('.nav-link[data-section="movie-section"]');
        if (movieLink) movieLink.click();

        // Load External Sources
        document.getElementById('movieSourcesList').innerHTML = '';
        fetch(`/media/video/${movie.id}/sources`)
            .then(res => res.json())
            .then(sources => {
                sources.forEach(src => addSourceField('movie', src.sourceName, src.sourceUrl));
            });
    };



    function resetMovieForm() {
        movieForm.reset();
        movieIdInput.value = "";
        document.getElementById('movieSourcesList').innerHTML = '';
        movieSubmitBtn.innerText = "FİLMİ KAYDET";
        movieSubmitBtn.classList.add('btn-primary');
        movieSubmitBtn.style.backgroundColor = "";
        movieSubmitBtn.style.color = "";
        movieCancelBtn.style.display = "none";
        movieFileInput.setAttribute('required', 'required');

        if (movieImageInput) movieImageInput.value = "";
        if (movieImageInput) movieImageInput.value = "";
        if (moviePosterUrlInput) moviePosterUrlInput.value = "";
        document.getElementById('movieCountry').value = 'kr';
        lastFetchedPosterUrl = "";
        updatePosterPreview('movie', null);
        resetFileInputs();
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
    const episodeIdInput = document.getElementById('episodeId');
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
        const epId = episodeIdInput.value;
        const mode = document.querySelector('input[name="seriesMode"]:checked').value;

        // Form Mode Handling
        if (sId !== "" && epId === "") {
            // SERIES METADATA EDIT MODE
            const formData = new FormData();
            formData.append('id', sId);
            formData.append('name', document.getElementById('seriesName').value);
            formData.append('category', document.getElementById('seriesCategory').value);
            formData.append('summary', document.getElementById('seriesSummary').value);
            formData.append('language', document.getElementById('seriesLanguage').value);
            formData.append('country', document.getElementById('seriesCountry').value);
            formData.append('seriesType', document.getElementById('seriesType').value);

            if (seriesImageInput.files.length > 0) {
                formData.append('image', seriesImageInput.files[0]);
            }
            if (lastFetchedPosterUrl) {
                formData.append('imageUrl', lastFetchedPosterUrl);
            }

            seriesSubmitBtn.innerText = "GÜNCELLENİYOR...";
            seriesSubmitBtn.disabled = true;

            fetch('/admin/update-series', {
                method: 'POST',
                body: formData
            }).then(res => {
                if (!res.ok) {
                    if (res.status === 302 || res.status === 0) {
                        throw new Error("Oturum süresi dolmuş olabilir veya yetki hatası (302). Lütfen sayfayı yenileyip tekrar giriş yapın.");
                    }
                    return res.text().then(text => { throw new Error(`Sunucu Hatası (${res.status}): ${text}`); });
                }
                return res.json();
            }).then(data => {
                alert(data.message || "Dizi bilgileri güncellendi.");
                resetSeriesForm();
                fetchSeries();
            }).catch(err => {
                console.error("Update Series Error:", err);
                alert("Hata: " + err.message);
            }).finally(() => { seriesSubmitBtn.disabled = false; });
            return;
        }

        if (epId !== "") {
            // EPISODE EDIT MODE
            const formData = new FormData();
            formData.append('episodeId', epId);
            formData.append('season', document.getElementById('seasonNum').value);
            formData.append('episodeNum', document.getElementById('episodeNum').value);

            if (seriesFileInput.files.length > 0) {
                formData.append('file', seriesFileInput.files[0]);
            }

            seriesSubmitBtn.innerText = "BÖLÜM GÜNCELLENİYOR...";
            seriesSubmitBtn.disabled = true;

            uploadDataWithProgress('/admin/update-episode', formData, 'seriesForm', 'progressWrapperSeries', 'progressBarSeries', 'percentSeries', async (res) => {
                await saveExternalSources(epId, 'series');
                resetSeriesForm();
                fetchSeries();
            });
            return;
        }

        // ADD NEW SERIES or ADD TO EXISTING SERIES MODE
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

        const formData = new FormData();
        const existingId = document.getElementById('selectedSeriesId').value;

        // Episode specific fields
        formData.append('season', document.getElementById('seasonNum').value);
        formData.append('episode', document.getElementById('episodeNum').value);
        if (seriesFileInput.files.length > 0) formData.append('file', seriesFileInput.files[0]);

        if (mode === 'existing') {
            if (!existingId) return alert("Lütfen bir dizi seçin!");
            formData.append('existingSeriesId', existingId);
        } else {
            // New Series Mode
            formData.append('name', document.getElementById('seriesName').value);
            formData.append('category', document.getElementById('seriesCategory').value);
            formData.append('summary', document.getElementById('seriesSummary').value);
            formData.append('year', document.getElementById('seriesYear').value);
            formData.append('language', document.getElementById('seriesLanguage').value);
            formData.append('country', document.getElementById('seriesCountry').value);
            formData.append('seriesType', document.getElementById('seriesType').value);
            if (seriesImageInput.files.length > 0) formData.append('image', seriesImageInput.files[0]);
            if (lastFetchedPosterUrl) formData.append('imageUrl', lastFetchedPosterUrl);
        }

        uploadDataWithProgress('/admin/add-series', formData, 'seriesForm', 'progressWrapperSeries', 'progressBarSeries', 'percentSeries', async (res) => {
            try {
                const data = JSON.parse(res);
                if (data.id) await saveExternalSources(data.id, 'series');
            } catch (e) { console.error("Source save error:", e); }
            fetchSeries();
        });
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
                seriesList.sort((a, b) => (a.name || "").localeCompare(b.name || "", 'tr'));

                seriesList.forEach(series => {
                    // Populate Card Grid
                    const card = document.createElement('div');
                    card.className = 'series-card';
                    card.dataset.id = series.id;
                    card.dataset.name = (series.name || "").toLowerCase();

                    // Correct poster path using MediaController endpoint
                    const posterPath = `/media/image/${series.id}`;

                    card.innerHTML = `
                        <img src="${posterPath}" class="series-card-poster" onerror="this.onerror=null; this.src='/images/placeholder.webp'">
                        <div class="series-card-info">
                            <div class="series-card-title">${series.name}</div>
                            <div class="series-card-meta">
                                <span><i class="fas fa-calendar-alt"></i> ${series.year || '-'}</span>
                                &bull;
                                <span><i class="fas fa-language"></i> ${series.language || '-'}</span>
                            </div>
                        </div>
                    `;

                    card.onclick = () => {
                        document.querySelectorAll('.series-card').forEach(c => c.classList.remove('selected'));
                        card.classList.add('selected');
                        selectedSeriesIdInput.value = series.id;
                        updatePosterPreview('series', posterPath);
                    };

                    cardGrid.appendChild(card);

                    // Populate Accordion
                    // TODO: Fetch episodes via API instead of XML parsing for the list
                    const episodes = parseEpisodesFromXML(series.episodeMetadataXml, series.name, series.category, series.summary, series.language, series.year);
                    const episodeCount = series.episodeCount || 0;

                    const groupDiv = document.createElement('div');
                    groupDiv.className = 'series-group';

                    groupDiv.innerHTML = `
                        <div class="series-header" onclick="toggleAccordion(this)">
                            <div class="series-info" style="display: flex; align-items: center; gap: 15px;">
                                <input type="checkbox" class="series-checkbox" value="${series.id}" onclick="event.stopPropagation(); toggleSeriesSelection('${series.id}', this.checked)">
                                <div class="series-title-wrapper">
                                    <h3>${series.name}</h3>
                                    <div class="series-meta-info">
                                        <span class="meta-item"><i class="fas fa-tags"></i> ${series.category || 'Genel'}</span>
                                        <span class="meta-item"><i class="fas fa-globe"></i> ${series.language || '-'}</span>
                                        <span class="meta-item"><i class="fas fa-video"></i> ${episodeCount} Bölüm</span>
                                    </div>
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
                                        <button class="btn btn-sm btn-edit" onclick='editEpisode(${JSON.stringify(ep).replace(/'/g, "&#39;")})'><i class="fas fa-edit"></i> DÜZENLE</button>
                                        <button class="btn btn-sm btn-edit" style="background:#6c757d;" onclick='preloadEpisodeForm(${JSON.stringify(ep).replace(/'/g, "&#39;")})'><i class="fas fa-copy"></i> KOPYALA</button>
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
                // More detailed error for debugging
                document.getElementById('seriesAccordion').innerHTML = `<div class="loading-state" style="color:var(--danger);">Veriler alınırken bir hata oluştu: ${err.message}</div>`;
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
            newFields.style.display = 'grid';
            updatePosterPreview('series', null);
        }
    };

    window.toggleUpcomingCategory = function () {
        const category = document.querySelector('input[name="upcomingCategory"]:checked').value;
        const statusGroup = document.getElementById('upcomingStatusGroup');
        const statusInput = document.getElementById('upcomingStatus');

        if (category === 'Upcoming') {
            statusGroup.style.display = 'none';
            statusInput.required = false;
            statusInput.value = ""; // Clear if hidden
        } else {
            statusGroup.style.display = 'block';
            statusInput.required = true;
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
        resetSeriesForm();
        seriesIdInput.value = series.id;
        document.getElementById('seriesName').value = series.name;
        document.getElementById('seriesCategory').value = series.category;
        document.getElementById('seriesSummary').value = series.summary || series.content || series._content;

        // Country population
        document.getElementById('seriesCountry').value = series.country || 'kr';

        // Series Type population
        document.getElementById('seriesType').value = series.seriesType || 'Dizi';

        // Language matching
        const languageSelect = document.getElementById('seriesLanguage');
        const incomingLang = series.language ? series.language.trim() : "";

        let found = false;
        for (let i = 0; i < languageSelect.options.length; i++) {
            if (languageSelect.options[i].value === incomingLang) {
                languageSelect.selectedIndex = i;
                found = true;
                break;
            }
        }
        if (!found) languageSelect.value = "Korece";

        updatePosterPreview('series', `/media/image/${series.id}?t=${Date.now()}`);

        // Form Layout for Series Edit
        document.getElementById('seriesModeGroup').style.display = 'none';
        document.getElementById('newSeriesFields').style.display = 'grid';
        document.getElementById('episodeFields').style.display = 'none';
        document.getElementById('seriesPosterGroup').style.display = 'block';

        document.getElementById('seriesFormStatus').style.display = 'block';
        document.getElementById('seriesFormModeText').innerText = "Dizi Bilgilerini Düzenle";

        setupSeriesEditMode("DİZİ BİLGİSİNİ GÜNCELLE");

        const seriesLink = document.querySelector('.nav-link[data-section="series-section"]');
        if (seriesLink) seriesLink.click();
    };

    window.editEpisode = function (ep) {
        resetSeriesForm();
        episodeIdInput.value = ep.id;
        document.getElementById('seasonNum').value = ep.season;
        document.getElementById('episodeNum').value = ep.episode;

        // Form Layout for Episode Edit
        document.getElementById('seriesModeGroup').style.display = 'none';
        document.getElementById('newSeriesFields').style.display = 'none';
        document.getElementById('episodeFields').style.display = 'grid';
        document.getElementById('seriesPosterGroup').style.display = 'none';

        document.getElementById('seriesFormStatus').style.display = 'block';
        document.getElementById('seriesFormModeText').innerText = `Bölüm Düzenle (${ep.name})`;

        setupSeriesEditMode("BÖLÜMÜ GÜNCELLE");

        // Load External Sources
        document.getElementById('seriesSourcesList').innerHTML = '';
        if (ep.id) {
            fetch(`/media/video/${ep.id}/sources`)
                .then(res => res.json())
                .then(sources => {
                    sources.forEach(src => addSourceField('series', src.sourceName, src.sourceUrl));
                });
        }

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

        // Load External Sources
        document.getElementById('seriesSourcesList').innerHTML = '';
        if (ep.id) {
            fetch(`/media/video/${ep.id}/sources`)
                .then(res => res.json())
                .then(sources => {
                    sources.forEach(src => addSourceField('series', src.sourceName, src.sourceUrl));
                });
        }
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


    seriesCancelBtn.addEventListener('click', resetSeriesForm);
    function resetSeriesForm() {
        seriesForm.reset();
        seriesIdInput.value = "";
        episodeIdInput.value = "";

        document.getElementById('seriesFormStatus').style.display = 'none';
        document.getElementById('seriesFormModeText').innerText = "-";

        seriesSubmitBtn.innerText = "BÖLÜMÜ / DİZİYİ KAYDET";
        seriesSubmitBtn.classList.add('btn-primary');
        seriesSubmitBtn.style.backgroundColor = "";
        seriesSubmitBtn.style.color = "";
        seriesCancelBtn.style.display = "none";
        seriesFileInput.setAttribute('required', 'required');

        // Reset visibility to default (New mode)
        document.getElementById('seriesModeGroup').style.display = 'block';
        document.getElementById('newSeriesFields').style.display = 'grid';
        document.getElementById('episodeFields').style.display = 'grid';
        document.getElementById('seriesPosterGroup').style.display = 'block';
        document.getElementById('existingSeriesSelectWrapper').style.display = 'none';
        document.getElementById('modeNew').checked = true;

        if (seriesImageInput) seriesImageInput.value = "";
        if (seriesImageInput) seriesImageInput.value = "";
        if (seriesPosterUrlInput) seriesPosterUrlInput.value = "";
        document.getElementById('seriesCountry').value = 'kr';
        document.getElementById('seriesType').value = 'Dizi';
        lastFetchedPosterUrl = "";
        updatePosterPreview('series', null);

        // Clear card selection
        document.querySelectorAll('.series-card').forEach(c => c.classList.remove('selected'));
        const selectedSeriesIdInput = document.getElementById('selectedSeriesId');
        if (selectedSeriesIdInput) selectedSeriesIdInput.value = "";
        const searchInput = document.getElementById('seriesSearchFilter');
        if (searchInput) searchInput.value = "";

        // Clear sources
        document.getElementById('seriesSourcesList').innerHTML = '';

        resetFileInputs();
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

        xhr.addEventListener("load", async function () {
            if (xhr.status === 200) {
                alert("İşlem Başarılı!\n" + xhr.responseText);

                // CRITICAL FIX: Run callback (save sources) BEFORE resetting the form!
                if (successCallback) {
                    await successCallback(xhr.responseText);
                }

                document.getElementById(formId).reset();
                wrapper.style.display = "none";
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

    window.translateText = function (elementId) {
        const textarea = document.getElementById(elementId);
        const text = textarea.value.trim();
        const btn = textarea.parentElement.querySelector('.btn-translate') || document.querySelector(`button[onclick="translateText('${elementId}')"]`);

        if (!text) {
            alert("Lütfen önce çevrilecek bir metin girin veya TVMaze'den getirin.");
            return;
        }

        const originalBtnHtml = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Çevriliyor...';

        fetch('/admin/translate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `text=${encodeURIComponent(text)}`
        })
            .then(res => res.text())
            .then(translatedText => {
                if (translatedText.startsWith("Çeviri hatası:")) {
                    alert(translatedText);
                } else {
                    textarea.value = translatedText;
                }
            })
            .catch(err => alert("Hata: " + err))
            .finally(() => {
                btn.disabled = false;
                btn.innerHTML = originalBtnHtml;
            });
    };

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

    const fetchUpcomingTvMazeBtn = document.getElementById('fetchUpcomingTvMazeBtn');
    if (fetchUpcomingTvMazeBtn) {
        fetchUpcomingTvMazeBtn.addEventListener('click', () => {
            const query = document.getElementById('upcomingName').value.trim();
            if (!query) return alert("Lütfen aramak için bir show adı girin!");
            currentFetchType = 'Upcoming';
            searchTvMaze(query, 'Upcoming');
        });
    }

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
        } else if (type === 'Series') {
            document.getElementById('seriesName').value = data.name;
            document.getElementById('seriesYear').value = data.premiered ? data.premiered.substring(0, 4) : "2025";
            document.getElementById('seriesSummary').value = cleanSummary;
            document.getElementById('seriesCategory').value = data.genres ? data.genres.join(', ') : "";
            document.getElementById('seriesImageUrl').value = lastFetchedPosterUrl;
            document.getElementById('seriesType').value = 'Dizi'; // Default
            updatePosterPreview('series', lastFetchedPosterUrl);

            const langMap = { 'Korean': 'Korece', 'Turkish': 'Türkçe', 'English': 'İngilizce', 'Japanese': 'Japonca' };
            document.getElementById('seriesLanguage').value = langMap[data.language] || 'Korece';
        } else if (type === 'Upcoming') {
            document.getElementById('upcomingName').value = data.name;
            document.getElementById('upcomingImageUrl').value = lastFetchedPosterUrl;
            updatePosterPreview('upcoming', lastFetchedPosterUrl);
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
                episodeCount += (s.episodeCount || 0);
            });
            const epEl = document.getElementById('statTotalEpisodes');
            if (epEl) epEl.innerText = episodeCount;
        }
    }

    /* ===========================================================
       PREMIUM DOSYA YÜKLEME MANTIĞI
       =========================================================== */
    window.handleFileSelect = function (input) {
        const container = input.closest('.premium-file-input');
        const nameSpan = container.querySelector('.file-name');

        if (input.files && input.files[0]) {
            const file = input.files[0];
            nameSpan.innerText = file.name;
            container.classList.add('has-file');

            // Boyut kontrolü uyarısı (opsiyonel görsel feedback)
            const sizeMB = file.size / (1024 * 1024);
            if (sizeMB > 95) {
                nameSpan.innerHTML = `<i class="fas fa-exclamation-triangle"></i> ${file.name} (Büyük Dosya)`;
                nameSpan.style.color = "var(--danger)";
            } else {
                nameSpan.style.color = "";
            }
        } else {
            nameSpan.innerText = "Dosya seçilmedi";
            container.classList.remove('has-file');
        }
    };

    function resetFileInputs() {
        document.querySelectorAll('.premium-file-input').forEach(container => {
            const input = container.querySelector('input[type="file"]');
            const nameSpan = container.querySelector('.file-name');
            if (input) input.value = '';
            if (nameSpan) nameSpan.innerText = "Dosya seçilmedi";
            container.classList.remove('has-file');
        });
    }

    // Drag and Drop Logic
    document.querySelectorAll('.premium-file-input').forEach(container => {
        const input = container.querySelector('input[type="file"]');

        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            container.addEventListener(eventName, preventDefaults, false);
        });

        function preventDefaults(e) {
            e.preventDefault();
            e.stopPropagation();
        }

        ['dragenter', 'dragover'].forEach(eventName => {
            container.addEventListener(eventName, () => container.classList.add('drag-over'), false);
        });

        ['dragleave', 'drop'].forEach(eventName => {
            container.addEventListener(eventName, () => container.classList.remove('drag-over'), false);
        });

        container.addEventListener('drop', (e) => {
            const dt = e.dataTransfer;
            const files = dt.files;
            input.files = files;
            window.handleFileSelect(input);
        }, false);
    });

    /* ===========================================================
       UPCOMING (BEKLENENLER) YÖNETİMİ
       =========================================================== */
    const upcomingForm = document.getElementById('upcomingForm');
    const upcomingSearchInput = document.getElementById('upcomingSearch');
    const upcomingSearchResults = document.getElementById('upcomingSearchResults');
    const upcomingRefIdInput = document.getElementById('upcomingRefId');
    const upcomingRefTypeInput = document.getElementById('upcomingRefType');
    const upcomingRefDisplay = document.getElementById('upcomingRefDisplay');

    if (upcomingForm) {
        fetchUpcoming();
        toggleUpcomingCategory();

        // Search logic for upcoming reference
        upcomingSearchInput.addEventListener('input', function () {
            const query = this.value.trim();
            if (query.length < 2) {
                upcomingSearchResults.style.display = 'none';
                return;
            }
            fetch(`/api/search?q=${encodeURIComponent(query)}`)
                .then(res => res.json())
                .then(data => {
                    upcomingSearchResults.innerHTML = '';
                    if (data.length === 0) {
                        upcomingSearchResults.innerHTML = '<div class="search-result-item">Sonuç bulunamadı</div>';
                    } else {
                        data.slice(0, 10).forEach(item => {
                            const div = document.createElement('div');
                            div.className = 'search-result-item';
                            div.innerHTML = `<span>${item.Name || item.name}</span> <small>${item.Type || item.type}</small>`;
                            div.onclick = () => {
                                upcomingRefIdInput.value = item.ID || item.id;
                                upcomingRefTypeInput.value = item.Type || item.type;
                                upcomingRefDisplay.querySelector('strong').innerText = item.Name || item.name;
                                upcomingRefDisplay.style.display = 'flex';
                                upcomingSearchResults.style.display = 'none';
                                upcomingSearchInput.value = '';
                                // Also pre-fill the name field
                                document.getElementById('upcomingName').value = item.Name || item.name;
                                document.getElementById('upcomingType').value = (item.Type === 'Movie' ? 'Movie' : 'SoapOpera');
                            };
                            upcomingSearchResults.appendChild(div);
                        });
                    }
                    upcomingSearchResults.style.display = 'block';
                });
        });

        document.querySelector('.btn-clear-upcoming').onclick = () => {
            upcomingRefIdInput.value = '';
            upcomingRefTypeInput.value = '';
            upcomingRefDisplay.style.display = 'none';
        };

        // File selection UI handler for upcoming
        const upcomingImageInput = document.getElementById('upcomingImage');
        const upcomingImageUrlInput = document.getElementById('upcomingImageUrl');
        const upcomingImageName = document.querySelector('#upcomingImageContainer .file-name');
        // upcomingForm is already declared globally, no need to redeclare here unless scope is intended to be local.
        // const upcomingForm = document.getElementById('upcomingForm');

        if (upcomingImageUrlInput) {
            upcomingImageUrlInput.addEventListener('input', function () {
                updatePosterPreview('upcoming', this.value);
                if (this.value) {
                    lastFetchedPosterUrl = this.value;
                    currentFetchType = 'Upcoming';
                }
            });
        }
        if (upcomingImageInput) {
            upcomingImageInput.addEventListener('change', function () {
                if (this.files && this.files.length > 0) {
                    const file = this.files[0];
                    upcomingImageName.innerText = file.name;
                    document.getElementById('upcomingImageContainer').classList.add('has-file');

                    const reader = new FileReader();
                    reader.onload = function (e) {
                        updatePosterPreview('upcoming', e.target.result);
                    };
                    reader.readAsDataURL(file);
                } else {
                    upcomingImageName.innerText = "Dosya seçilmedi";
                    document.getElementById('upcomingImageContainer').classList.remove('has-file');
                    updatePosterPreview('upcoming', null);
                }
            });
        }

        upcomingForm.addEventListener('submit', function (e) {
            e.preventDefault();
            const formData = new FormData();
            formData.append('name', document.getElementById('upcomingName').value);
            formData.append('type', document.getElementById('upcomingType').value);
            formData.append('category', document.querySelector('input[name="upcomingCategory"]:checked').value);
            formData.append('status', document.getElementById('upcomingStatus').value);
            formData.append('datetime', document.getElementById('upcomingDatetime').value);

            const refId = upcomingRefIdInput.value;
            if (refId) formData.append('referenceId', refId);

            const imageFile = upcomingImageInput.files[0];
            const manualImageUrl = document.getElementById('upcomingImageUrl').value.trim();

            if (imageFile) {
                formData.append('image', imageFile);
            } else if (manualImageUrl) {
                formData.append('imageUrl', manualImageUrl);
            } else if (lastFetchedPosterUrl && currentFetchType === 'Upcoming') {
                formData.append('imageUrl', lastFetchedPosterUrl);
            }

            fetch('/admin/add-upcoming', {
                method: 'POST',
                body: formData
            }).then(res => {
                if (res.ok) {
                    upcomingForm.reset();
                    // Reset toggle to default (Translated)
                    document.getElementById('catTranslated').checked = true;
                    toggleUpcomingCategory();

                    upcomingRefDisplay.style.display = 'none';
                    upcomingRefIdInput.value = '';
                    if (document.getElementById('upcomingImageUrl')) document.getElementById('upcomingImageUrl').value = '';
                    upcomingImageName.innerText = "Resim seçin veya sürükleyin";
                    document.getElementById('upcomingImageContainer').classList.remove('has-file');
                    updatePosterPreview('upcoming', null);
                    lastFetchedPosterUrl = "";
                    fetchUpcoming();
                } else {
                    res.text().then(msg => alert("Hata: " + msg));
                }
            });
        });
    }

    function fetchUpcoming() {
        fetch('/admin/upcoming')
            .then(res => res.json())
            .then(data => {
                const tbody = document.querySelector('#upcomingTable tbody');
                if (!tbody) return;
                tbody.innerHTML = '';
                data.forEach(item => {
                    const tr = document.createElement('tr');
                    const catBadge = item.Category === 'Translated' ?
                        '<span class="badge" style="background: #1ed760; color: #fff; padding: 4px 8px; border-radius: 4px; font-weight: 700;">ÇEVRİLEN</span>' :
                        '<span class="badge" style="background: #6c757d; color: #fff; padding: 4px 8px; border-radius: 4px; font-weight: 700;">BEKLENEN</span>';

                    tr.innerHTML = `
                        <td style="font-weight: 600;">${item.name}</td>
                        <td>${item.type === 'Movie' ? 'Film' : 'Dizi'} ${catBadge}</td>
                        <td><span class="badge" style="background: var(--primary-dim); color: var(--primary); padding: 4px 8px; border-radius: 4px; font-weight: 700;">${item.status}</span></td>
                        <td>${new Date(item.datetime).toLocaleString('tr-TR')}</td>
                        <td>
                            <button class="btn btn-sm btn-danger" onclick='deleteUpcoming("${item.upcomingId}")'><i class="fas fa-trash"></i> SİL</button>
                        </td>
                    `;
                    tbody.appendChild(tr);
                });
            });
    }

    window.addSourceField = function (type, name = '', url = '') {
        const container = document.getElementById(`${type}SourcesList`);
        if (!container) return;

        const row = document.createElement('div');
        row.className = 'source-row premium-source-row';
        row.innerHTML = `
            <div class="source-inputs">
                <div class="input-with-icon">
                    <i class="fas fa-tag"></i>
                    <input type="text" class="source-name" placeholder="Kaynak Adı (örn: Vidmoly)" value="${name}">
                </div>
                <div class="input-with-icon">
                    <i class="fas fa-link"></i>
                    <input type="text" class="source-url" placeholder="Iframe URL veya Link" value="${url}">
                </div>
            </div>
            <button type="button" class="btn-remove-source" onclick="this.parentElement.remove()" title="Kaynağı Kaldır">
                <i class="fas fa-times"></i>
            </button>
        `;
        container.appendChild(row);
    };

    async function saveExternalSources(contentId, type) {
        try {
            // First delete existing sources
            await fetch(`/admin/delete-sources-for-content?contentId=${contentId}`, { method: 'DELETE' });

            const listDiv = document.getElementById(`${type}SourcesList`);
            if (!listDiv) return;

            const rows = listDiv.querySelectorAll('.source-row');
            let order = 0;
            for (const row of rows) {
                const name = row.querySelector('.source-name').value.trim();
                const url = row.querySelector('.source-url').value.trim();

                if (name && url) {
                    const res = await fetch('/admin/add-source', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            contentId: contentId,
                            sourceName: name,
                            sourceUrl: url,
                            sortOrder: order++
                        })
                    });
                    if (!res.ok) {
                        const msg = await res.text();
                        alert(`Kaynak (${name}) kaydedilemedi: ${msg}`);
                    }
                }
            }
        } catch (e) {
            console.error("Source save error:", e);
            alert("Kaynaklar kaydedilirken bir hata oluştu. Lütfen konsolu kontrol edin.");
        }
    }

    window.deleteUpcoming = function (id) {
        if (!confirm("Bunu silmek istediğinize emin misiniz?")) return;
        fetch(`/admin/delete-upcoming?id=${id}`, { method: 'DELETE' })
            .then(res => {
                if (res.ok) fetchUpcoming();
                else res.text().then(msg => alert("Hata: " + msg));
            });
    };

    /* ===========================================================
       VIEW MANAGEMENT (İZLENME YÖNETİMİ)
       =========================================================== */
    const viewStatsSearchInput = document.getElementById('viewStatsSearch');
    if (viewStatsSearchInput) {
        viewStatsSearchInput.addEventListener('input', function () {
            const query = this.value.toLowerCase().trim();
            const rows = document.querySelectorAll('#viewStatsTable tbody tr');
            rows.forEach(row => {
                const name = row.cells[0].innerText.toLowerCase();
                row.style.display = name.includes(query) ? '' : 'none';
            });
        });
    }

    function fetchViewStats() {
        fetch('/admin/view-stats')
            .then(res => res.json())
            .then(stats => {
                const tbody = document.querySelector('#viewStatsTable tbody');
                if (!tbody) return;
                tbody.innerHTML = '';
                stats.forEach(item => {
                    const tr = document.createElement('tr');
                    const isMovie = item.Type === 'Movie';
                    tr.innerHTML = `
                        <td style="font-weight: 600;">${item.Name}</td>
                        <td><span class="item-type">${isMovie ? 'Film' : 'Dizi'}</span></td>
                        <td>
                            <div class="premium-counter">
                                <button class="spin-btn minus" onclick="stepViewCount('${item.ID}', -1)"><i class="fas fa-minus"></i></button>
                                <input type="number" class="view-input" value="${item.viewCount}" id="view_input_${item.ID}">
                                <button class="spin-btn plus" onclick="stepViewCount('${item.ID}', 1)"><i class="fas fa-plus"></i></button>
                            </div>
                        </td>
                        <td>
                            <button class="btn btn-sm btn-primary" onclick="updateViewCount('${item.ID}', '${item.Type}')">
                                <i class="fas fa-save"></i> GÜNCELLE
                            </button>
                        </td>
                    `;
                    tbody.appendChild(tr);
                });
            })
            .catch(err => console.error("View stats error:", err));
    }

    window.stepViewCount = function (id, step) {
        const input = document.getElementById(`view_input_${id}`);
        if (!input) return;
        let val = parseInt(input.value) || 0;
        val += step;
        if (val < 0) val = 0;
        input.value = val;
    };

    window.updateViewCount = function (id, type) {
        const input = document.getElementById(`view_input_${id}`);
        if (!input) return;
        const newCount = input.value;

        const formData = new URLSearchParams();
        formData.append('id', id);
        formData.append('type', type);
        formData.append('count', newCount);

        fetch('/admin/update-view-count', {
            method: 'POST',
            body: formData
        }).then(res => {
            if (res.ok) {
                alert("İzlenme sayısı başarıyla güncellendi.");
                // Sync with other tables if needed, or just let it be
            } else {
                res.text().then(msg => alert("Hata: " + msg));
            }
        });
    }

    /* ===========================================================
       KULLANICI YÖNETİMİ (USER MANAGEMENT)
       =========================================================== */
    const userSearchInput = document.getElementById('userSearch');
    if (userSearchInput) {
        userSearchInput.addEventListener('input', function () {
            const query = this.value.toLowerCase().trim();
            const rows = document.querySelectorAll('#userTable tbody tr');
            rows.forEach(row => {
                const nickname = row.cells[0].innerText.toLowerCase();
                const email = row.cells[1].innerText.toLowerCase();
                row.style.display = (nickname.includes(query) || email.includes(query)) ? '' : 'none';
            });
        });
    }

    const banModal = document.getElementById('banModal');
    let currentBanUserId = null;

    window.openBanModal = function (userId, nickname) {
        currentBanUserId = userId;
        document.getElementById('banUserPlaceholder').innerHTML = `<strong>${nickname}</strong> kullanıcısını yasaklamak üzeresiniz.`;
        document.getElementById('banReason').value = '';
        banModal.style.display = 'block';
    };

    document.querySelectorAll('.close-ban-modal').forEach(btn => {
        btn.onclick = () => { banModal.style.display = 'none'; };
    });

    document.getElementById('confirmBanBtn').onclick = () => {
        const reason = document.getElementById('banReason').value.trim();
        if (!reason) return alert("Lütfen bir yasaklama nedeni giriniz!");
        toggleUserBan(currentBanUserId, true, reason);
    };

    function fetchUsers() {
        const isSuperAdmin = window.currentAdmin && window.currentAdmin.roles.includes('ROLE_SUPER_ADMIN');
        if (!isSuperAdmin) return;

        fetch('/admin/users')
            .then(res => res.json())
            .then(users => {
                const tbody = document.querySelector('#userTable tbody');
                if (!tbody) return;
                tbody.innerHTML = '';
                users.forEach(user => {
                    const tr = document.createElement('tr');
                    const isBanned = user.isBanned;
                    const statusBadge = isBanned ?
                        '<span class="badge" style="background:#dc3545; color:#fff; padding:4px 8px; border-radius:4px; font-weight: 700;">Yasaklı</span>' :
                        '<span class="badge" style="background:#1ed760; color:#fff; padding:4px 8px; border-radius:4px; font-weight: 700;">Aktif</span>';

                    tr.innerHTML = `
                        <td style="font-weight: 600;">${user.nickname}</td>
                        <td>${user.email || '-'}</td>
                        <td>${user.name} ${user.surname}</td>
                        <td>
                            <div class="role-switcher">
                                <button class="role-btn ${user.role === 'USER' || !user.role ? 'active' : ''}" data-role="USER" onclick="updateUserRoleUnified('${user.id}', 'USER', this)">User</button>
                                <button class="role-btn ${user.role === 'ADMIN' ? 'active' : ''}" data-role="ADMIN" onclick="updateUserRoleUnified('${user.id}', 'ADMIN', this)">Admin</button>
                                <button class="role-btn ${user.role === 'SUPER_ADMIN' ? 'active' : ''}" data-role="SUPER_ADMIN" onclick="updateUserRoleUnified('${user.id}', 'SUPER_ADMIN', this)">Super</button>
                            </div>
                        </td>
                        <td>${statusBadge}</td>
                        <td style="max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${user.banReason || ''}">${user.banReason || '-'}</td>
                        <td>
                            <div class="action-btn-group">
                                ${isBanned ?
                            `<button class="action-btn-premium btn-unban" onclick="toggleUserBan('${user.id}', false, '')" title="Yasağı Kaldır"><i class="fas fa-undo"></i></button>` :
                            `<button class="action-btn-premium btn-ban" onclick="openBanModal('${user.id}', '${user.nickname}')" title="Yasakla"><i class="fas fa-ban"></i></button>`
                        }
                                <button class="action-btn-premium btn-delete" onclick="deleteUser('${user.id}', '${user.nickname}')" title="Kullanıcıyı Sil"><i class="fas fa-trash"></i></button>
                            </div>
                        </td>
                    `;
                    tbody.appendChild(tr);
                });
            })
            .catch(err => console.error("User fetch error:", err));
    }

    window.updateUserRoleUnified = function (userId, role, btnElement) {
        if (!confirm(`Kullanıcı rolünü "${role}" olarak güncellemek istediğinize emin misiniz?`)) {
            return;
        }

        const formData = new URLSearchParams();
        formData.append('userId', userId);
        formData.append('role', role);

        // Visual update
        const parent = btnElement.parentElement;
        parent.querySelectorAll('.role-btn').forEach(b => b.classList.remove('active'));
        btnElement.classList.add('active');

        fetch('/admin/update-role', {
            method: 'POST',
            body: formData
        }).then(res => {
            if (res.ok) {
                alert("Rol başarıyla güncellendi.");
                fetchUsers();
            } else {
                res.text().then(msg => alert("Hata: " + msg));
            }
        });
    };

    window.toggleUserBan = function (userId, ban, reason) {
        const formData = new URLSearchParams();
        formData.append('userId', userId);
        formData.append('ban', ban);
        formData.append('reason', reason);

        fetch('/admin/ban-user', {
            method: 'POST',
            body: formData
        }).then(res => {
            if (res.ok) {
                alert(ban ? "Kullanıcı yasaklandı." : "Kullanıcının yasağı kaldırıldı.");
                if (banModal) banModal.style.display = 'none';
                fetchUsers();
            } else {
                res.text().then(msg => alert("Hata: " + msg));
            }
        });
    };

    window.deleteUser = function (userId, nickname) {
        if (!confirm(`"${nickname}" isimli kullanıcıyı kalıcı olarak silmek istediğinize emin misiniz? Bu işlem geri alınamaz.`)) {
            return;
        }

        fetch(`/admin/delete-user?userId=${userId}`, {
            method: 'DELETE'
        }).then(res => {
            if (res.ok) {
                alert("Kullanıcı başarıyla silindi.");
                fetchUsers();
            } else {
                res.text().then(msg => alert("Hata: " + msg));
            }
        });
    };

    /* ===========================================================
       YORUM YÖNETİMİ (COMMENT MODERATION)
       =========================================================== */
    const commentsTableBody = document.querySelector('#commentsTable tbody');
    const refreshCommentsBtn = document.getElementById('refreshCommentsBtn');

    if (refreshCommentsBtn) {
        refreshCommentsBtn.addEventListener('click', fetchPendingComments);
    }

    // Navigasyon değiştiğinde Comments sekmesine tıklanırsa verileri çek
    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', function () {
            if (this.getAttribute('data-section') === 'comments-section') {
                fetchPendingComments();
            }
        });
    });

    function fetchPendingComments() {
        if (!commentsTableBody) return;

        commentsTableBody.innerHTML = '<tr><td colspan="6" class="text-center">Yükleniyor...</td></tr>';

        fetch('/admin/comment-moderation/pending')
            .then(res => res.json())
            .then(comments => {
                commentsTableBody.innerHTML = '';
                if (comments.length === 0) {
                    commentsTableBody.innerHTML = '<tr><td colspan="6" class="text-center">Onay bekleyen yorum yok.</td></tr>';
                    return;
                }

                comments.forEach(comment => {
                    const tr = document.createElement('tr');
                    const dateStr = new Date(comment.date).toLocaleString('tr-TR');
                    // Kullanıcı resmi istenmediği için kaldırıldı.
                    // const profilePhoto = comment.profilePhoto || '/images/default-avatar.png';

                    // Content Name ve Link
                    const contentName = comment.contentName || comment.contentId || 'İçerik';
                    const contentLink = comment.contentId ? `<a href="/watch?id=${comment.contentId}" target="_blank" style="color: var(--primary-color); font-weight: 500;">${contentName}</a>` : '-';

                    tr.innerHTML = `
                        <td>
                            <div style="display: flex; align-items: center; gap: 10px;">
                                <span style="font-weight: 600;">${comment.nickname}</span>
                            </div>
                        </td>
                        <td>
                            <div style="max-width: 300px; white-space: pre-wrap;">${comment.comment}</div>
                            ${comment.spoiler ? '<span class="badge badge-warning" style="font-size: 0.7em;">SPOILER</span>' : ''}
                        </td>
                        <td>${contentLink}</td>
                        <td>${dateStr}</td>
                        <td><span class="badge badge-warning">Onay Bekliyor</span></td>
                        <td>
                            <button class="btn btn-sm btn-success" onclick="approveComment('${comment.id}')" title="Onayla"><i class="fas fa-check"></i></button>
                            <button class="btn btn-sm btn-danger" onclick="rejectComment('${comment.id}')" title="Reddet/Sil"><i class="fas fa-times"></i></button>
                        </td>
                    `;
                    commentsTableBody.appendChild(tr);
                });
            })
            .catch(err => {
                console.error("Comments fetch error:", err);
                commentsTableBody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Yorumlar yüklenirken hata oluştu.</td></tr>';
            });
    }

    window.approveComment = function (id) {
        if (!confirm("Bu yorumu onaylamak istiyor musunuz?")) return;

        fetch(`/admin/comment-moderation/approve?commentId=${id}`, { method: 'POST' })
            .then(res => {
                if (res.ok) {
                    // Remove row or refresh
                    fetchPendingComments();
                } else {
                    res.text().then(msg => alert("Hata: " + msg));
                }
            })
            .catch(err => alert("Hata: " + err));
    };

    window.rejectComment = function (id) {
        if (!confirm("Bu yorumu reddetmek (silmek) istediğinize emin misiniz?")) return;

        fetch(`/admin/comment-moderation/reject?commentId=${id}`, { method: 'DELETE' })
            .then(res => {
                if (res.ok) {
                    fetchPendingComments();
                } else {
                    res.text().then(msg => alert("Hata: " + msg));
                }
            })
            .catch(err => alert("Hata: " + err));
    };

    /* ===========================================================
       DUYURU YÖNETİMİ
       =========================================================== */
    const announcementForm = document.getElementById('announcementForm');

    if (announcementForm) {
        // Load initial settings
        function loadAnnouncementSettings() {
            fetch('/api/settings/announcement')
                .then(res => res.json())
                .then(data => {
                    document.getElementById('announcementText').value = data.text || '';
                    const isActive = data.active === true;
                    document.getElementById('announcementActive').checked = isActive;
                    updateAnnouncementStatusText(isActive);
                })
                .catch(err => console.error("Duyuru ayarları yüklenemedi:", err));
        }

        // Toggle text update
        document.getElementById('announcementActive').addEventListener('change', function () {
            updateAnnouncementStatusText(this.checked);
        });

        function updateAnnouncementStatusText(isActive) {
            const statusSpan = document.getElementById('announcementStatusText');
            if (statusSpan) {
                statusSpan.textContent = isActive ? 'Aktif' : 'Pasif';
                statusSpan.style.color = isActive ? 'var(--primary)' : 'var(--text-dim)';
            }
        }

        // Handle Form Submit
        announcementForm.addEventListener('submit', function (e) {
            e.preventDefault();

            const text = document.getElementById('announcementText').value;
            const active = document.getElementById('announcementActive').checked;
            const btn = this.querySelector('button[type="submit"]');
            const originalText = btn.innerHTML;

            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> GÜNCELLENİYOR...';
            btn.disabled = true;

            fetch('/admin/settings/announcement', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ text, active })
            })
                .then(res => res.json())
                .then(data => {
                    alert("Duyuru ayarları başarıyla güncellendi!");
                })
                .catch(err => {
                    alert("Hata oluştu: " + err);
                })
                .finally(() => {
                    btn.innerHTML = originalText;
                    btn.disabled = false;
                });
        });

        // Load on init
        loadAnnouncementSettings();
    }

});
