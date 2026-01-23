document.addEventListener('DOMContentLoaded', function() {

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

    // Sayfa açılınca mevcut listeleri çek
    fetchHeroVideos();
    fetchMovies();
    fetchSeries();

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

    heroSearchInput.addEventListener('input', function() {
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

    heroForm.addEventListener('submit', function(e) {
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

    window.moveHero = function(index, direction) {
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

    window.deleteHero = function(id, name) {
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

    movieForm.addEventListener('submit', function(e) {
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

            uploadDataWithProgress('/admin/add-movie', formData, 'movieForm', 'progressWrapperMovie', 'progressBarMovie', 'percentMovie', () => {
                fetchMovies();
            });
        }
    });

    function fetchMovies() {
        fetch('/admin/movies')
            .then(res => res.json())
            .then(movies => {
                const tbody = document.querySelector('#movieTable tbody');
                tbody.innerHTML = '';
                movies.forEach(movie => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
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

    window.editMovie = function(movie) {
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
    }

    window.deleteMovie = function(id, name) {
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

    seriesForm.addEventListener('submit', function(e) {
        e.preventDefault();
        const sId = seriesIdInput.value;

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
            formData.append('name', document.getElementById('seriesName').value);
            formData.append('season', document.getElementById('seasonNum').value);
            formData.append('episode', document.getElementById('episodeNum').value);
            formData.append('category', document.getElementById('seriesCategory').value);
            formData.append('summary', document.getElementById('seriesSummary').value);
            formData.append('year', document.getElementById('seriesYear').value);
            formData.append('language', document.getElementById('seriesLanguage').value);
            if (seriesFileInput.files.length > 0) formData.append('file', seriesFileInput.files[0]);
            if (seriesImageInput.files.length > 0) formData.append('image', seriesImageInput.files[0]);

            uploadDataWithProgress('/admin/add-series', formData, 'seriesForm', 'progressWrapperSeries', 'progressBarSeries', 'percentSeries', () => {
                fetchSeries();
            });
        }
    });

    function fetchSeries() {
        fetch('/admin/series')
            .then(res => res.json())
            .then(seriesList => {
                const container = document.getElementById('seriesAccordion');
                container.innerHTML = '';

                if (seriesList.length === 0) {
                    container.innerHTML = '<div class="loading-state">Henüz hiç dizi yüklenmemiş.</div>';
                    return;
                }

                seriesList.forEach(series => {
                    const episodes = parseEpisodesFromXML(series.xmlData, series.name, series.category, series.content, series.language, series.year);
                    const episodeCount = episodes.length;

                    const groupDiv = document.createElement('div');
                    groupDiv.className = 'series-group';

                    groupDiv.innerHTML = `
                        <div class="series-header" onclick="toggleAccordion(this)">
                            <div class="series-info">
                                <h3>${series.name}</h3>
                                <div class="series-meta">
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
    }

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

    window.toggleAccordion = function(element) {
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

    window.editSeriesMetadata = function(series) {
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

    window.preloadEpisodeForm = function(ep) {
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

    window.deleteEpisode = function(id) {
        if(!confirm("Bu bölümü silmek istediğinize emin misiniz?")) return;
        fetch('/admin/delete-episode?id=' + id, { method: 'DELETE' })
            .then(res => {
                if(res.ok) { fetchSeries(); }
                else alert("Hata oluştu.");
            });
    };

    window.deleteSeriesByName = function(name) {
        if(!confirm("'" + name + "' dizisine ait TÜM BÖLÜMLER silinecek. Emin misiniz?")) return;
        fetch('/admin/delete-series-by-name?name=' + encodeURIComponent(name), { method: 'DELETE' })
            .then(res => {
                if(res.ok) { fetchSeries(); }
                else alert("Silme işlemi başarısız oldu.");
            });
    };

    function updateSeries(data) {
        seriesSubmitBtn.innerText = "GÜNCELLENİYOR...";
        seriesSubmitBtn.disabled = true;
        fetch('/admin/update-series', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
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
                    "⚠️ Cloudflare kalkanları 100MB üzerini engelliyor.\n" +
                    "Lütfen 'GİZLİ ARKA KAPI' adresine geçiş yapın:\n" +
                    "➡️ https://uploadozr9x0q3glr158beem49.whodatidols.com:8443/admin/panel"
                );
                return;
            }
        }

        btn.disabled = true;
        btn.innerText = "YÜKLENİYOR...";
        wrapper.style.display = "block";
        bar.style.width = "0%";
        percentText.innerText = "0%";

        xhr.upload.addEventListener("progress", function(e) {
            if (e.lengthComputable) {
                const percentComplete = Math.round((e.loaded / e.total) * 100);
                bar.style.width = percentComplete + "%";
                percentText.innerText = percentComplete + "%";
                if(percentComplete > 99) {
                    percentText.innerText = "İşleniyor...";
                }
            }
        });

        xhr.addEventListener("load", function() {
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

        xhr.addEventListener("error", function() {
            alert("Ağ Hatası!");
            btn.disabled = false;
            btn.innerText = originalBtnText;
        });

        xhr.open("POST", url);
        xhr.send(formData);
    }
});