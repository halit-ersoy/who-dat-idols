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
            setupAuthStatus();
            initSettingsNavigation();
            initPasswordForm();
            initNotificationToggles();
        })
        .catch(error => {
            console.error('Initialization error:', error);
        });
});

/**
 * Sets up authentication status: redirects if unauthenticated,
 * and builds the profile dropdown in header.
 */
function setupAuthStatus() {
    const authCookie = getCookie('wdiAuth') || localStorage.getItem('wdiUserToken');
    const userNickname = localStorage.getItem('wdiUserNickname');

    if (!authCookie) {
        window.location.href = '/';
        return;
    }

    // Find header element (fallback to <header> tag)
    const header = document.getElementById('header') || document.querySelector('header');
    if (!header) {
        console.error('Header element not found for setupAuthStatus');
        return;
    }

    const profileSection = document.createElement('div');
    profileSection.className = 'profile-section';
    profileSection.innerHTML = `
        <button class="profile-btn" aria-label="Profil">
            <span class="profile-avatar">${userNickname ? userNickname.charAt(0).toUpperCase() : 'U'}</span>
            <span class="profile-name">${userNickname || 'User'}</span>
            <i class="fas fa-chevron-down"></i>
        </button>
        <div class="profile-dropdown">
            <a href="/profile"><i class="fas fa-user"></i> Profilim</a>
            <a href="/favorites"><i class="fas fa-heart"></i> Favorilerim</a>
            <a href="/settings"><i class="fas fa-cog"></i> Ayarlar</a>
            <a href="#" id="logout-btn"><i class="fas fa-sign-out-alt"></i> Çıkış Yap</a>
        </div>
    `;
    header.appendChild(profileSection);

    const profileBtn = profileSection.querySelector('.profile-btn');
    const dropdown = profileSection.querySelector('.profile-dropdown');

    profileBtn.addEventListener('click', () => {
        dropdown.classList.toggle('active');
    });

    document.addEventListener('click', (e) => {
        if (!profileBtn.contains(e.target) && !dropdown.contains(e.target)) {
            dropdown.classList.remove('active');
        }
    });

    document.getElementById('logout-btn').addEventListener('click', (e) => {
        e.preventDefault();
        localStorage.removeItem('wdiUserToken');
        localStorage.removeItem('wdiUserNickname');
        document.cookie = 'wdiAuth=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
        window.location.href = '/';
    });
}

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
    if (!data.Result) {
        throw new Error(data.Message || 'No user data');
    }

    document.getElementById('user-avatar').innerText = data.nickname.charAt(0).toUpperCase();
    document.getElementById('user-fullname').innerText = `${data.name} ${data.surname}`;
    document.getElementById('user-nickname').innerText = `@${data.nickname}`;
    document.getElementById('user-name').innerText = data.name;
    document.getElementById('user-surname').innerText = data.surname;
    document.getElementById('settings-nickname').innerText = data.nickname;
    document.getElementById('user-email').innerText = data.email;
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
        showButtonError(updateBtn, 'Şifreler eşleşmiyor');
        return;
    }
    if (newPassword.length < 6) {
        showButtonError(updateBtn, 'Şifre çok kısa');
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
            showButtonError(updateBtn, 'Başarısız');
        }
    } catch (error) {
        console.error('Error updating password:', error);
        showButtonError(updateBtn, 'Hata');
    }
}

function showButtonError(button, message) {
    const original = button.innerHTML;
    button.innerHTML = `<i class="fas fa-times"></i> ${message}`;
    button.style.backgroundColor = '#e74c3c';
    setTimeout(() => {
        button.innerHTML = original || 'Şifreyi Güncelle';
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

/**
 * Helper to read a cookie by name.
 */
function getCookie(name) {
    const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    return match ? match[2] : null;
}