import { initHeaderScroll } from '/homepage/js/headerScroll.js';

document.addEventListener('DOMContentLoaded', () => {
    initHeaderScroll();
    setupProfileSection();
    setupAuthStatus();

    // Setup navigation
    const navItems = document.querySelectorAll('.settings-nav li');
    navItems.forEach(item => {
        item.addEventListener('click', () => {
            // Remove active class from all items
            navItems.forEach(nav => nav.classList.remove('active'));

            // Add active class to clicked item
            item.classList.add('active');

            // Hide all sections
            const sections = document.querySelectorAll('.settings-section');
            sections.forEach(section => section.classList.remove('active'));

            // Show selected section
            const sectionId = `${item.dataset.section}-section`;
            document.getElementById(sectionId).classList.add('active');
        });
    });

    // Setup password form
    const passwordForm = document.getElementById('password-form');
    passwordForm.addEventListener('submit', handlePasswordUpdate);

    // Setup loading screen
    const loadingScreen = document.getElementById('loading-screen');
    if (loadingScreen) {
        loadingScreen.style.display = 'none';
    }
});

// Setup notification toggle buttons with permission handling
document.addEventListener('DOMContentLoaded', () => {
    const notificationOnBtn = document.getElementById('notifications-on');
    const notificationOffBtn = document.getElementById('notifications-off');

    // Check current notification permission status and user preference
    checkNotificationStatus();

    notificationOnBtn.addEventListener('click', async () => {
        // Request permission if not granted yet
        if (Notification.permission !== 'granted') {
            const permission = await Notification.requestPermission();
            if (permission === 'granted') {
                setNotificationPreference(true);
            } else {
                // If permission denied or dismissed, keep toggle off
                setNotificationPreference(false);
                alert('Bildirim izni reddedildi. Bildirimleri açmak için tarayıcı izinlerini değiştirmeniz gerekmektedir.');
            }
        } else {
            // Permission already granted, just enable user preference
            setNotificationPreference(true);
        }
    });

    notificationOffBtn.addEventListener('click', () => {
        setNotificationPreference(false);

        if (Notification.permission === 'granted') {
            // Inform user that permission remains but notifications won't be shown
            alert('Bildirimler kullanıcı tercihinize göre devre dışı bırakıldı, ancak tarayıcı izni hala aktif. Tarayıcı izinlerini tamamen iptal etmek için tarayıcı ayarlarını kullanmanız gerekir.');
        }
    });

    // Check notification permission and update toggle state
    function checkNotificationStatus() {
        // Get user preference (default to false if not set)
        const userPreference = localStorage.getItem('wdiNotificationsEnabled') === 'true';

        if (!('Notification' in window)) {
            // Browser doesn't support notifications
            setNotificationUIState(false);
            notificationOnBtn.disabled = true;
            return;
        }

        // Set state based on both permission AND user preference
        if (Notification.permission === 'granted' && userPreference) {
            setNotificationUIState(true);
        } else {
            setNotificationUIState(false);
        }
    }

    // Helper function to update UI state and save preference
    function setNotificationPreference(enabled) {
        // Save user preference
        localStorage.setItem('wdiNotificationsEnabled', enabled);

        // Update UI
        setNotificationUIState(enabled);

        // Here you would also call API to update user preference in backend
        // Example: saveNotificationPreferenceToServer(enabled);
    }

    // Update only the UI state
    function setNotificationUIState(enabled) {
        if (enabled) {
            notificationOnBtn.classList.add('active');
            notificationOffBtn.classList.remove('active');
        } else {
            notificationOffBtn.classList.add('active');
            notificationOnBtn.classList.remove('active');
        }
    }
});

function setupAuthStatus() {
    const authCookie = getCookie('wdiAuth') || localStorage.getItem('wdiUserToken');
    const userNickname = localStorage.getItem('wdiUserNickname');

    if (!authCookie) {
        // Redirect to home if not authenticated
        window.location.href = '/';
        return;
    }

    // Create profile section in header
    const header = document.getElementById('header');
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

    // Setup dropdown functionality
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

    // Setup logout
    const logoutBtn = document.getElementById('logout-btn');
    logoutBtn.addEventListener('click', (e) => {
        e.preventDefault();
        localStorage.removeItem('wdiUserToken');
        localStorage.removeItem('wdiUserNickname');
        document.cookie = 'wdiAuth=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
        window.location.href = '/';
    });
}

async function setupProfileSection() {
    try {
        const response = await fetch('/api/user/profile');

        if (!response.ok) {
            if (response.status === 401) {
                // Unauthorized, redirect to home
                window.location.href = '/';
                return;
            }
            throw new Error('Failed to fetch profile data');
        }

        const data = await response.json();

        if (!data.Result) {
            throw new Error(data.Message || 'Failed to fetch user data');
        }

        // Update profile information
        document.getElementById('user-avatar').innerText = data.nickname.charAt(0).toUpperCase();
        document.getElementById('user-fullname').innerText = `${data.name} ${data.surname}`;
        document.getElementById('user-nickname').innerText = `@${data.nickname}`;
        document.getElementById('user-name').innerText = data.name;
        document.getElementById('user-surname').innerText = data.surname;
        document.getElementById('settings-nickname').innerText = data.nickname;
        document.getElementById('user-email').innerText = data.email;

    } catch (error) {
        console.error('Error fetching user data:', error);
    }
}

async function handlePasswordUpdate(e) {
    e.preventDefault();

    const newPasswordInput = document.getElementById('new-password');
    const confirmPasswordInput = document.getElementById('confirm-password');
    const updateBtn = document.getElementById('update-password-btn');

    const newPassword = newPasswordInput.value.trim();
    const confirmPassword = confirmPasswordInput.value.trim();

    // Validate inputs
    if (!newPassword || !confirmPassword) {
        updateBtn.innerHTML = '<i class="fas fa-times"></i> Tüm alanları doldurun';
        updateBtn.style.backgroundColor = '#e74c3c';
        setTimeout(() => {
            updateBtn.innerHTML = 'Şifreyi Güncelle';
            updateBtn.style.backgroundColor = '';
        }, 3000);
        return;
    }

    if (newPassword !== confirmPassword) {
        updateBtn.innerHTML = '<i class="fas fa-times"></i> Şifreler eşleşmiyor';
        updateBtn.style.backgroundColor = '#e74c3c';
        setTimeout(() => {
            updateBtn.innerHTML = 'Şifreyi Güncelle';
            updateBtn.style.backgroundColor = '';
        }, 3000);
        return;
    }

    if (newPassword.length < 6) {
        updateBtn.innerHTML = '<i class="fas fa-times"></i> Şifre çok kısa';
        updateBtn.style.backgroundColor = '#e74c3c';
        setTimeout(() => {
            updateBtn.innerHTML = 'Şifreyi Güncelle';
            updateBtn.style.backgroundColor = '';
        }, 3000);
        return;
    }

    try {
        // Show loading state
        updateBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Güncelleniyor...';

        const response = await fetch('/api/user/update-password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ newPassword })
        });

        const data = await response.json();

        if (response.ok && data.Result) {
            updateBtn.innerHTML = '<i class="fas fa-check"></i> Başarılı';
            updateBtn.style.backgroundColor = '#1ed760';
            newPasswordInput.value = '';
            confirmPasswordInput.value = '';
        } else {
            updateBtn.innerHTML = '<i class="fas fa-times"></i> Başarısız';
            updateBtn.style.backgroundColor = '#e74c3c';
            newPasswordInput.value = '';
            confirmPasswordInput.value = '';
        }

        setTimeout(() => {
            updateBtn.innerHTML = 'Şifreyi Güncelle';
            updateBtn.style.backgroundColor = '';
            updateBtn.disabled = false;
        }, 3000);
    } catch (error) {
        updateBtn.innerHTML = '<i class="fas fa-times"></i> Hata';
        updateBtn.style.backgroundColor = '#e74c3c';
        messageElement.innerText = 'Bir hata oluştu, lütfen tekrar deneyin';
        messageElement.classList.add('error');
        console.error('Error updating password:', error);

        setTimeout(() => {
            updateBtn.innerHTML = 'Şifreyi Güncelle';
            updateBtn.style.backgroundColor = '';
            updateBtn.disabled = false;
        }, 3000);
    }
}

// Helper function to get cookie value
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}