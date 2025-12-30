document.addEventListener('DOMContentLoaded', function() {

    // Sayfa açılınca mevcut listeleri çek
    fetchMovies();
    fetchSeries();

    /* ===========================================================
       FİLM YÖNETİMİ (BU KISIM AYNI - DOKUNMAYIN)
       =========================================================== */
    const movieForm = document.getElementById('movieForm');
    const movieCancelBtn = document.getElementById('cancelMovieEditBtn');
    const movieSubmitBtn = document.getElementById('movieSubmitBtn');
    const movieFileInput = document.getElementById('movieFile');
    const movieIdInput = document.getElementById('movieId');

    movieForm.addEventListener('submit', function(e) {
        e.preventDefault();
        const movieId = movieIdInput.value;
        const isEditMode = movieId !== "";

        if (isEditMode) {
            const updateData = {
                id: movieId,
                name: document.getElementById('movieName').value,
                category: document.getElementById('movieCategory').value,
                content: document.getElementById('movieSummary').value,
                year: parseInt(document.getElementById('movieYear').value),
                language: document.getElementById('movieLanguage').value
            };
            updateMovie(updateData);
        } else {
            const formData = new FormData();
            formData.append('name', document.getElementById('movieName').value);
            formData.append('category', document.getElementById('movieCategory').value);
            formData.append('summary', document.getElementById('movieSummary').value);
            formData.append('year', document.getElementById('movieYear').value);
            formData.append('language', document.getElementById('movieLanguage').value);
            if (movieFileInput.files.length > 0) formData.append('file', movieFileInput.files[0]);

            uploadDataWithProgress('/admin/add-movie', formData, 'movieForm', 'progressWrapperMovie', 'progressBarMovie', () => {
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
                        <td style="color: white; font-weight: bold;">${movie.name}</td>
                        <td>${movie.year}</td>
                        <td>${movie.category}</td>
                        <td>${movie.language}</td>
                        <td><button class="action-btn" onclick='editMovie(${JSON.stringify(movie)})'>✏️ DÜZENLE</button></td>
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
        movieSubmitBtn.style.backgroundColor = "#ffc107";
        movieSubmitBtn.style.color = "#000";
        movieCancelBtn.style.display = "block";
        movieFileInput.removeAttribute('required');
        document.getElementById('movieForm').scrollIntoView({ behavior: 'smooth' });
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
        movieSubmitBtn.innerText = "FİLMİ SİSTEME YÜKLE";
        movieSubmitBtn.style.backgroundColor = "";
        movieSubmitBtn.style.color = "";
        movieCancelBtn.style.display = "none";
        movieFileInput.setAttribute('required', 'required');
    }


    /* ===========================================================
       DİZİ YÖNETİMİ (REVISED - XML PARSER SYSTEM)
       =========================================================== */
    const seriesForm = document.getElementById('seriesForm');
    const seriesIdInput = document.getElementById('seriesId');
    const seriesCancelBtn = document.getElementById('cancelSeriesEditBtn');
    const seriesSubmitBtn = document.getElementById('seriesSubmitBtn');
    const seriesFileInput = document.getElementById('seriesFile');

    seriesForm.addEventListener('submit', function(e) {
        e.preventDefault();
        const sId = seriesIdInput.value;

        if (sId !== "") {
            // GÜNCELLEME (Sadece metadata)
            const updateData = {
                id: sId,
                name: document.getElementById('seriesName').value,
                category: document.getElementById('seriesCategory').value,
                content: document.getElementById('seriesSummary').value,
                language: document.getElementById('seriesLanguage').value,
                // Metadata güncellemesinde bölüm numaralarını göndermiyoruz, sunucu zaten mevcut diziyi güncelliyor
            };
            updateSeries(updateData);
        } else {
            // YENİ EKLEME
            const formData = new FormData();
            formData.append('name', document.getElementById('seriesName').value);
            formData.append('season', document.getElementById('seasonNum').value);
            formData.append('episode', document.getElementById('episodeNum').value);
            formData.append('category', document.getElementById('seriesCategory').value);
            formData.append('summary', document.getElementById('seriesSummary').value);
            formData.append('year', document.getElementById('seriesYear').value);
            formData.append('language', document.getElementById('seriesLanguage').value);
            if (seriesFileInput.files.length > 0) formData.append('file', seriesFileInput.files[0]);

            uploadDataWithProgress('/admin/add-series', formData, 'seriesForm', 'progressWrapperSeries', 'progressBarSeries', () => {
                fetchSeries();
            });
        }
    });

    // --- YENİ FETCH ALGORİTMASI (XML PARSER) ---
    function fetchSeries() {
        fetch('/admin/series')
            .then(res => res.json())
            .then(seriesList => {
                const container = document.getElementById('seriesAccordion');
                container.innerHTML = '';

                if (seriesList.length === 0) {
                    container.innerHTML = '<p style="text-align:center; color:#888;">Henüz hiç dizi yüklenmemiş Efendim.</p>';
                    return;
                }

                seriesList.forEach(series => {
                    // 1. Diziye ait XML verisini çözümle (Parse)
                    const episodes = parseEpisodesFromXML(series.xmlData, series.name, series.category, series.content, series.language, series.year);
                    const episodeCount = episodes.length;

                    // 2. HTML İskeletini Oluştur
                    const groupDiv = document.createElement('div');
                    groupDiv.className = 'series-group';

                    groupDiv.innerHTML = `
                        <div class="series-header" onclick="toggleAccordion(this)">
                            <div class="series-info">
                                <h3>${series.name}</h3>
                                <div class="series-meta">
                                    ${series.category || 'Genel'} | ${series.language || '-'} | Toplam: ${episodeCount} Bölüm
                                </div>
                            </div>
                            <div class="series-actions" onclick="event.stopPropagation()">
                                <button class="btn btn-sm btn-primary" onclick='editSeriesMetadata(${JSON.stringify(series).replace(/'/g, "&#39;")})'>DİZİ BİLGİSİNİ DÜZENLE</button>
                                <button class="btn btn-sm btn-danger" onclick='deleteSeriesByName("${series.name}")'>SERİYİ SİL</button>
                            </div>
                        </div>
                        <div class="episodes-list">
                            ${episodes.length > 0 ? episodes.map(ep => `
                                <div class="episode-item">
                                    <div class="episode-title">
                                        <strong>Sezon ${ep.season} - Bölüm ${ep.episode}</strong>
                                        <small style="margin-left:10px; color:#888;">(ID: ${ep.id.substring(0,8)}...)</small>
                                    </div>
                                    <div class="episode-actions">
                                        <button class="btn btn-sm btn-primary" onclick='preloadEpisodeForm(${JSON.stringify(ep)})'>BİLGİLERİ GETİR</button>
                                        <button class="btn btn-sm btn-danger" onclick='deleteEpisode("${ep.id}")'>SİL</button>
                                    </div>
                                </div>
                            `).join('') : '<div style="padding:15px; color:#666;">Bu dizide henüz bölüm yok veya XML bozuk.</div>'}
                        </div>
                    `;
                    container.appendChild(groupDiv);
                });
            })
            .catch(err => {
                console.error("Dizi listesi hatası:", err);
                document.getElementById('seriesAccordion').innerHTML = '<p style="color:red; text-align:center;">Veriler alınırken bir hata oluştu Efendim.<br>' + err + '</p>';
            });
    }

    // --- XML PARSER (JARVIS ENGINE) ---
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
                        id: ep.textContent, // UUID
                        season: seasonNum,
                        episode: ep.getAttribute("number"),
                        // Metadata'yı Parent'tan miras alıyoruz
                        name: seriesName,
                        category: cat,
                        content: summ,
                        language: lang,
                        year: year
                    });
                });
            });

            // Sıralama: Sezon -> Bölüm
            return allEpisodes.sort((a, b) => {
                if (parseInt(a.season) === parseInt(b.season)) return parseInt(a.episode) - parseInt(b.episode);
                return parseInt(a.season) - parseInt(b.season);
            });

        } catch (e) {
            console.error("XML Parse Hatası:", e);
            return [];
        }
    }

    // Accordion Aç/Kapa
    window.toggleAccordion = function(element) {
        element.classList.toggle("active");
        const panel = element.nextElementSibling;
        if (panel.style.maxHeight) {
            panel.style.maxHeight = null;
            panel.classList.remove('active');
        } else {
            panel.classList.add('active');
            panel.style.maxHeight = panel.scrollHeight + "px";
        }
    };

    // --- EDİT İŞLEMLERİ ---

    // 1. Dizi Metadata Düzenle
    window.editSeriesMetadata = function(series) {
        seriesIdInput.value = series.id;
        document.getElementById('seriesName').value = series.name;
        document.getElementById('seriesCategory').value = series.category;
        document.getElementById('seriesSummary').value = series.content || series._content; // content veya _content
        document.getElementById('seriesLanguage').value = series.language;
        // document.getElementById('seriesYear').value = series.year; // Eğer varsa

        // Dizi düzenlerken bölüm numarası girilmez, pasif yapalım veya gizleyelim
        // Ancak form yapısı gereği boş kalması sorun değil
        setupEditMode("DİZİ BİLGİSİNİ GÜNCELLE");
    };

    // 2. Bölüm Bilgilerini Forma Getir (Yeni dosya yüklemek veya benzerini eklemek için)
    window.preloadEpisodeForm = function(ep) {
        // Not: Bölüm ID'sini güncelleme için kullanamıyoruz çünkü bölüm güncelleme API'si yok.
        // Ama bilgileri forma doldurup yeni bölüm eklemeyi kolaylaştırabiliriz.
        // seriesIdInput.value = ""; // Yeni kayıt gibi davran

        document.getElementById('seriesName').value = ep.name;
        document.getElementById('seriesCategory').value = ep.category;
        document.getElementById('seriesSummary').value = ep.content;
        document.getElementById('seriesLanguage').value = ep.language;
        document.getElementById('seasonNum').value = ep.season;
        document.getElementById('episodeNum').value = ep.episode;

        alert("Bölüm bilgileri forma kopyalandı. Dosya seçip 'KAYDET' derseniz bu numaraya (veya üzerine) yazar.");
        document.getElementById('seriesForm').scrollIntoView({ behavior: 'smooth' });
    };

    function setupEditMode(btnText) {
        seriesSubmitBtn.innerText = btnText;
        seriesSubmitBtn.style.backgroundColor = "#ffc107";
        seriesSubmitBtn.style.color = "#000";
        seriesCancelBtn.style.display = "block";
        seriesFileInput.removeAttribute('required');
        document.getElementById('seriesForm').scrollIntoView({ behavior: 'smooth' });
    }

    // --- SİLME İŞLEMLERİ ---
    window.deleteEpisode = function(id) {
        if(!confirm("Bu bölümü silmek istediğinize emin misiniz Efendim?")) return;
        fetch('/admin/delete-episode?id=' + id, { method: 'DELETE' })
            .then(res => {
                if(res.ok) { alert("Bölüm imha edildi."); fetchSeries(); }
                else alert("Hata oluştu.");
            });
    };

    window.deleteSeriesByName = function(name) {
        if(!confirm("DİKKAT! '" + name + "' dizisine ait TÜM BÖLÜMLER silinecek. Onaylıyor musunuz?")) return;
        fetch('/admin/delete-series-by-name?name=' + encodeURIComponent(name), { method: 'DELETE' })
            .then(res => {
                if(res.ok) { alert("Dizi komple silindi."); fetchSeries(); }
                else alert("Silme işlemi başarısız oldu.");
            });
    };

    // Güncelleme Request
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
        seriesSubmitBtn.style.backgroundColor = "";
        seriesSubmitBtn.style.color = "";
        seriesCancelBtn.style.display = "none";
        seriesFileInput.setAttribute('required', 'required');
    }

    /* ===========================================================
       ORTAK FONKSİYONLAR (JARVIS GÜVENLİK)
       =========================================================== */
    function uploadDataWithProgress(url, formData, formId, wrapperId, barId, successCallback) {
        const xhr = new XMLHttpRequest();
        const wrapper = document.getElementById(wrapperId);
        const bar = document.getElementById(barId);
        const btn = document.querySelector(`#${formId} button[type="submit"]`);
        const originalBtnText = btn.innerText;

        // ---- JARVIS AKILLI PROTOKOL (v2.0 - Gizli Hat) ----
        const formElement = document.getElementById(formId);
        const fileInput = formElement.querySelector('input[type="file"]');

        if (fileInput && fileInput.files.length > 0) {
            const fileSizeMB = fileInput.files[0].size / (1024 * 1024);
            const currentHost = window.location.hostname;
            const currentPort = window.location.port;
            // Gizli kelime veya port kontrolü
            const isBackdoor = currentHost.includes('uploadozr9x0q3glr158beem49') || currentPort === '8443';

            if (fileSizeMB > 95 && !isBackdoor) {
                alert(
                    "⚠️ DİKKAT EFENDİM! GÜVENLİ (KISITLI) HATTASINIZ.\n\n" +
                    "Cloudflare kalkanları 100MB üzerini engelliyor.\n" +
                    "Lütfen 'GİZLİ ARKA KAPI' adresine geçiş yapın:\n" +
                    "➡️ https://uploadozr9x0q3glr158beem49.whodatidols.com:8443/admin/panel"
                );
                return;
            }
        }
        // ---- JARVIS KONTROLÜ BİTİŞİ ----

        btn.disabled = true;
        btn.innerText = "YÜKLENİYOR... (%0)";
        wrapper.style.display = "block";
        bar.style.width = "0%";
        bar.innerText = "0%";
        bar.style.backgroundColor = "#007bff";

        xhr.upload.addEventListener("progress", function(e) {
            if (e.lengthComputable) {
                const percentComplete = Math.round((e.loaded / e.total) * 100);
                bar.style.width = percentComplete + "%";
                bar.innerText = percentComplete + "%";
                btn.innerText = "YÜKLENİYOR... (%" + percentComplete + ")";
                if(percentComplete > 99) {
                    bar.style.backgroundColor = "#28a745";
                    bar.innerText = "Sunucu İşliyor...";
                }
            }
        });

        xhr.addEventListener("load", function() {
            if (xhr.status === 200) {
                alert("İşlem Başarılı Efendim!\n" + xhr.responseText);
                document.getElementById(formId).reset();
                wrapper.style.display = "none";
                if (successCallback) successCallback();
            } else {
                alert("Hata Oluştu: " + xhr.statusText + "\n" + xhr.responseText);
                bar.style.backgroundColor = "#dc3545";
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