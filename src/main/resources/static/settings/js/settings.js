import { initHeaderScroll } from '/homepage/js/headerScroll.js';

// Main initialization on DOMContentLoaded
document.addEventListener('DOMContentLoaded', () => {
    const loadingScreen = document.getElementById('loading-screen');
    if (loadingScreen) {
        loadingScreen.style.display = 'none';
    }

    initHeaderScroll();

    setupProfileSection()
        .then(() => {
            initSettingsNavigation();
            initPasswordForm();
            initNotificationToggles();
        })
        .catch(error => {
            console.error('Initialization error:', error);
        });
});

/**
 * Fetches user profile data and populates settings UI.
 * Returns a Promise that resolves when data is loaded or rejects on error.
 */
async function setupProfileSection() {
    const response = await fetch('/api/user/profile');
    if (!response.ok) {
        if (response.status === 401) {
            window.location.href = '/';
            return;
        }
        throw new Error('Failed to fetch profile data');
    }
    const data = await response.json();

    // Header/Sidebar info
    document.getElementById('user-avatar').innerText = (data.nickname || '').charAt(0).toUpperCase();
    document.getElementById('user-fullname').innerText = `${data.name} ${data.surname}`;
    document.getElementById('user-nickname').innerText = `@${data.nickname}`;

    // Form inputs
    document.getElementById('input-name').value = data.name || '';
    document.getElementById('input-surname').value = data.surname || '';
    document.getElementById('input-nickname').value = data.nickname || '';
    document.getElementById('input-email').value = data.email || '';

    initProfileForm();
}

function initProfileForm() {
    const profileForm = document.getElementById('profile-form');
    if (profileForm) {
        profileForm.addEventListener('submit', handleProfileUpdate);
    }
}

async function handleProfileUpdate(e) {
    e.preventDefault();
    const updateBtn = document.getElementById('update-profile-btn');
    const messageDiv = document.getElementById('profile-message');

    const name = document.getElementById('input-name').value.trim();
    const surname = document.getElementById('input-surname').value.trim();
    const nickname = document.getElementById('input-nickname').value.trim();
    const email = document.getElementById('input-email').value.trim();

    if (!name || !surname || !nickname || !email) {
        let missingField = "";
        if (!name) missingField = "Ad";
        else if (!surname) missingField = "Soyad";
        else if (!nickname) missingField = "Kullanıcı Adı";
        else if (!email) missingField = "E-posta";

        showButtonError(updateBtn, `${missingField} eksik!`);
        return;
    }

    const payload = { name, surname, nickname, email };

    try {
        updateBtn.disabled = true;
        updateBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Güncelleniyor...';
        const response = await fetch('/api/user/update-profile', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await response.json();

        if (response.ok && (data.Result || data.success)) {
            updateBtn.innerHTML = '<i class="fas fa-check"></i> Başarılı';
            // Update sidebar info
            document.getElementById('user-fullname').innerText = `${payload.name} ${payload.surname}`;
            document.getElementById('user-nickname').innerText = `@${payload.nickname}`;

            setTimeout(() => {
                updateBtn.disabled = false;
                updateBtn.innerHTML = 'Profil Bilgilerini Güncelle';
                updateBtn.style.backgroundColor = '';
            }, 3000);
        } else {
            showButtonError(updateBtn, data.message || 'Başarısız', 'Profil Bilgilerini Güncelle');
        }
    } catch (error) {
        console.error('Error updating profile:', error);
        showButtonError(updateBtn, 'Hata', 'Profil Bilgilerini Güncelle');
    }
}

/**
 * Initializes the settings navigation tabs behavior.
 */
function initSettingsNavigation() {
    const navItems = document.querySelectorAll('.settings-nav li');
    navItems.forEach(item => {
        item.addEventListener('click', () => {
            navItems.forEach(nav => nav.classList.remove('active'));
            item.classList.add('active');

            document.querySelectorAll('.settings-section').forEach(section => section.classList.remove('active'));
            const sectionId = `${item.dataset.section}-section`;
            document.getElementById(sectionId).classList.add('active');
        });
    });
}

/**
 * Sets up password update form handling.
 */
function initPasswordForm() {
    const passwordForm = document.getElementById('password-form');
    if (!passwordForm) {
        console.error('Password form not found');
        return;
    }
    passwordForm.addEventListener('submit', handlePasswordUpdate);
}

/**
 * Handles password update logic with validation and feedback.
 */
async function handlePasswordUpdate(e) {
    e.preventDefault();
    const newPasswordInput = document.getElementById('new-password');
    const confirmPasswordInput = document.getElementById('confirm-password');
    const updateBtn = document.getElementById('update-password-btn');

    const newPassword = newPasswordInput.value.trim();
    const confirmPassword = confirmPasswordInput.value.trim();

    if (!newPassword || !confirmPassword) {
        showButtonError(updateBtn, 'Tüm alanları doldurun');
        return;
    }
    if (newPassword !== confirmPassword) {
        showButtonError(updateBtn, 'Şifreler eşleşmiyor', 'Şifreyi Güncelle');
        return;
    }
    if (newPassword.length < 6) {
        showButtonError(updateBtn, 'Şifre çok kısa', 'Şifreyi Güncelle');
        return;
    }

    try {
        updateBtn.disabled = true;
        updateBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Güncelleniyor...';

        const response = await fetch('/api/user/update-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ newPassword })
        });
        const data = await response.json();

        if (response.ok && data.Result) {
            updateBtn.innerHTML = '<i class="fas fa-check"></i> Başarılı';
            resetPasswordInputs(newPasswordInput, confirmPasswordInput);
        } else {
            showButtonError(updateBtn, 'Başarısız', 'Şifreyi Güncelle');
        }
    } catch (error) {
        console.error('Error updating password:', error);
        showButtonError(updateBtn, 'Hata', 'Şifreyi Güncelle');
    }
}

function showButtonError(button, message, revertText) {
    const original = revertText || button.innerHTML;
    button.disabled = true;
    button.innerHTML = `<i class="fas fa-times"></i> ${message}`;
    button.style.backgroundColor = '#e74c3c';
    setTimeout(() => {
        button.innerHTML = original;
        button.style.backgroundColor = '';
        button.disabled = false;
    }, 3000);
}

function resetPasswordInputs(...inputs) {
    inputs.forEach(i => i.value = '');
    setTimeout(() => {
        const btn = document.getElementById('update-password-btn');
        btn.innerHTML = 'Şifreyi Güncelle';
        btn.style.backgroundColor = '#1ed760';
        btn.disabled = false;
    }, 2000);
}

/**
 * Initializes notification toggle buttons with permission handling.
 */
function initNotificationToggles() {
    const onBtn = document.getElementById('notifications-on');
    const offBtn = document.getElementById('notifications-off');
    if (!onBtn || !offBtn) {
        console.error('Notification toggle buttons not found');
        return;
    }

    checkNotificationStatus();

    onBtn.addEventListener('click', async () => {
        if (Notification.permission !== 'granted') {
            const perm = await Notification.requestPermission();
            if (perm === 'granted') setNotificationPreference(true);
            else {
                setNotificationPreference(false);
                alert('Bildirim izni reddedildi. Tarayıcı izinlerinden değiştirin.');
            }
        } else {
            setNotificationPreference(true);
        }
    });
    offBtn.addEventListener('click', () => {
        setNotificationPreference(false);
        if (Notification.permission === 'granted') {
            alert('Bildirimler devre dışı; tarayıcı izinleri açık.');
        }
    });
}

function checkNotificationStatus() {
    const pref = localStorage.getItem('wdiNotificationsEnabled') === 'true';
    if (!('Notification' in window)) {
        setNotificationUIState(false);
        document.getElementById('notifications-on').disabled = true;
        return;
    }
    setNotificationUIState(Notification.permission === 'granted' && pref);
}

function setNotificationPreference(enabled) {
    localStorage.setItem('wdiNotificationsEnabled', enabled);
    setNotificationUIState(enabled);
    // TODO: saveNotificationPreferenceToServer(enabled);
}

function setNotificationUIState(enabled) {
    document.getElementById('notifications-on').classList.toggle('active', enabled);
    document.getElementById('notifications-off').classList.toggle('active', !enabled);
}
