// login.js

export function initLogin() {
    // DOM Element Referansları
    const loginBtn = document.querySelector('.login-btn');
    const loginLink = document.getElementById('login-link');
    const loginModal = document.getElementById('login-modal');
    const closeLoginModal = document.getElementById('close-modal');
    const loginSubmit = document.getElementById('login-submit');
    const formInputs = document.querySelectorAll('.form-control');

    // Kullanıcı giriş durumunu kontrol edip profil oluşturma
    // Kullanıcı giriş durumunu kontrol edip profil oluşturma
    async function checkAuthStatus() {
        const authCookie = localStorage.getItem('wdiUserToken');
        const userNickname = localStorage.getItem('wdiUserNickname');

        if (authCookie && userNickname) {
            try {
                const response = await fetch('/api/user/profile');
                if (response.ok) {
                    const data = await response.json();

                    // IF user is found but banned, clear everything and logout
                    if (data.isBanned) {
                        handleLogout();
                        return;
                    }

                    const profileSection = document.createElement('div');
                    profileSection.className = 'profile-section';
                    profileSection.innerHTML = `
                        <button class="profile-btn" aria-label="Profil">
                            <span class="profile-avatar">${userNickname.charAt(0).toUpperCase()}</span>
                            <span class="profile-name">${userNickname}</span>
                            <i class="fas fa-chevron-down"></i>
                        </button>
                        <div class="profile-dropdown">
                            <a href="/profile"><i class="fas fa-user"></i> Profilim</a>
                            <a href="/favorites"><i class="fas fa-heart"></i> Favorilerim</a>
                            <a href="/settings"><i class="fas fa-cog"></i> Ayarlar</a>
                            <a href="#" id="feedback-open-btn"><i class="fas fa-comment-dots"></i> Geri Bildirim</a>
                            <a href="#" id="logout-btn"><i class="fas fa-sign-out-alt"></i> Çıkış Yap</a>
                        </div>
                    `;
                    loginBtn.parentNode.replaceChild(profileSection, loginBtn);

                    const profileBtn = profileSection.querySelector('.profile-btn');
                    const dropdown = profileSection.querySelector('.profile-dropdown');
                    if (profileBtn && dropdown) {
                        profileBtn.addEventListener('click', () => {
                            dropdown.classList.toggle('active');
                        });
                        document.addEventListener('click', (e) => {
                            if (!profileBtn.contains(e.target) && !dropdown.contains(e.target)) {
                                dropdown.classList.remove('active');
                            }
                        });
                    }

                    const logoutBtn = document.getElementById('logout-btn');
                    if (logoutBtn) {
                        logoutBtn.addEventListener('click', (e) => {
                            e.preventDefault();
                            handleLogout();
                        });
                    }
                } else if (response.status === 401 || response.status === 404) {
                    // Cookie invalid or not found in DB
                    handleLogout();
                }
            } catch (error) {
                console.error('Auth verification failed:', error);
            }
        }
    }

    function handleLogout() {
        localStorage.removeItem('wdiUserToken');
        localStorage.removeItem('wdiUserNickname');
        document.cookie = 'wdiAuth=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
        window.location.reload();
    }

    checkAuthStatus();

    // Login modal açma
    loginBtn.addEventListener('click', (e) => {
        e.preventDefault();
        loginModal.classList.add('active');
        document.body.style.overflow = 'hidden';
        setTimeout(() => {
            const emailInput = document.getElementById('email');
            if (emailInput) emailInput.focus();
        }, 400);
    });

    // Login modal kapatma
    closeLoginModal.addEventListener('click', () => {
        loginModal.classList.remove('active');
        document.body.style.overflow = '';

        // Reset form fields
        document.getElementById('email').value = '';
        document.getElementById('password').value = '';

        // Reset any error styling
        loginSubmit.innerHTML = 'Giriş Yap';
        loginSubmit.style.backgroundColor = '';
        loginSubmit.classList.remove('loading');

        // Reset form field highlighting
        formInputs.forEach(input => {
            input.value = '';
            input.parentElement.classList.remove('active');
        });
    });

    // Form input odaklanma/çıkma işlemleri
    formInputs.forEach(input => {
        input.addEventListener('focus', function () {
            this.parentElement.classList.add('active');
        });
        input.addEventListener('blur', function () {
            if (!this.value) {
                this.parentElement.classList.remove('active');
            }
        });
    });

    // Giriş Yapma İşlemleri (login submit)
    loginSubmit.addEventListener('click', async function () {
        const usernameOrEmailValue = document.getElementById('email').value;
        const passwordValue = document.getElementById('password').value;

        if (!usernameOrEmailValue || !passwordValue) {
            this.innerHTML = '<i class="fas fa-times"></i> Tüm alanları doldurun';
            this.style.backgroundColor = '#e74c3c';
            setTimeout(() => {
                this.innerHTML = 'Giriş Yap';
                this.style.backgroundColor = '';
            }, 3000);
            return;
        }

        this.classList.add('loading');
        this.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        const loginData = { usernameOrEmail: usernameOrEmailValue, password: passwordValue };

        // Update this part of the login.js file
        try {
            const response = await fetch('/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(loginData)
            });

            const data = await response.json();

            // Backend'den gelen 'success' değeri boolean veya 1/0 olabilir
            const isSuccess = data.success === true || data.success === 1 || data.success === "true";

            if (isSuccess) {
                this.classList.remove('loading');
                this.innerHTML = '<i class="fas fa-check"></i> Başarılı';
                this.style.backgroundColor = '#1ed760';
                localStorage.setItem('wdiUserToken', data.cookie);
                localStorage.setItem('wdiUserNickname', data.nickname);

                setTimeout(() => {
                    loginModal.classList.remove('active');
                    document.body.style.overflow = '';
                    document.getElementById('email').value = '';
                    document.getElementById('password').value = '';
                    window.location.reload();
                }, 1500);
            } else {
                this.classList.remove('loading');

                // Clear existing ban reason if any
                const oldReason = document.getElementById('ban-reason-msg');
                if (oldReason) oldReason.remove();

                const isBannedFlag = data.isBanned || String(data.isBanned) === "1" || String(data.isBanned) === "true";
                const isBanMessage = data.message && (data.message.includes('askıya') || data.message.includes('yasak') || data.message.includes('istesi'));
                const notWrongCredentials = data.message && !data.message.includes('Hatali') && !data.message.includes('doldurun') && data.message !== 'Hata';

                if (isBannedFlag || isBanMessage || notWrongCredentials) {
                    this.innerHTML = '<i class="fas fa-ban"></i> Yasaklı Kullanıcı';
                    this.style.backgroundColor = '#000000';
                    this.style.color = '#ffffff';

                    // Provide reason from banReason, or fallback to message
                    const reason = data.banReason || data.message || 'Belirtilmedi';

                    const reasonEl = document.createElement('div');
                    reasonEl.id = 'ban-reason-msg';
                    reasonEl.style.color = '#ff4b4b';
                    reasonEl.style.fontSize = '14px';
                    reasonEl.style.marginTop = '10px';
                    reasonEl.style.fontWeight = '600';
                    reasonEl.style.textAlign = 'center';
                    reasonEl.innerHTML = `<i class="fas fa-exclamation-triangle"></i> Sebep: ${reason}`;
                    this.parentNode.insertBefore(reasonEl, this.nextSibling);

                    setTimeout(() => {
                        this.innerHTML = 'Giriş Yap';
                        this.style.backgroundColor = '';
                        this.style.color = '';
                        setTimeout(() => {
                            const currentReason = document.getElementById('ban-reason-msg');
                            if (currentReason) currentReason.remove();
                        }, 8000);
                    }, 4000);
                } else {
                    this.innerHTML = '<i class="fas fa-times"></i> Başarısız';
                    this.style.backgroundColor = '#e74c3c';
                    setTimeout(() => {
                        this.innerHTML = 'Giriş Yap';
                        this.style.backgroundColor = '';
                    }, 2000);
                }
            }
        } catch (error) {
            this.classList.remove('loading');
            this.innerHTML = '<i class="fas fa-times"></i> Hata';
            this.style.backgroundColor = '#e74c3c';
            setTimeout(() => {
                this.innerHTML = 'Giriş Yap';
                this.style.backgroundColor = '';
            }, 2000);
        }
    });

    // Enter tuşuyla giriş gönderme desteği
    const passwordInput = document.getElementById('password');
    if (passwordInput) {
        passwordInput.addEventListener('keyup', (e) => {
            if (e.key === 'Enter') {
                loginSubmit.click();
            }
        });
    }
}
