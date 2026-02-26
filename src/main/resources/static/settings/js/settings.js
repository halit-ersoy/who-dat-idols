import { initHeaderScroll } from '/homepage/js/headerScroll.js';

// Main initialization on DOMContentLoaded
document.addEventListener('DOMContentLoaded', () => {
    const loadingScreen = document.getElementById('loading-screen');
    if (loadingScreen) {
        loadingScreen.style.display = 'none';
    }

    initHeaderScroll();

    setupProfileSection()
        .then((userData) => {
            initSettingsNavigation();
            initPasswordForm();
            initNotificationToggles();
            initMessageToggles(userData.allowMessages);
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

    document.getElementById('input-name').value = data.name || '';
    document.getElementById('input-surname').value = data.surname || '';
    document.getElementById('input-nickname').value = data.nickname || '';
    document.getElementById('input-email').value = data.email || '';

    // Verification UI
    const verificationContainer = document.getElementById('verification-status-container');
    if (verificationContainer) {
        if (data.isVerified) {
            verificationContainer.innerHTML = '<div class="verification-badge"><i class="fas fa-check-circle"></i> Doğrulanmış Hesap</div>';
        } else {
            verificationContainer.innerHTML = '<button type="button" id="request-verification-btn" class="settings-btn" style="padding: 10px 20px; font-size: 0.8rem; margin: 0; width: auto; border-radius: 12px;">Hesabımı Doğrula</button>';
        }
    }

    initProfileForm();
    initVerification();
    return data;
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

    const nicknameRegex = /^[a-zA-Z0-9_.]+$/;
    if (!nicknameRegex.test(nickname)) {
        showButtonError(updateBtn, 'Geçersiz Karakter!', 'Profil Bilgilerini Güncelle');
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
 * Initializes the email verification form and modal logic.
 */
function initVerification() {
    const requestBtn = document.getElementById('request-verification-btn');
    const modal = document.getElementById('verification-modal');
    const closeBtn = document.getElementById('close-verification-modal');
    const submitBtn = document.getElementById('submit-verification-btn');
    const input = document.getElementById('verification-code-input');
    const message = document.getElementById('verification-message');

    if (requestBtn) {
        requestBtn.addEventListener('click', async () => {
            const originalText = requestBtn.innerHTML;
            requestBtn.disabled = true;
            requestBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Gönderiliyor...';

            try {
                const response = await fetch('/api/user/request-verification', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' }
                });
                const data = await response.json();

                if (response.ok && data.success) {
                    requestBtn.innerHTML = '<i class="fas fa-check"></i> Gönderildi';
                    setTimeout(() => {
                        modal.style.display = 'flex';
                        requestBtn.innerHTML = originalText;
                        requestBtn.disabled = false;
                    }, 1000);
                } else {
                    showButtonError(requestBtn, data.message || 'Başarısız', originalText);
                }
            } catch (error) {
                console.error('Error requesting verification:', error);
                showButtonError(requestBtn, 'Sunucu Hatası', originalText);
            }
        });
    }

    if (closeBtn) {
        closeBtn.addEventListener('click', () => {
            modal.style.display = 'none';
            if (message) message.style.display = 'none';
            if (input) input.value = '';
        });
    }

    // Close modal on outside click
    window.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.style.display = 'none';
            if (message) message.style.display = 'none';
            if (input) input.value = '';
        }
    });

    if (submitBtn) {
        submitBtn.addEventListener('click', async () => {
            const code = input.value.trim();
            if (!code || code.length !== 6) {
                showButtonError(submitBtn, '6 haneli kodu girin', 'Doğrula');
                return;
            }

            const originalText = submitBtn.innerHTML;
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Doğrulanıyor...';
            if (message) message.style.display = 'none';

            try {
                const response = await fetch('/api/user/verify-email', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ code })
                });
                const data = await response.json();

                if (response.ok && data.success) {
                    submitBtn.innerHTML = '<i class="fas fa-check"></i> Doğrulandı';
                    submitBtn.style.backgroundColor = '#1ed760';

                    setTimeout(() => {
                        modal.style.display = 'none';
                        // Refresh the UI by fetching profile again, or just reload the page for simplicity
                        window.location.reload();
                    }, 1500);
                } else {
                    showButtonError(submitBtn, data.message || 'Geçersiz kod', originalText);
                }
            } catch (error) {
                console.error('Error verifying code:', error);
                showButtonError(submitBtn, 'Sunucu Hatası', originalText);
            }
        });
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

/**
 * Initializes direct message toggle buttons.
 */
function initMessageToggles(initialState) {
    const onBtn = document.getElementById('messages-on');
    const offBtn = document.getElementById('messages-off');

    if (!onBtn || !offBtn) return;

    setMessageUIState(initialState);

    onBtn.addEventListener('click', () => setMessagePreference(true));
    offBtn.addEventListener('click', () => setMessagePreference(false));
}

async function setMessagePreference(enabled) {
    try {
        const response = await fetch('/api/user/settings/toggle-messages', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ allow: enabled })
        });

        if (response.ok) {
            setMessageUIState(enabled);
        } else {
            alert('Ayarlar güncellenirken bir hata oluştu.');
        }
    } catch (error) {
        console.error('Error toggling messages:', error);
    }
}

function setMessageUIState(enabled) {
    // Backend returns bit/boolean, sometimes 1/0
    const isEnabled = enabled === true || enabled === 1 || enabled === "true";

    const onBtn = document.getElementById('messages-on');
    const offBtn = document.getElementById('messages-off');

    if (onBtn) onBtn.classList.toggle('active', isEnabled);
    if (offBtn) offBtn.classList.toggle('active', !isEnabled);

    // Sync header icon visibility
    const messagesWrapper = document.getElementById('messages-wrapper');
    if (messagesWrapper) {
        messagesWrapper.style.display = isEnabled ? 'block' : 'none';
    }
}
