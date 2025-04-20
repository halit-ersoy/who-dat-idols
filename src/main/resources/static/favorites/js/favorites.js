// favorites.js
document.addEventListener('DOMContentLoaded', () => {
    initFavorites();

    // Add event listener for create list button
    const createListBtn = document.getElementById('createListBtn');
    if (createListBtn) {
        createListBtn.addEventListener('click', openCreateListModal);
    }
});

function openCreateListModal() {
    const modal = document.getElementById('listManageModal');
    const modalTitle = modal.querySelector('.modal-header h3');
    const nameInput = document.getElementById('editListName');
    const saveBtn = modal.querySelector('.rename-list');
    const deleteBtn = modal.querySelector('.btn-delete');
    const cancelBtn = modal.querySelector('.btn-cancel');
    const closeBtn = modal.querySelector('.close-modal');

    // Update modal for creation mode
    modalTitle.textContent = 'Yeni Liste Oluştur';
    nameInput.value = '';
    nameInput.placeholder = 'Liste adını giriniz';
    saveBtn.textContent = 'Oluştur';
    deleteBtn.style.display = 'none'; // Hide delete button in create mode

    // Show modal with animation
    modal.style.display = 'flex';
    setTimeout(() => modal.classList.add('show'), 10);

    // Setup modal interactions
    const closeModal = () => {
        modal.classList.remove('show');
        setTimeout(() => modal.style.display = 'none', 300);
        deleteBtn.style.display = 'block'; // Restore delete button visibility
    };

    closeBtn.onclick = closeModal;
    cancelBtn.onclick = closeModal;

    // Create new list
    saveBtn.onclick = async () => {
        const listName = nameInput.value.trim();
        if (!listName) {
            showToast('error', 'Liste adı boş olamaz');
            return;
        }

        try {
            await createNewList(listName);
            showToast('success', 'Liste başarıyla oluşturuldu');
            closeModal();

            // Refresh the lists to show the new one
            initFavorites();
        } catch (error) {
            console.error('Error creating list:', error);
            showToast('error', 'Liste oluşturulamadı');
        }
    };
}

async function createNewList(listName) {
    const res = await fetch(`/api/saved/create?title=${encodeURIComponent(listName)}`, {
        method: 'POST',
        credentials: 'include'
    });

    const data = await res.json();
    if (data.Result !== 1) {
        throw new Error(data.Message || 'İşlem başarısız oldu');
    }

    return data;
}

async function initFavorites() {
    const container = document.querySelector('.lists-container');

    try {
        const lists = await fetchUserLists();
        if (!lists || !lists.length) {
            showNoListsMessage(container);
            return;
        }

        renderLists(lists, container);
        setupListInteractions();
    } catch (error) {
        console.error('Error loading favorites:', error);
        showError(container, error.message || 'Listeler yüklenirken bir hata oluştu.');
    }
}

async function fetchUserLists() {
    const token = localStorage.getItem('wdiUserToken');
    if (!token) {
        throw new Error('Oturum açmalısınız');
    }

    const res = await fetch('/api/saved/lists', {credentials: 'include'});
    if (!res.ok) {
        throw new Error('Listeler alınamadı');
    }

    const data = await res.json();
    return processListData(data);
}

function processListData(data) {
    if (!Array.isArray(data) || data.length === 0) {
        return [];
    }

    // Group items by list name
    const groupedLists = data.reduce((acc, item) => {
        const name = item.ListName;
        if (!acc[name]) {
            acc[name] = {
                name,
                videos: []
            };
        }

        if (item.VideoID) {
            acc[name].videos.push({
                id: item.VideoID,
                title: item.VideoName || 'Başlıksız Video',
                image: item.PosterURL || '/homepage/img/default-poster.jpg',
                year: item.Year || 'N/A',
                type: item.Category.toString().split(',')[0] || 'Video'
            });
        }

        return acc;
    }, {});

    return Object.values(groupedLists);
}

function renderLists(lists, container) {
    container.innerHTML = '';

    lists.forEach(list => {
        const listElement = document.createElement('div');
        listElement.className = 'list-wrapper';
        listElement.dataset.listName = list.name;

        const videoCount = list.videos.length;

        listElement.innerHTML = `
            <div class="list-header">
                <h3 class="list-title">
                    <i class="fas fa-list"></i> 
                    ${escapeHtml(list.name)} 
                    <span class="video-count">(${videoCount} ${videoCount === 1 ? 'video' : 'video'})</span>
                </h3>
                <div class="list-actions">
                    <button class="list-action-btn edit-list-btn" title="Listeyi Düzenle">
                        <i class="fas fa-edit"></i>
                    </button>
                </div>
            </div>
            <div class="list-content">
                <div class="item-grid">
                    ${renderListItems(list.videos)}
                </div>
            </div>
        `;

        container.appendChild(listElement);
    });
}

function renderListItems(videos) {
    if (!videos.length) {
        return `
            <div class="empty-list">
                <i class="fas fa-film"></i>
                <p>Bu listede henüz bir video bulunmuyor.</p>
            </div>
        `;
    }

    return videos.map(video => `
        <div class="content-item" data-id="${video.id}">
            <img src="${video.image}" alt="${escapeHtml(video.title)}">
            <div class="content-overlay">
                <h4 class="content-title">${escapeHtml(video.title)}</h4>
                <div class="content-meta">
                    <span>${video.year}</span>
                    <span>${video.type}</span>
                </div>
            </div>
            <div class="content-buttons">
                <button class="content-btn watch-btn" title="İzle">
                    <i class="fas fa-play"></i>
                </button>
                <button class="content-btn remove-btn" title="Listeden Çıkar">
                    <i class="fas fa-times"></i>
                </button>
            </div>
        </div>
    `).join('');
}

function setupListInteractions() {
    // Watch content
    document.querySelectorAll('.watch-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const item = e.target.closest('.content-item');
            const videoId = item.dataset.id;
            window.location.href = `/watch?id=${videoId}`;
        });
    });

    // Click on content item to watch
    document.querySelectorAll('.content-item').forEach(item => {
        item.addEventListener('click', (e) => {
            if (!e.target.closest('.content-buttons')) {
                const videoId = item.dataset.id;
                window.location.href = `/watch?id=${videoId}`;
            }
        });
    });

    // Remove from list
    document.querySelectorAll('.remove-btn').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            e.stopPropagation();
            const item = e.target.closest('.content-item');
            const videoId = item.dataset.id;

            try {
                await removeFromList(videoId);

                // Add remove animation
                item.style.transition = 'all 0.3s ease';
                item.style.transform = 'scale(0.8)';
                item.style.opacity = '0';

                setTimeout(() => {
                    item.remove();

                    // Check if list is now empty
                    const listElement = item.closest('.list-wrapper');
                    const grid = listElement.querySelector('.item-grid');
                    if (!grid.children.length || (grid.children.length === 1 && grid.children[0].classList.contains('empty-list'))) {
                        grid.innerHTML = `
                            <div class="empty-list">
                                <i class="fas fa-film"></i>
                                <p>Bu listede henüz bir video bulunmuyor.</p>
                            </div>
                        `;
                    }
                }, 300);
            } catch (error) {
                console.error('Error removing from list:', error);
                showToast('error', 'Video listeden çıkarılamadı.');
            }
        });
    });

    // Edit list
    document.querySelectorAll('.edit-list-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const listElement = e.target.closest('.list-wrapper');
            const listName = listElement.dataset.listName;
            openEditModal(listName, listElement);
        });
    });
}

function openEditModal(listName, listElement) {
    const modal = document.getElementById('listManageModal');
    const modalTitle = modal.querySelector('.modal-header h3');
    const nameInput = document.getElementById('editListName');
    const saveBtn = modal.querySelector('.rename-list');
    const deleteBtn = modal.querySelector('.btn-delete');
    const cancelBtn = modal.querySelector('.btn-cancel');
    const closeBtn = modal.querySelector('.close-modal');

    // Update modal for edit mode
    modalTitle.textContent = 'Listeyi Düzenle';
    nameInput.value = listName;
    saveBtn.textContent = 'Kaydet';
    deleteBtn.style.display = 'block';

    // Show modal with animation
    modal.style.display = 'flex';
    setTimeout(() => modal.classList.add('show'), 10);

    // Close modal functions
    const closeModal = () => {
        modal.classList.remove('show');
        setTimeout(() => modal.style.display = 'none', 300);
    };

    closeBtn.onclick = closeModal;
    cancelBtn.onclick = closeModal;

    // Set up save button for renaming
    saveBtn.onclick = async () => {
        const newName = nameInput.value.trim();
        if (newName && newName !== listName) {
            try {
                const result = await renameList(listName, newName);
                if (result.Result === 1) {
                    showToast('success', 'Liste adı başarıyla değiştirildi');
                    // Update the list title in the DOM
                    const titleElement = listElement.querySelector('.list-title');
                    titleElement.textContent = escapeHtml(newName);
                    closeModal();
                    // Reload the page to reflect changes
                    setTimeout(() => location.reload(), 1000);
                } else {
                    showToast('error', result.Message || 'Liste adı değiştirilemedi');
                }
            } catch (error) {
                showToast('error', 'Liste adı değiştirilemedi: ' + error.message);
            }
        }
    };

    // Set up delete button - This was missing or not working
    deleteBtn.onclick = async () => {
        if (confirm(`"${listName}" listesini silmek istediğinizden emin misiniz?`)) {
            try {
                const result = await deleteList(listName);
                if (result.Result === 1) {
                    showToast('success', 'Liste başarıyla silindi');
                    closeModal();

                    // Remove the list from DOM
                    listElement.style.opacity = '0';
                    listElement.style.transform = 'translateY(-20px)';
                    setTimeout(() => {
                        listElement.remove();

                        // Check if there are any lists left
                        const remainingLists = document.querySelectorAll('.list-wrapper');
                        if (remainingLists.length === 0) {
                            showNoListsMessage(document.querySelector('.lists-container'));
                        }
                    }, 300);
                } else {
                    showToast('error', result.Message || 'Liste silinemedi');
                }
            } catch (error) {
                showToast('error', 'Liste silinemedi: ' + error.message);
            }
        }
    };

// Rename list
    renameBtn.onclick = async () => {
        const newName = nameInput.value.trim();
        if (!newName || newName === listName) {
            closeModal();
            return;
        }

        try {
            await renameList(listName, newName);

            // Update UI
            listElement.dataset.listName = newName;
            const titleEl = listElement.querySelector('.list-title');
            titleEl.innerHTML = `<i class="fas fa-list"></i> ${escapeHtml(newName)} ${titleEl.innerHTML.split('</i>')[1].split('</span>')[1]}</span>`;

            showToast('success', 'Liste adı güncellendi');
            closeModal();
        } catch (error) {
            console.error('Error renaming list:', error);
            showToast('error', 'Liste adı güncellenemedi');
        }
    };

// Delete list
    deleteBtn.onclick = () => {
        // Create confirmation overlay
        const overlay = document.createElement('div');
        overlay.className = 'confirm-overlay';
        overlay.innerHTML = `
            <div class="confirm-box">
                <h3>Listeyi Sil</h3>
                <p>"${escapeHtml(listName)}" listesi silinecek. Emin misiniz?</p>
                <div class="confirm-actions">
                    <button class="btn-cancel">İptal</button>
                    <button class="btn-delete confirm-delete">Sil</button>
                </div>
            </div>
        `;

        modal.appendChild(overlay);

        // Setup confirmation interactions
        overlay.querySelector('.btn-cancel').onclick = () => {
            overlay.remove();
        };

        overlay.querySelector('.confirm-delete').onclick = async () => {
            try {
                await deleteList(listName);

                // Remove list from UI with animation
                listElement.style.transition = 'all 0.5s ease';
                listElement.style.height = listElement.offsetHeight + 'px';
                listElement.style.opacity = '0';
                listElement.style.transform = 'translateY(-20px)';

                setTimeout(() => {
                    listElement.style.height = '0';
                    listElement.style.margin = '0';
                    listElement.style.padding = '0';

                    setTimeout(() => {
                        listElement.remove();

                        // Check if there are no more lists
                        const container = document.querySelector('.lists-container');
                        if (!container.querySelector('.list-wrapper')) {
                            showNoListsMessage(container);
                        }
                    }, 300);
                }, 300);

                showToast('success', 'Liste silindi');
                closeModal();
            } catch (error) {
                console.error('Error deleting list:', error);
                showToast('error', 'Liste silinemedi');
                overlay.remove();
            }
        };
    }
}

async function removeFromList(videoId) {
    const res = await fetch(`/api/saved/remove?videoId=${videoId}`, {
        method: 'POST',
        credentials: 'include'
    });

    const data = await res.json();
    if (data.Result !== 1) {
        throw new Error(data.Message || 'İşlem başarısız oldu');
    }

    return data;
}

async function renameList(oldName, newName) {
    const res = await fetch(`/api/saved/rename-list?title=${encodeURIComponent(oldName)}&newTitle=${encodeURIComponent(newName)}`, {
        method: 'POST',
        credentials: 'include'
    });

    const data = await res.json();
    if (data.Result !== 1) {
        throw new Error(data.Message || 'İşlem başarısız oldu');
    }

    return data;
}

async function deleteList(listName) {
    try {
        const formData = new FormData();
        formData.append('title', listName);

        const response = await fetch('/api/saved/delete-list', {
            method: 'POST',
            body: formData,
            credentials: 'include'
        });

        if (!response.ok) {
            throw new Error(`Server error: ${response.status}`);
        }

        return await response.json();
    } catch (error) {
        console.error('Error deleting list:', error);
        throw error;
    }
}

function showNoListsMessage(container) {
    container.innerHTML = `
        <div class="empty-list">
            <i class="fas fa-heart-broken"></i>
            <h3>Henüz bir istek listeniz bulunmuyor</h3>
            <p>İçerikleri izlerken "İstek Listesine Ekle" butonuna tıklayarak favorilerinize ekleyebilirsiniz.</p>
            <a href="/" class="btn-primary" style="display: inline-block; margin-top: 15px; padding: 10px 20px; background: #1ed760; color: #000; text-decoration: none; border-radius: 8px; font-weight: 600;">
                <i class="fas fa-home"></i> Anasayfaya Git
            </a>
        </div>
    `;
}

function showError(container, message) {
    container.innerHTML = `
        <div class="empty-list">
            <i class="fas fa-exclamation-circle"></i>
            <h3>Bir hata oluştu</h3>
            <p>${message}</p>
            <button class="btn-primary retry-btn" style="margin-top: 15px; padding: 10px 20px; background: #1ed760; color: #000; border: none; border-radius: 8px; font-weight: 600; cursor: pointer;">
                <i class="fas fa-redo"></i> Tekrar Dene
            </button>
        </div>
    `;

    container.querySelector('.retry-btn').addEventListener('click', () => {
        container.innerHTML = `
            <div class="loading-spinner">
                <div class="spinner"></div>
                <p>Listeleriniz yükleniyor...</p>
            </div>
        `;
        initFavorites();
    });
}

function showToast(type, message) {
    let toast = document.querySelector('.toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.className = 'toast';
        document.body.appendChild(toast);
    }

    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'}"></i>
        ${message}
    `;

    toast.style.display = 'flex';
    toast.style.opacity = '0';

    // Add animation
    setTimeout(() => {
        toast.style.transform = 'translateY(0)';
        toast.style.opacity = '1';
    }, 10);

    // Auto remove
    setTimeout(() => {
        toast.style.transform = 'translateY(-20px)';
        toast.style.opacity = '0';
        setTimeout(() => {
            toast.style.display = 'none';
        }, 300);
    }, 3000);
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Add toast styles
const style = document.createElement('style');
style.textContent = `
    .toast {
        position: fixed;
        top: 20px;
        right: 20px;
        background: #333;
        color: #fff;
        padding: 12px 20px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        gap: 10px;
        z-index: 2000;
        box-shadow: 0 5px 15px rgba(0,0,0,0.3);
        transform: translateY(-20px);
        opacity: 0;
        transition: all 0.3s ease;
    }
    
    .toast.success {
        background: rgba(30, 215, 96, 0.9);
        color: #000;
    }
    
    .toast.error {
        background: rgba(255, 70, 70, 0.9);
    }
`;
document.head.appendChild(style);