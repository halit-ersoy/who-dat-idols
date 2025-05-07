/*** Entry point ***/
document.addEventListener('DOMContentLoaded', () => {
    initFavorites()
        .then(() => setupCollapsibleLists())
        .catch(err => console.error('Initialization error:', err));

    const createListBtn = document.getElementById('createListBtn');
    if (createListBtn) {
        createListBtn.addEventListener('click', openCreateListModal);
    }
});

/*** Create List Modal ***/
function openCreateListModal() {
    const modal = document.getElementById('listManageModal');
    const titleEl = modal.querySelector('.modal-header h3');
    const nameInput = document.getElementById('editListName');
    const saveBtn = modal.querySelector('.rename-list');
    const deleteBtn = modal.querySelector('.btn-delete');
    const cancelBtn = modal.querySelector('.btn-cancel');
    const closeBtn = modal.querySelector('.close-modal');

    // Setup for "create"
    titleEl.textContent = 'Yeni Liste Oluştur';
    nameInput.value = '';
    nameInput.placeholder = 'Liste adını giriniz';
    saveBtn.textContent = 'Oluştur';
    deleteBtn.style.display = 'none';

    showModal();

    function closeModal() {
        modal.classList.remove('show');
        setTimeout(() => {
            modal.style.display = 'none';
            deleteBtn.style.display = 'block';
        }, 300);
    }

    closeBtn.onclick = closeModal;
    cancelBtn.onclick = closeModal;

    saveBtn.onclick = async () => {
        const title = nameInput.value.trim();
        if (!title) {
            showToast('error', 'Liste adı boş olamaz');
            return;
        }
        try {
            await createNewList(title);
            showToast('success', 'Liste başarıyla oluşturuldu');
            closeModal();
            await initFavorites();
        } catch (err) {
            console.error(err);
            showToast('error', 'Liste oluşturulamadı');
        }
    };

    function showModal() {
        modal.style.display = 'flex';
        setTimeout(() => modal.classList.add('show'), 10);
    }
}

/*** Edit & Delete List Modal ***/
function openEditModal(oldName, listElement) {
    const modal = document.getElementById('listManageModal');
    const titleEl = modal.querySelector('.modal-header h3');
    const nameInput = document.getElementById('editListName');
    const saveBtn = modal.querySelector('.rename-list');
    const deleteBtn = modal.querySelector('.btn-delete');
    const cancelBtn = modal.querySelector('.btn-cancel');
    const closeBtn = modal.querySelector('.close-modal');

    // Setup for "edit"
    titleEl.textContent = 'Listeyi Düzenle';
    nameInput.value = oldName;
    saveBtn.textContent = 'Kaydet';
    deleteBtn.style.display = 'block';

    showModal();

    function closeModal() {
        modal.classList.remove('show');
        setTimeout(() => modal.style.display = 'none', 300);
    }

    closeBtn.onclick = closeModal;
    cancelBtn.onclick = closeModal;

    // Rename
    saveBtn.onclick = async () => {
        const newName = nameInput.value.trim();
        if (!newName || newName === oldName) {
            closeModal();
            return;
        }
        try {
            const res = await renameList(oldName, newName);
            if (res.Result === 1) {
                showToast('success', 'Liste adı değiştirildi');
                closeModal();
                setTimeout(() => location.reload(), 500);
            } else {
                showToast('error', res.Message || 'Değiştirilemedi');
            }
        } catch (err) {
            console.error(err);
            showToast('error', 'Liste adı güncellenemedi');
        }
    };

    // Delete
    deleteBtn.onclick = () => {
        const overlay = document.createElement('div');
        overlay.className = 'confirm-overlay';
        overlay.innerHTML = `
<div class="confirm-box">
    <h3>Listeyi Sil</h3>
<p>"${escapeHtml(oldName)}" listesini silmek istediğinize emin misiniz?</p>
<div class="confirm-actions">
    <button class="btn-cancel">İptal</button>
    <button class="btn-delete confirm-delete">Sil</button>
</div>
</div>
`;
        modal.appendChild(overlay);

        overlay.querySelector('.btn-cancel').onclick = () => overlay.remove();
        overlay.querySelector('.confirm-delete').onclick = async () => {
            try {
                await deleteList(oldName);
                showToast('success', 'Liste silindi');
                closeModal();
                // Animate removal
                listElement.style.transition = 'all 0.4s ease';
                listElement.style.opacity = '0';
                listElement.style.transform = 'translateY(-20px)';
                setTimeout(() => {
                    listElement.remove();
                    if (!document.querySelector('.list-wrapper')) {
                        showNoListsMessage(document.querySelector('.lists-container'));
                    }
                }, 450);
            } catch (err) {
                console.error(err);
                showToast('error', 'Silme işlemi başarısız');
            } finally {
                overlay.remove();
            }
        };
    };

    function showModal() {
        modal.style.display = 'flex';
        setTimeout(() => modal.classList.add('show'), 10);
    }
}

/*** Fetch & Render Lists ***/
async function initFavorites() {
    const container = document.querySelector('.lists-container');
    try {
        const lists = await fetchUserLists();
        if (!lists.length) {
            showNoListsMessage(container);
        } else {
            renderLists(lists, container);
            setupListInteractions();
        }
    } catch (err) {
        console.error(err);
        showError(container, err.message || 'Hata oluştu');
    }
}

async function fetchUserLists() {
    if (!localStorage.getItem('wdiUserToken')) {
        throw new Error('Oturum açmalısınız');
    }
    const res = await fetch('/api/saved/lists', {credentials: 'include'});
    if (!res.ok) throw new Error('Listeler alınamadı');
    const data = await res.json();
    return processListData(data);
}

function processListData(data) {
    if (!Array.isArray(data) || !data.length) return [];
    const map = {};
    data.forEach(item => {
        const name = item.ListName;
        if (!map[name]) map[name] = {name, videos: []};
        if (item.VideoID) {
            map[name].videos.push({
                id: item.VideoID,
                title: item.VideoName || 'Başlıksız Video',
                image: 'media/image/' + item.VideoID,
                year: item.Year || 'N/A',
                type: (item.Category || '').toString().split(',')[0] || 'Video'
            });
        }
    });
    return Object.values(map);
}

function renderLists(lists, container) {
    container.innerHTML = '';
    lists.forEach(list => {
        const wrapper = document.createElement('div');
        wrapper.className = 'list-wrapper';
        wrapper.dataset.listName = list.name;
        const count = list.videos.length;
        wrapper.innerHTML = `
<div class="list-header">
    <h3 class="list-title">
    <i class="fas fa-list"></i> ${escapeHtml(list.name)} <span class="video-count">(${count} video)</span>
</h3>
<div class="list-actions">
    <button class="list-action-btn edit-list-btn" title="Listeyi Düzenle"><i class="fas fa-edit"></i></button>
</div>
</div>
<div class="list-content">
    <div class="item-grid">${renderItems(list.videos)}</div>
</div>
    `;
        container.appendChild(wrapper);
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

/*** Interactions ***/
function setupListInteractions() {
    // Watch
    document.querySelectorAll('.watch-btn').forEach(btn =>
        btn.addEventListener('click', e => {
            e.stopPropagation();
            const id = e.currentTarget.closest('.content-item').dataset.id;
            window.location.href = `/watch?id=${id}`;
        })
    );
    // Click item
    document.querySelectorAll('.content-item').forEach(item =>
        item.addEventListener('click', e => {
            if (!e.target.closest('.content-buttons')) {
                const id = item.dataset.id;
                window.location.href = `/watch?id=${id}`;
            }
        })
    );
    // Remove
    document.querySelectorAll('.remove-btn').forEach(btn =>
        btn.addEventListener('click', async e => {
            e.stopPropagation();
            const item = e.currentTarget.closest('.content-item');
            try {
                await removeFromList(item.dataset.id);
                item.style.transition = 'all 0.3s';
                item.style.opacity = '0';
                item.style.transform = 'scale(0.8)';
                setTimeout(() => {
                    const grid = item.closest('.item-grid');
                    item.remove();
                    if (!grid.children.length) grid.innerHTML = renderItems([]);
                }, 300);
            } catch (err) {
                console.error(err);
                showToast('error', 'Çıkarılamadı');
            }
        })
    );
    // Edit list
    document.querySelectorAll('.edit-list-btn').forEach(btn =>
        btn.addEventListener('click', e => {
            const wrapper = e.currentTarget.closest('.list-wrapper');
            openEditModal(wrapper.dataset.listName, wrapper);
        })
    );
}

/*** Collapsible Lists ***/
function setupCollapsibleLists() {
    document.querySelectorAll('.list-content').forEach(content => {
        if (!content.querySelector('.list-content-inner')) {
            const inner = document.createElement('div');
            inner.className = 'list-content-inner';
            while (content.firstChild) inner.appendChild(content.firstChild);
            content.appendChild(inner);
        }
        content.style.height = '0';
        content.style.overflow = 'hidden';
        content.style.transition = 'height 0.3s ease';
    });

    document.querySelectorAll('.list-header').forEach(header => {
        if (!header.querySelector('.toggle-indicator')) {
            const icon = document.createElement('i');
            icon.className = 'fas fa-chevron-down toggle-indicator';
            header.insertBefore(icon, header.querySelector('.list-actions'));
        }
        header.addEventListener('click', e => {
            if (e.target.closest('.list-action-btn')) return;
            const wrapper = header.closest('.list-wrapper');
            const content = wrapper.querySelector('.list-content');
            const inner = content.querySelector('.list-content-inner');
            const isOpen = wrapper.classList.toggle('expanded');
            // close others
            document.querySelectorAll('.list-wrapper.expanded').forEach(other => {
                if (other !== wrapper) {
                    other.classList.remove('expanded');
                    other.querySelector('.list-content').style.height = '0';
                }
            });
            content.style.height = isOpen ? inner.scrollHeight + 'px' : '0';
        });
    });
}

/*** API Helpers ***/
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
        });
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
    if (data.Result !== 1) throw new Error(data.Message || 'Hata');
    return data;
}

/*** UI Helpers ***/
function showNoListsMessage(container) {
    container.innerHTML = `
      <div class="empty-list">
        <i class="fas fa-heart-broken"></i>
        <h3>Henüz istek listeniz yok</h3>
        <p>İzlerken "İstek Listesine Ekle" tıklayın.</p>
        <a href="/" class="btn-primary">Anasayfa</a>
      </div>
    `;
}

function showError(container, msg) {
    container.innerHTML = `
      <div class="empty-list">
        <i class="fas fa-exclamation-circle"></i>
        <h3>Hata</h3>
        <p>${msg}</p>
        <button class="btn-primary retry-btn"><i class="fas fa-redo"></i> Tekrar</button>
      </div>
    `;
    container.querySelector('.retry-btn').onclick = () => {
        container.innerHTML = `<div class="loading"><p>Yükleniyor...</p></div>`;
        initFavorites();
    };
}

function showToast(type, text) {
    let t = document.querySelector('.toast');
    if (!t) {
        t = document.createElement('div');
        t.className = 'toast';
        document.body.appendChild(t);
    }
    t.className = `toast ${type}`;
    t.innerHTML = `<i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'}"></i>${text}`;
    t.style.display = 'flex';
    setTimeout(() => t.style.opacity = '1', 10);
    setTimeout(() => {
        t.style.opacity = '0';
        setTimeout(() => t.style.display = 'none', 300);
    }, 3000);
}

function escapeHtml(str) {
    const d = document.createElement('div');
    d.textContent = str;
    return d.innerHTML;
}

/*** Inject basic toast CSS ***/
const style = document.createElement('style');
style.textContent = `
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
    .confirm-box { background: #fff; padding: 20px; border-radius: 8px; max-width: 300px; text-align: center; }
    .confirm-actions { margin-top: 15px; display: flex; justify-content: space-around; }
  `;
document.head.appendChild(style);
