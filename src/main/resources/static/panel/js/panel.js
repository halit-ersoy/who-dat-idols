document.addEventListener('DOMContentLoaded', function() {

    // Sayfa açılınca mevcut listeleri çek
    fetchMovies();
    fetchSeries();

    /* ===========================================================
       FİLM YÖNETİMİ (MOVIE SECTION)
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
            // GÜNCELLEME (Update)
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
            // EKLEME (Upload)
            const formData = new FormData();
            formData.append('name', document.getElementById('movieName').value);
            formData.append('category', document.getElementById('movieCategory').value);
            formData.append('summary', document.getElementById('movieSummary').value);
            formData.append('year', document.getElementById('movieYear').value);
            formData.append('language', document.getElementById('movieLanguage').value);

            if (movieFileInput.files.length > 0) {
                formData.append('file', movieFileInput.files[0]);
            }

            uploadDataWithProgress('/admin/add-movie', formData, 'movieForm', 'progressWrapperMovie', 'progressBarMovie', () => {
                fetchMovies(); // Başarılı olunca listeyi yenile
            });
        }
    });

    // Film Listeleme
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
                        <td>
                            <button class="action-btn" onclick='editMovie(${JSON.stringify(movie)})'>✏️ DÜZENLE</button>
                        </td>
                    `;
                    tbody.appendChild(tr);
                });
            })
            .catch(err => console.error("Film listesi hatası:", err));
    }

    // Film Düzenleme Modu
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
        movieFileInput.removeAttribute('required'); // Dosya zorunlu değil

        document.getElementById('movieForm').scrollIntoView({ behavior: 'smooth' });
    };

    // Film Güncelleme (JSON)
    function updateMovie(data) {
        movieSubmitBtn.innerText = "GÜNCELLENİYOR...";
        movieSubmitBtn.disabled = true;

        fetch('/admin/update-movie', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        })
            .then(res => res.text())
            .then(msg => {
                alert(msg);
                resetMovieForm();
                fetchMovies();
            })
            .catch(err => alert("Hata: " + err))
            .finally(() => { movieSubmitBtn.disabled = false; });
    }

    // Film Formu Reset
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
       DİZİ YÖNETİMİ (SERIES SECTION)
       =========================================================== */
    const seriesForm = document.getElementById('seriesForm');
    const seriesIdInput = document.getElementById('seriesId');
    const seriesCancelBtn = document.getElementById('cancelSeriesEditBtn');
    const seriesSubmitBtn = document.getElementById('seriesSubmitBtn');
    const seriesFileInput = document.getElementById('seriesFile');
    const seasonRow = document.getElementById('seriesSeasonRow');

    seriesForm.addEventListener('submit', function(e) {
        e.preventDefault();

        const sId = seriesIdInput.value;
        const isEdit = sId !== "";

        if (isEdit) {
            // Sadece Metadata GÜNCELLEME (Bölüm/Sezon eklemez)
            const updateData = {
                id: sId,
                name: document.getElementById('seriesName').value,
                category: document.getElementById('seriesCategory').value,
                content: document.getElementById('seriesSummary').value,
                language: document.getElementById('seriesLanguage').value
            };
            updateSeries(updateData);
        } else {
            // YENİ BÖLÜM EKLEME (XML'e işler)
            const formData = new FormData();
            formData.append('name', document.getElementById('seriesName').value);
            formData.append('season', document.getElementById('seasonNum').value);
            formData.append('episode', document.getElementById('episodeNum').value);
            formData.append('category', document.getElementById('seriesCategory').value);
            formData.append('summary', document.getElementById('seriesSummary').value);
            formData.append('year', document.getElementById('seriesYear').value);
            formData.append('language', document.getElementById('seriesLanguage').value);

            if (seriesFileInput.files.length > 0) {
                formData.append('file', seriesFileInput.files[0]);
            }

            uploadDataWithProgress('/admin/add-series', formData, 'seriesForm', 'progressWrapperSeries', 'progressBarSeries', () => {
                fetchSeries(); // Listeyi yenile
            });
        }
    });

    // Dizi Listeleme
    function fetchSeries() {
        fetch('/admin/series')
            .then(res => res.json())
            .then(list => {
                const tbody = document.querySelector('#seriesTable tbody');
                tbody.innerHTML = '';
                list.forEach(s => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td style="color: white; font-weight: bold;">${s.name}</td>
                        <td>${s.category}</td>
                        <td>${s.language}</td>
                        <td>
                            <button class="action-btn" onclick='editSeries(${JSON.stringify(s)})'>✏️ DÜZENLE</button>
                        </td>
                    `;
                    tbody.appendChild(tr);
                });
            })
            .catch(err => console.error("Dizi listesi hatası:", err));
    }

    // Dizi Düzenleme Modu
    window.editSeries = function(s) {
        seriesIdInput.value = s.id;
        document.getElementById('seriesName').value = s.name;
        document.getElementById('seriesCategory').value = s.category;
        document.getElementById('seriesSummary').value = s.content;
        document.getElementById('seriesLanguage').value = s.language;

        // Düzenleme modunda bölüm/sezon/dosya gizlenebilir veya pasif yapılabilir
        // Burada dosya zorunluluğunu kaldırıyoruz
        seriesFileInput.removeAttribute('required');

        // Sezon/Bölüm inputlarını düzenleme modunda opsiyonel gizleyebiliriz (veya sadece bilgi düzenliyoruz)
        // document.getElementById('seasonNum').disabled = true;

        seriesSubmitBtn.innerText = "DİZİ BİLGİLERİNİ GÜNCELLE";
        seriesSubmitBtn.style.backgroundColor = "#ffc107"; // Sarı
        seriesSubmitBtn.style.color = "#000";
        seriesCancelBtn.style.display = "block";

        document.getElementById('seriesForm').scrollIntoView({ behavior: 'smooth' });
    };

    // Dizi Metadata Güncelleme
    function updateSeries(data) {
        seriesSubmitBtn.innerText = "GÜNCELLENİYOR...";
        seriesSubmitBtn.disabled = true;

        fetch('/admin/update-series', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        })
            .then(res => res.text())
            .then(msg => {
                alert(msg);
                resetSeriesForm();
                fetchSeries();
            })
            .catch(err => alert("Hata: " + err))
            .finally(() => { seriesSubmitBtn.disabled = false; });
    }

    // Dizi Formu Reset
    seriesCancelBtn.addEventListener('click', resetSeriesForm);
    function resetSeriesForm() {
        seriesForm.reset();
        seriesIdInput.value = "";

        seriesSubmitBtn.innerText = "BÖLÜMÜ / DİZİYİ KAYDET";
        seriesSubmitBtn.style.backgroundColor = "";
        seriesSubmitBtn.style.color = "";

        seriesCancelBtn.style.display = "none";
        seriesFileInput.setAttribute('required', 'required');

        // document.getElementById('seasonNum').disabled = false;
    }


    /* ===========================================================
       ORTAK FONKSİYONLAR (CORE) & JARVIS GÜVENLİK
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

            // KONTROL: Adresimiz belirlediğiniz GİZLİ KELİMEYİ içeriyor mu?
            // Veya doğrudan 8443 portundan mı bağlıyız?
            const isBackdoor = currentHost.includes('uploadozr9x0q3glr158beem49');

            // Eğer dosya 95MB'dan büyükse VE gizli kapıda değilsek alarm ver
            if (fileSizeMB > 95 && !isBackdoor) {
                alert(
                    "⚠️ DİKKAT EFENDİM! GÜVENLİ (KISITLI) HATTASINIZ.\n\n" +
                    "Cloudflare kalkanları 100MB üzerini engelliyor.\n" +
                    "Bu büyük dosyayı yüklemek için lütfen 'GİZLİ ARKA KAPI' adresine geçiş yapın:\n\n" +
                    "➡️ https://uploadozr9x0q3glr158beem49.whodatidols.com:8443/admin/panel\n\n" +
                    "(Not: Tarayıcı 'Güvenli Değil' diyebilir, şifreleme aktiftir, devam ediniz.)"
                );
                return; // İşlemi iptal et
            }
        }
        // ---- JARVIS KONTROLÜ BİTİŞİ ----

        // Arayüz Hazırlığı
        btn.disabled = true;
        btn.innerText = "YÜKLENİYOR... (%0)";
        wrapper.style.display = "block";
        bar.style.width = "0%";
        bar.innerText = "0%";
        bar.style.backgroundColor = "#007bff";

        // Progress
        xhr.upload.addEventListener("progress", function(e) {
            if (e.lengthComputable) {
                const percentComplete = Math.round((e.loaded / e.total) * 100);
                bar.style.width = percentComplete + "%";
                bar.innerText = percentComplete + "%";
                btn.innerText = "YÜKLENİYOR... (%" + percentComplete + ")";

                if(percentComplete > 99) {
                    bar.style.backgroundColor = "#28a745"; // Yeşil
                    bar.innerText = "Sunucu İşliyor / Süre Hesaplanıyor...";
                }
            }
        });

        // Load
        xhr.addEventListener("load", function() {
            if (xhr.status === 200) {
                alert("İşlem Başarılı Efendim!\n\n" + xhr.responseText);
                document.getElementById(formId).reset();
                wrapper.style.display = "none";
                if (successCallback) successCallback();
            } else {
                alert("Hata Oluştu: " + xhr.statusText + "\n" + xhr.responseText);
                bar.style.backgroundColor = "#dc3545"; // Kırmızı
            }
            btn.disabled = false;
            btn.innerText = originalBtnText;
        });

        // Error
        xhr.addEventListener("error", function() {
            alert("Ağ Hatası! Sunucuya ulaşılamadı. (Dosya boyutu çok büyük olabilir)");
            btn.disabled = false;
            btn.innerText = originalBtnText;
        });

        xhr.open("POST", url);
        xhr.send(formData);
    }
});