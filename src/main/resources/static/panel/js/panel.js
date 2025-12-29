document.addEventListener('DOMContentLoaded', function() {

    // --- FİLM FORMU DİNLEYİCİSİ ---
    const movieForm = document.getElementById('movieForm');
    movieForm.addEventListener('submit', function(e) {
        e.preventDefault();

        // Verileri Hazırla
        const formData = new FormData();
        formData.append('name', document.getElementById('movieName').value);
        formData.append('category', document.getElementById('movieCategory').value);
        formData.append('time', document.getElementById('movieTime').value);
        formData.append('year', document.getElementById('movieYear').value);
        formData.append('language', document.getElementById('movieLanguage').value);

        // Dosyayı Ekle
        const fileInput = document.getElementById('movieFile');
        if (fileInput.files.length > 0) {
            formData.append('file', fileInput.files[0]);
        }

        // Sunucuya Gönder
        uploadData('/admin/add-movie', formData, 'movieForm', 'Film');
    });


    // --- DİZİ FORMU DİNLEYİCİSİ ---
    const seriesForm = document.getElementById('seriesForm');
    seriesForm.addEventListener('submit', function(e) {
        e.preventDefault();

        // Verileri Hazırla
        const formData = new FormData();
        formData.append('name', document.getElementById('seriesName').value);
        formData.append('category', document.getElementById('seriesCategory').value);
        formData.append('time', document.getElementById('seriesTime').value);
        formData.append('year', document.getElementById('seriesYear').value);
        formData.append('language', document.getElementById('seriesLanguage').value);

        // Dosyayı Ekle (Diziler için de artık dosya alıyoruz)
        const fileInput = document.getElementById('seriesFile');
        if (fileInput.files.length > 0) {
            formData.append('file', fileInput.files[0]);
        }

        // Sunucuya Gönder
        uploadData('/admin/add-series', formData, 'seriesForm', 'Dizi');
    });


    // --- ORTAK GÖNDERİM FONKSİYONU (JARVIS CORE) ---
    function uploadData(url, formData, formId, typeName) {
        // Kullanıcıya bilgi ver (Büyük dosyalar için bekleme süresi olabilir)
        const originalBtnText = document.querySelector(`#${formId} button`).innerText;
        document.querySelector(`#${formId} button`).innerText = "YÜKLENİYOR... LÜTFEN BEKLEYİN";
        document.querySelector(`#${formId} button`).disabled = true;

        fetch(url, {
            method: 'POST',
            // Content-Type header'ını elle ayarlamıyoruz, FormData otomatik halleder (boundary ile)
            body: formData
        })
            .then(response => {
                if (response.ok) {
                    return response.text();
                } else {
                    throw new Error('Sunucu hatası: ' + response.statusText);
                }
            })
            .then(message => {
                alert(typeName + ' ve dosyası başarıyla sisteme işlendi Efendim!\n\nSunucu Mesajı: ' + message);
                document.getElementById(formId).reset();
            })
            .catch(error => {
                console.error('Hata:', error);
                alert('Bir pürüz çıktı Efendim. Lütfen sunucu loglarını ve dosya boyutunu kontrol edin.');
            })
            .finally(() => {
                // Butonu eski haline getir
                document.querySelector(`#${formId} button`).innerText = originalBtnText;
                document.querySelector(`#${formId} button`).disabled = false;
            });
    }

});