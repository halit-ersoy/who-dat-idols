export function initLogin() {
    const loginBtn = document.querySelector('.login-btn');
    const registerLink = document.querySelector('.register-link a');
    const loginLink = document.getElementById('login-link');
    const loginModal = document.getElementById('login-modal');
    const registerModal = document.getElementById('register-modal');
    const closeLoginModal = document.getElementById('close-modal');
    const closeRegisterModal = document.getElementById('close-register-modal');
    const loginSubmit = document.getElementById('login-submit');
    const registerSubmit = document.getElementById('register-submit');
    const formInputs = document.querySelectorAll('.form-control');

    if (!loginBtn || !loginModal || !registerModal || !closeLoginModal || !closeRegisterModal || !loginSubmit || !registerSubmit) {
        console.error('Login: Gerekli elementler bulunamadı.');
        return;
    }

    // Kullanıcı giriş durumunu kontrol et
    function checkAuthStatus() {
        const authCookie = localStorage.getItem('wdiUserToken');
        const userNickname = localStorage.getItem('wdiUserNickname');

        if (authCookie && userNickname) {
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
                    localStorage.removeItem('wdiUserToken');
                    localStorage.removeItem('wdiUserNickname');
                    document.cookie = 'wdiAuth=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
                    window.location.reload();
                });
            }
        }
    }
    checkAuthStatus();

    loginBtn.addEventListener('click', (e) => {
        e.preventDefault();
        loginModal.classList.add('active');
        document.body.style.overflow = 'hidden';
        setTimeout(() => {
            const emailInput = document.getElementById('email');
            if (emailInput) emailInput.focus();
        }, 400);
    });

    registerLink.addEventListener('click', (e) => {
        e.preventDefault();
        loginModal.classList.remove('active');
        registerModal.classList.add('active');
        setTimeout(() => {
            const firstNameInput = document.getElementById('first-name');
            if (firstNameInput) firstNameInput.focus();
        }, 400);
    });

    if (loginLink) {
        loginLink.addEventListener('click', (e) => {
            e.preventDefault();
            registerModal.classList.remove('active');
            loginModal.classList.add('active');
            setTimeout(() => {
                const emailInput = document.getElementById('email');
                if (emailInput) emailInput.focus();
            }, 400);
        });
    }

    closeLoginModal.addEventListener('click', () => {
        loginModal.classList.remove('active');
        document.body.style.overflow = '';
    });

    closeRegisterModal.addEventListener('click', () => {
        registerModal.classList.remove('active');
        document.body.style.overflow = '';
    });

    loginModal.addEventListener('click', (e) => {
        if (e.target === loginModal) {
            loginModal.classList.remove('active');
            document.body.style.overflow = '';
        }
    });

    registerModal.addEventListener('click', (e) => {
        if (e.target === registerModal) {
            registerModal.classList.remove('active');
            document.body.style.overflow = '';
        }
    });

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

    loginSubmit.addEventListener('click', async function () {
        const usernameOrEmail = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        if (!usernameOrEmail || !password) {
            alert('Lütfen kullanıcı adı/e-posta ve şifre giriniz.');
            return;
        }
        this.classList.add('loading');
        this.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        const loginData = { usernameOrEmail, password };

        try {
            const response = await fetch('/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(loginData)
            });
            const data = await response.json();
            if (data.success) {
                this.classList.remove('loading');
                this.innerHTML = '<i class="fas fa-check"></i> Başarılı';
                this.style.backgroundColor = '#1ed760';
                localStorage.setItem('wdiUserToken', data.cookie);
                localStorage.setItem('wdiUserNickname', usernameOrEmail);
                const expiryDate = new Date();
                expiryDate.setDate(expiryDate.getDate() + 30);
                document.cookie = `wdiAuth=${data.cookie}; expires=${expiryDate.toUTCString()}; path=/`;

                setTimeout(() => {
                    loginModal.classList.remove('active');
                    document.body.style.overflow = '';
                    document.getElementById('email').value = '';
                    document.getElementById('password').value = '';
                    window.location.reload();
                }, 1500);
            } else {
                this.classList.remove('loading');
                this.innerHTML = '<i class="fas fa-times"></i> Başarısız';
                this.style.backgroundColor = '#e74c3c';
                setTimeout(() => {
                    this.innerHTML = 'Giriş Yap';
                    this.style.backgroundColor = '';
                }, 2000);
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

    const passwordInput = document.getElementById('password');
    if (passwordInput) {
        passwordInput.addEventListener('keyup', (e) => {
            if (e.key === 'Enter') {
                loginSubmit.click();
            }
        });
    }

    function isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    if (registerSubmit) {
        registerSubmit.addEventListener('click', async function () {
            const firstName = document.getElementById('first-name').value;
            const lastName = document.getElementById('last-name').value;
            const nickname = document.getElementById('nickname').value;
            const email = document.getElementById('register-email').value;
            const password = document.getElementById('register-password').value;

            if (!firstName || !lastName || !nickname || !email || !password) {
                this.innerHTML = '<i class="fas fa-times"></i> Tüm alanları doldurun';
                this.style.backgroundColor = '#e74c3c';
                setTimeout(() => {
                    this.innerHTML = 'Kayıt Ol';
                    this.style.backgroundColor = '';
                }, 3000);
                return;
            }

            if (!isValidEmail(email)) {
                this.innerHTML = '<i class="fas fa-times"></i> Geçerli bir e-posta adresi giriniz';
                this.style.backgroundColor = '#e74c3c';
                setTimeout(() => {
                    this.innerHTML = 'Kayıt Ol';
                    this.style.backgroundColor = '';
                }, 3000);
                return;
            }

            this.classList.add('loading');
            const userData = { name: firstName, surname: lastName, nickname, email, password };

            try {
                const response = await fetch('/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(userData)
                });
                const data = await response.json();
                this.classList.remove('loading');

                if (data.success) {
                    localStorage.setItem('wdiUserToken', data.cookie);
                    localStorage.setItem('wdiUserNickname', nickname);
                    const expiryDate = new Date();
                    expiryDate.setDate(expiryDate.getDate() + 30);
                    document.cookie = `wdiAuth=${data.cookie}; expires=${expiryDate.toUTCString()}; path=/`;
                    this.innerHTML = '<i class="fas fa-check"></i> Başarılı';
                    this.style.backgroundColor = '#2ecc71';
                    setTimeout(() => {
                        registerModal.classList.remove('active');
                        document.body.style.overflow = '';
                        window.location.reload();
                    }, 1500);
                } else {
                    this.innerHTML = `<i class="fas fa-times"></i> ${data.error || 'Kayıt başarısız'}`;
                    this.style.backgroundColor = '#e74c3c';
                    setTimeout(() => {
                        this.innerHTML = 'Kayıt Ol';
                        this.style.backgroundColor = '';
                    }, 3000);
                }
            } catch (error) {
                this.classList.remove('loading');
                this.innerHTML = `<i class="fas fa-times"></i> ${error.message || 'Bir hata oluştu'}`;
                this.style.backgroundColor = '#e74c3c';
                setTimeout(() => {
                    this.innerHTML = 'Kayıt Ol';
                    this.style.backgroundColor = '';
                }, 3000);
            }
        });
    }
}
