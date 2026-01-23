/*** favorites.js ***/
(() => {
    'use strict';

    // --- Cache’lenmiş DOM Elemanları ---
    const container = document.querySelector('.lists-container');
    const createListBtn = document.getElementById('createListBtn');
    const modal = document.getElementById('listManageModal');
    const modalTitle = modal.querySelector('.modal-header h3');
    const nameInput = document.getElementById('editListName');
    const saveBtn = modal.querySelector('.rename-list');
    const deleteBtn = modal.querySelector('.btn-delete');
    const cancelBtn = modal.querySelector('.btn-cancel');
    const closeBtn = modal.querySelector('.close-modal');

    let currentMode = null;   // 'create' | 'edit'
    let currentListName = null;
    let currentListElem = null;

    // --- Başlangıç ---
    document.addEventListener('DOMContentLoaded', () => {
        injectStyles();
        initFavorites();
        if (createListBtn) {
            createListBtn.addEventListener('click', () => openModal('create'));
        }
        // Liste içi tüm etkileşimler için event delegation
        container.addEventListener('click', handleContainerClick);
        // Modal üstündeki düğmeler
        saveBtn.addEventListener('click', handleSave);
        deleteBtn.addEventListener('click', handleDeleteConfirm);
        cancelBtn.addEventListener('click', closeModal);
        closeBtn.addEventListener('click', closeModal);
    });

    // --- Favori Listeleri Başlat ---
    async function initFavorites() {
        container.innerHTML = `<div class="loading"><p>Yükleniyor...</p></div>`;
        try {
            const lists = await fetchUserLists();
            if (!lists.length) {
                showNoListsMessage();
            } else {
                renderLists(lists);
                setupCollapsibleLists();
            }
        } catch (err) {
            console.error(err);
            showError(err.message);
        }
    }

    // --- Event Delegation Handler ---
    function handleContainerClick(e) {
        // İzle butonu
        if (e.target.closest('.watch-btn')) {
            const id = e.target.closest('.content-item').dataset.id;
            window.location.href = `/watch?id=${id}`;
            return;
        }
        // Kaldır butonu
        if (e.target.closest('.remove-btn')) {
            e.stopPropagation();
            const item = e.target.closest('.content-item');
            removeItemFromList(item);
            return;
        }
        // Liste düzenle (kalem ikonu)
        if (e.target.closest('.edit-list-btn')) {
            const wrapper = e.target.closest('.list-wrapper');
            openModal('edit', wrapper.dataset.listName, wrapper);
            return;
        }
        // Video öğesinin kendisine tıklandığında
        if (e.target.closest('.content-item') && !e.target.closest('.content-buttons')) {
            const item = e.target.closest('.content-item');
            window.location.href = `/watch?id=${item.dataset.id}`;
            return;
        }
        // Liste başlığına tıklayıp aç/kapa
        if (e.target.closest('.list-header') && !e.target.closest('.list-action-btn')) {
            const wrapper = e.target.closest('.list-wrapper');
            toggleCollapse(wrapper);
        }
    }

    // --- Modal Açma/Kapama ---
    function openModal(mode, listName = '', listElem = null) {
        currentMode = mode;
        currentListName = listName;
        currentListElem = listElem;

        // Ortak ayarlar
        modal.style.display = 'flex';
        setTimeout(() => modal.classList.add('show'), 10);

        if (mode === 'create') {
            modalTitle.textContent = 'Yeni Liste Oluştur';
            nameInput.value = '';
            nameInput.placeholder = 'Liste adını giriniz';
            saveBtn.textContent = 'Oluştur';
            deleteBtn.style.display = 'none';
        } else {
            modalTitle.textContent = 'Listeyi Düzenle';
            nameInput.value = listName;
            saveBtn.textContent = 'Kaydet';
            deleteBtn.style.display = 'block';
        }

        nameInput.focus();
    }

    function closeModal() {
        modal.classList.remove('show');
        setTimeout(() => modal.style.display = 'none', 300);
    }

    // --- Kaydet Butonu İşlemi ---
    async function handleSave() {
        const title = nameInput.value.trim();
        if (!title) {
            const originalPlaceholder = nameInput.placeholder;
            nameInput.style.border = '2px solid #ff4646';
            nameInput.placeholder = 'Liste adı boş olamaz';
            nameInput.focus();
            setTimeout(() => {
                nameInput.placeholder = originalPlaceholder;
                nameInput.style.border = '2px solid rgba(255,255,255,0.1)';
            }, 2400);
            return;
        }
        try {
            if (currentMode === 'create') {
                await createNewList(title);
                await initFavorites();
                flashButton(createListBtn, '<i class="fas fa-check"></i> Liste başarıyla oluşturuldu');
            } else {
                if (title === currentListName) {
                    closeModal();
                    return;
                }
                const res = await renameList(currentListName, title);
                if (res.Result === 1) {
                    flashButton(createListBtn, '<i class="fas fa-check"></i> Liste adı değiştirildi');
                    setTimeout(() => location.reload(), 500);
                } else {
                    flashButton(createListBtn, '<i class="fas fa-times"></i> Liste adı değiştirilemedi');
                }
            }
            closeModal();
        } catch (err) {
            console.error(err);
            flashButton(createListBtn, '<i class="fas fa-times"></i> Hata oluştu');
        }
    }

    // --- Silme Onay Kutusu ---
    function handleDeleteConfirm() {
        // Sadece düzenleme modundaysa göster
        if (currentMode !== 'edit') return;

        const overlay = document.createElement('div');
        overlay.className = 'confirm-overlay';
        overlay.innerHTML = `
      <div class="confirm-box">
        <h3>Listeyi Sil</h3>
        <p>"${escapeHtml(currentListName)}" listesini silmek istediğinize emin misiniz?</p>
        <div class="confirm-actions">
          <button class="btn-cancel confirm-cancel">İptal</button>
          <button class="btn-delete confirm-delete">Sil</button>
        </div>
      </div>
    `;
        document.body.appendChild(overlay);

        overlay.querySelector('.confirm-cancel').onclick = () => overlay.remove();
        overlay.querySelector('.confirm-delete').onclick = async () => {
            try {
                await deleteList(currentListName);
                flashButton(createListBtn, '<i class="fas fa-check"></i> Liste silindi');
                // Animasyonla kaldır
                currentListElem.style.transition = 'all 0.4s ease';
                currentListElem.style.opacity = '0';
                currentListElem.style.transform = 'translateY(-20px)';
                setTimeout(() => {
                    currentListElem.remove();
                    if (!container.querySelector('.list-wrapper')) {
                        showNoListsMessage();
                    }
                }, 450);
            } catch (err) {
                console.error(err);
                flashButton(createListBtn, '<i class="fas fa-times"></i> Liste silinemedi');
            } finally {
                overlay.remove();
                closeModal();
            }
        };
    }

    // --- Listeden Öğe Kaldır ---
    async function removeItemFromList(item) {
        try {
            await removeFromList(item.dataset.id);
            item.style.transition = 'all 0.3s';
            item.style.opacity = '0';
            item.style.transform = 'scale(0.8)';
            setTimeout(() => {
                const grid = item.closest('.item-grid');
                item.remove();
                if (!grid.children.length) {
                    grid.innerHTML = renderItems([]);
                }
            }, 300);
        } catch (err) {
            console.error(err);
            flashButton(createListBtn, '<i class="fas fa-times"></i> Hata oluştu');
        }
    }

    // --- Collapsible Logic ---
    function setupCollapsibleLists() {
        document.querySelectorAll('.list-header').forEach(header => {
            const icon = header.querySelector('.toggle-indicator') ||
                header.insertBefore(document.createElement('i'), header.querySelector('.list-actions'));
            icon.className = 'fas fa-chevron-down toggle-indicator';
            const content = header.nextElementSibling;
            content.style.height = '0';
            content.style.overflow = 'hidden';
            content.style.transition = 'height 0.3s ease';
        });
    }

    function toggleCollapse(wrapper) {
        const expanded = wrapper.classList.toggle('expanded');
        // Kapat diğerlerini
        document.querySelectorAll('.list-wrapper.expanded').forEach(w => {
            if (w !== wrapper) {
                w.classList.remove('expanded');
                w.querySelector('.list-content').style.height = '0';
            }
        });
        const content = wrapper.querySelector('.list-content');
        const inner = content.querySelector('.list-content-inner');
        if (expanded) {
            content.style.height = '0';
        } else {
            content.style.height = inner.scrollHeight + 'px';
        }
    }

    // --- API Çağrıları ---
    async function fetchUserLists() {
        if (!localStorage.getItem('wdiUserToken')) {
            throw new Error('Oturum açmalısınız');
        }
        const res = await fetch('/api/saved/lists', {credentials: 'include'});
        if (!res.ok) throw new Error('Listeler alınamadı');
        return processListData(await res.json());
    }

    function processListData(data) {
        if (!Array.isArray(data) || !data.length) return [];
        const map = {};
        data.forEach(item => {
            if (!map[item.ListName]) map[item.ListName] = {name: item.ListName, videos: []};
            if (item.VideoID) {
                map[item.ListName].videos.push({
                    id: item.VideoID,
                    title: item.VideoName || 'Başlıksız Video',
                    image: 'media/image/' + item.VideoID,
                    year: item.Year || 'N/A',
                    type: (item.Category || '').split(',')[0] || 'Video'
                });
            }
        });
        return Object.values(map);
    }

    async function createNewList(title) {
        const res = await fetch(`/api/saved/create?title=${encodeURIComponent(title)}`, {
            method: 'POST', credentials: 'include'
        });
        const data = await res.json();
        if (data.Result !== 1) throw new Error(data.Message || 'Hata');
        return data;
    }

    async function renameList(oldName, newName) {
        const res = await fetch(
            `/api/saved/rename-list?title=${encodeURIComponent(oldName)}&newTitle=${encodeURIComponent(newName)}`, {
                method: 'POST', credentials: 'include'
            }
        );
        const data = await res.json();
        if (data.Result !== 1) throw new Error(data.Message || 'Hata');
        return data;
    }

    async function deleteList(title) {
        const form = new FormData();
        form.append('title', title);
        const res = await fetch('/api/saved/delete-list', {
            method: 'POST', body: form, credentials: 'include'
        });
        if (!res.ok) throw new Error('Sunucu hatası');
        return res.json();
    }

    async function removeFromList(id) {
        const res = await fetch(`/api/saved/remove?videoId=${id}`, {
            method: 'POST', credentials: 'include'
        });
        const data = await res.json();
        if (data.Result !== 1) throw new Error('Hata');
        return data;
    }

    // --- Render ---
    function renderLists(lists) {
        container.innerHTML = '';
        lists.forEach(list => {
            const wrap = document.createElement('div');
            wrap.className = 'list-wrapper';
            wrap.dataset.listName = list.name;
            const count = list.videos.length;
            wrap.innerHTML = `
        <div class="list-header">
          <h3 class="list-title"><i class="fas fa-list"></i> ${escapeHtml(list.name)} <span class="video-count">(${count} video)</span></h3>
          <div class="list-actions">
            <button class="list-action-btn edit-list-btn" title="Listeyi Düzenle"><i class="fas fa-edit"></i></button>
          </div>
        </div>
        <div class="list-content">
          <div class="list-content-inner">
            <div class="item-grid">${renderItems(list.videos)}</div>
          </div>
        </div>
      `;
            container.appendChild(wrap);
        });
    }

    function renderItems(videos) {
        if (!videos.length) {
            return `<div class="empty-list"><i class="fas fa-film"></i><p>Henüz video yok.</p></div>`;
        }
        return videos.map(v => `
      <div class="content-item" data-id="${v.id}">
        <img src="${v.image}" alt="${escapeHtml(v.title)}">
        <div class="content-overlay">
          <h4 class="content-title">${escapeHtml(v.title)}</h4>
          <div class="content-meta"><span>${v.year}</span><span>${v.type}</span></div>
        </div>
        <div class="content-buttons">
          <button class="content-btn watch-btn" title="İzle"><i class="fas fa-play"></i></button>
          <button class="content-btn remove-btn" title="Kaldır"><i class="fas fa-times"></i></button>
        </div>
      </div>
    `).join('');
    }

    // --- UI Helpers ---
    function showNoListsMessage() {
        container.innerHTML = `
      <div class="empty-list">
        <i class="fas fa-heart-broken"></i>
        <h3>Henüz istek listeniz yok</h3>
        <p>İzlerken "İstek Listesine Ekle" tıklayın.</p>
        <a href="/" class="btn-primary">Anasayfa</a>
      </div>
    `;
    }

    function showError(msg) {
        container.innerHTML = `
      <div class="empty-list">
        <i class="fas fa-exclamation-circle"></i>
        <h3>Hata</h3>
        <p>${escapeHtml(msg)}</p>
        <button class="btn-primary retry-btn"><i class="fas fa-redo"></i> Tekrar</button>
      </div>
    `;
        container.querySelector('.retry-btn')
            .addEventListener('click', initFavorites);
    }

    function flashButton(btn, html) {
        const orig = btn.innerHTML;
        btn.innerHTML = html;
        setTimeout(() => btn.innerHTML = orig, 3000);
    }

    function escapeHtml(str) {
        const d = document.createElement('div');
        d.textContent = str;
        return d.innerHTML;
    }

    function injectStyles() {
        if (document.getElementById('favorites-styles')) return;
        const css = document.createElement('style');
        css.id = 'favorites-styles';
        css.textContent = `
      .toast { position: fixed; top: 20px; right: 20px; padding: 12px 20px;
        border-radius: 8px; display: flex; align-items: center; gap: 8px;
        background: rgba(0,0,0,0.8); color: #fff; opacity: 0;
        transition: opacity 0.3s ease; z-index: 10000;
      }
      .toast.success { background: rgba(30,215,96,0.9); color: #000; }
      .toast.error   { background: rgba(255,70,70,0.9); }
      .confirm-overlay {
        position: fixed; inset: 0; background: rgba(0,0,0,0.5);
        display: flex; align-items: center; justify-content: center;
        z-index: 10001;
      }
      .confirm-box {
        background: #222; color: #fff; padding: 20px; border-radius: 8px;
        max-width: 300px; text-align: center;
        box-shadow: 0 15px 30px rgba(0,0,0,0.4);
      }
      .confirm-actions {
        margin-top: 15px; display: flex; justify-content: space-around; gap: 10px;
      }
      .confirm-actions button {
        padding: 10px 15px; border-radius: 8px; border: none;
        font-weight: 600; cursor: pointer; transition: all 0.3s ease;
      }
      .confirm-cancel { background: rgba(255,255,255,0.1); color: #fff; }
      .confirm-delete {
        background: linear-gradient(135deg, #ff4646, #d32f2f); color: #fff;
      }
      .confirm-actions button:hover {
        transform: translateY(-3px); box-shadow: 0 5px 10px rgba(0,0,0,0.3);
      }
    `;
        document.head.appendChild(css);
    }

})();
