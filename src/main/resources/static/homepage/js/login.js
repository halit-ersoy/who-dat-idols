export function initLogin() {
    // ============================
    // DOM Element Referansları
    // ============================
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
    const forgotPasswordLink = document.querySelector('.forgot-password a');
    const forgotPasswordModal = document.getElementById('forgot-password-modal');
    const closeForgotPasswordModal = document.getElementById('close-forgot-modal');
    const forgotPasswordSubmit = document.getElementById('forgot-password-submit');
    const backToLoginLink = document.getElementById('back-to-login');

    // ============================
    // Gerekli Elementlerin Kontrolü
    // ============================
    if (!loginBtn || !loginModal || !registerModal || !closeLoginModal || !closeRegisterModal || !loginSubmit || !registerSubmit) {
        console.error('Login: Gerekli elementler bulunamadı.');
        return;
    }

    // ============================
    // Şifremi Unuttum Modal İşlemleri ve Yeni Eklenen Kod
    // ============================
    if (forgotPasswordLink && forgotPasswordModal && closeForgotPasswordModal && forgotPasswordSubmit) {

        // Forgot password link tıklanıldığında
        forgotPasswordLink.addEventListener('click', (e) => {
            e.preventDefault();
            loginModal.classList.remove('active');
            forgotPasswordModal.classList.add('active');
            setTimeout(() => {
                const forgotEmailInput = document.getElementById('forgot-email');
                if (forgotEmailInput) forgotEmailInput.focus();
            }, 400);
        });

        // Forgot password modal kapatma butonu
        closeForgotPasswordModal.addEventListener('click', () => {
            forgotPasswordModal.classList.remove('active');
            document.body.style.overflow = '';
        });

        // Modal dışına tıklanıldığında kapatma
        forgotPasswordModal.addEventListener('click', (e) => {
            if (e.target === forgotPasswordModal) {
                forgotPasswordModal.classList.remove('active');
                document.body.style.overflow = '';
            }
        });

        // ============================
        // Şifremi Unuttum İçin Form ve Dinamik Bölümler
        // ============================
        const forgotPasswordForm = document.querySelector('#forgot-password-modal .login-form');
        if (forgotPasswordForm) {
            // Doğrulama kodu girişi (ilk başta gizli)
            const verificationSection = document.createElement('div');
            verificationSection.className = 'verification-section';
            verificationSection.style.display = 'none';
            verificationSection.innerHTML = `
                <div class="form-group">
                    <label for="verification-code">Doğrulama Kodu</label>
                    <input type="text" class="form-control" id="verification-code" placeholder="6 haneli kodu girin">
                    <span class="highlight"></span>
                </div>
                <button class="login-btn-page" id="verify-code-submit">Kodu Doğrula</button>
            `;
            forgotPasswordForm.appendChild(verificationSection);

            // Şifre sıfırlama bölümü (ilk başta gizli)
            const passwordResetSection = document.createElement('div');
            passwordResetSection.className = 'password-reset-section';
            passwordResetSection.style.display = 'none';
            passwordResetSection.innerHTML = `
                <div class="form-group">
                    <label for="new-password">Yeni Şifre</label>
                    <input type="password" class="form-control" id="new-password" placeholder="Yeni şifrenizi giriniz">
                </div>
                <div class="form-group">
                    <label for="confirm-password">Şifre Tekrar</label>
                    <input type="password" class="form-control" id="confirm-password" placeholder="Şifrenizi tekrar giriniz">
                </div>
                <button class="login-btn-page" id="reset-password-submit">Şifremi Güncelle</button>
            `;
            forgotPasswordForm.appendChild(passwordResetSection);

            // currentEmail ve currentCode global olarak kullanılabilsin
            let currentEmail = '';
            let currentCode = '';

            // Şifremi sıfırla butonu (kod isteme)
            forgotPasswordSubmit.addEventListener('click', async function () {
                const userInput = document.getElementById('forgot-email').value;

                if (!userInput) {
                    this.innerHTML = '<i class="fas fa-times"></i> E-posta gerekli';
                    this.style.backgroundColor = '#e74c3c';
                    setTimeout(() => {
                        this.innerHTML = 'Şifremi Sıfırla';
                        this.style.backgroundColor = '';
                    }, 3000);
                    return;
                }

                this.classList.add('loading');
                this.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';

                try {
                    const response = await fetch('/forgot-password', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ usernameOrEmail: userInput })
                    });
                    const data = await response.json();

                    if (data.success) {
                        this.classList.remove('loading');
                        this.innerHTML = '<i class="fas fa-check"></i> E-posta Gönderildi';
                        this.style.backgroundColor = '#1ed760';

                        // currentEmail kaydediliyor ve doğrulama bölümü gösteriliyor
                        currentEmail = userInput;
                        document.getElementById('forgot-email').readOnly = true;
                        verificationSection.style.display = 'block';
                        forgotPasswordSubmit.style.display = 'none';

                        // Email gönderildi bildirimi (login link'in üstüne ekleniyor)
                        const emailNotification = document.createElement('div');
                        emailNotification.className = 'email-notification';
                        emailNotification.innerHTML =
                            `<div class="email-icon"><i class="fas fa-envelope"></i></div>
                             <div class="email-text">Doğrulama kodu e-posta adresinize gönderildi. Lütfen gelen kutunuzu kontrol edin.</div>`;
                        emailNotification.style.padding = '15px';
                        emailNotification.style.margin = '15px 0';
                        emailNotification.style.backgroundColor = 'rgba(30, 215, 96, 0.1)';
                        emailNotification.style.borderRadius = '8px';
                        emailNotification.style.display = 'flex';
                        emailNotification.style.alignItems = 'center';
                        const emailIcon = emailNotification.querySelector('.email-icon');
                        emailIcon.style.fontSize = '24px';
                        emailIcon.style.color = '#1ed760';
                        emailIcon.style.marginRight = '15px';
                        const loginLinkContainer = document.getElementById('back-to-login').closest('.register-link');
                        loginLinkContainer.parentNode.insertBefore(emailNotification, loginLinkContainer);
                        loginLinkContainer.parentNode.insertBefore(verificationSection, loginLinkContainer);
                    } else {
                        this.classList.remove('loading');
                        this.innerHTML = '<i class="fas fa-times"></i> ' + (data.message || 'E-posta gönderme başarısız oldu');
                        this.style.backgroundColor = '#e74c3c';
                        setTimeout(() => {
                            this.innerHTML = 'Şifremi Sıfırla';
                            this.style.backgroundColor = '';
                        }, 3000);
                    }
                } catch (error) {
                    this.classList.remove('loading');
                    this.innerHTML = '<i class="fas fa-times"></i> Bir hata oluştu';
                    this.style.backgroundColor = '#e74c3c';
                    setTimeout(() => {
                        this.innerHTML = 'Şifremi Sıfırla';
                        this.style.backgroundColor = '';
                    }, 3000);
                }
            });

            // Reset password submit butonu event listener'ı
            const resetPasswordSubmitBtn = document.getElementById('reset-password-submit');
            if (resetPasswordSubmitBtn) {
                resetPasswordSubmitBtn.addEventListener('click', async function () {
                    const newPassword = document.getElementById('new-password').value;
                    const confirmPassword = document.getElementById('confirm-password').value;

                    if (!newPassword || !confirmPassword) {
                        this.innerHTML = '<i class="fas fa-times"></i> Tüm alanlar gerekli';
                        this.style.backgroundColor = '#e74c3c';
                        setTimeout(() => {
                            this.innerHTML = 'Şifremi Güncelle';
                            this.style.backgroundColor = '';
                        }, 3000);
                        return;
                    }

                    if (newPassword !== confirmPassword) {
                        this.innerHTML = '<i class="fas fa-times"></i> Şifreler eşleşmiyor';
                        this.style.backgroundColor = '#e74c3c';
                        setTimeout(() => {
                            this.innerHTML = 'Şifremi Güncelle';
                            this.style.backgroundColor = '';
                        }, 3000);
                        return;
                    }

                    this.classList.add('loading');
                    this.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';

                    try {
                        const response = await fetch('/reset-password', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({
                                usernameOrEmail: currentEmail,
                                code: currentCode,
                                newPassword: newPassword
                            })
                        });
                        const data = await response.json();

                        if (data.success) {
                            this.innerHTML = '<i class="fas fa-check"></i> Şifre güncellendi';
                            this.style.backgroundColor = '#1ed760';

                            // Başarılı mesaj overlay'ı
                            const successMessage = document.createElement('div');
                            successMessage.className = 'password-success-message';
                            successMessage.innerHTML = `
                                <div class="success-icon">
                                    <i class="fas fa-check-circle"></i>
                                </div>
                                <div class="success-text">Şifreniz başarıyla güncellendi!</div>
                            `;
                            successMessage.style.position = 'absolute';
                            successMessage.style.top = '0';
                            successMessage.style.left = '0';
                            successMessage.style.width = '100%';
                            successMessage.style.height = '100%';
                            successMessage.style.display = 'flex';
                            successMessage.style.flexDirection = 'column';
                            successMessage.style.alignItems = 'center';
                            successMessage.style.justifyContent = 'center';
                            successMessage.style.background = 'rgba(0,0,0,0.9)';
                            successMessage.style.zIndex = '100';
                            successMessage.style.borderRadius = '16px';
                            successMessage.style.animation = 'fadeIn 0.5s ease-in-out';
                            const successIcon = successMessage.querySelector('.success-icon');
                            successIcon.style.fontSize = '60px';
                            successIcon.style.color = '#1ed760';
                            successIcon.style.marginBottom = '20px';
                            successIcon.style.animation = 'popIn 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards';
                            const successText = successMessage.querySelector('.success-text');
                            successText.style.color = '#ffffff';
                            successText.style.fontSize = '18px';
                            successText.style.fontWeight = 'bold';
                            const loginModalContent = document.querySelector('#forgot-password-modal .login-modal');
                            if (loginModalContent) {
                                loginModalContent.appendChild(successMessage);
                            }
                            const style = document.createElement('style');
                            style.textContent = `
                                @keyframes fadeIn {
                                    from { opacity: 0; }
                                    to { opacity: 1; }
                                }
                                @keyframes popIn {
                                    0% { transform: scale(0); }
                                    70% { transform: scale(1.2); }
                                    100% { transform: scale(1); }
                                }
                            `;
                            document.head.appendChild(style);

                            setTimeout(() => {
                                forgotPasswordModal.classList.remove('active');
                                document.body.style.overflow = '';
                                document.getElementById('forgot-email').value = '';
                                document.getElementById('new-password').value = '';
                                document.getElementById('confirm-password').value = '';
                                document.getElementById('forgot-email').readOnly = false;
                                forgotPasswordSubmit.style.display = 'block';
                                verificationSection.style.display = 'none';
                                passwordResetSection.style.display = 'none';
                                if (successMessage && successMessage.parentNode) {
                                    successMessage.parentNode.removeChild(successMessage);
                                }
                                this.innerHTML = 'Şifremi Güncelle';
                                this.style.backgroundColor = '';
                                this.classList.remove('loading');
                            }, 3000);
                        } else {
                            this.classList.remove('loading');
                            this.innerHTML = '<i class="fas fa-times"></i> ' + data.message;
                            this.style.backgroundColor = '#e74c3c';
                            setTimeout(() => {
                                this.innerHTML = 'Şifremi Güncelle';
                                this.style.backgroundColor = '';
                            }, 3000);
                        }
                    } catch (error) {
                        this.classList.remove('loading');
                        this.innerHTML = '<i class="fas fa-times"></i> Bir hata oluştu';
                        this.style.backgroundColor = '#e74c3c';
                        setTimeout(() => {
                            this.innerHTML = 'Şifremi Güncelle';
                            this.style.backgroundColor = '';
                        }, 3000);
                    }
                });
            }

            // Yeni eklenen Verification code submit event listener (document.body üzerinden dinleniyor)
            document.body.addEventListener('click', function(e) {
                if (e.target && e.target.id === 'verify-code-submit') {
                    const verifyButton = e.target;
                    verifyButton.classList.add('loading');
                    verifyButton.innerHTML = '';

                    const code = document.getElementById('verification-code').value;

                    // Check if code is empty
                    if (!code) {
                        verifyButton.classList.remove('loading');
                        verifyButton.innerHTML = '<i class="fas fa-times"></i> Kod boş olamaz';
                        verifyButton.style.backgroundColor = '#e74c3c';
                        setTimeout(() => {
                            verifyButton.innerHTML = 'Kodu Doğrula';
                            verifyButton.style.backgroundColor = '';
                        }, 3000);
                        return;
                    }

                    // currentEmail'nın erişilebilir olduğundan emin olmak için
                    const forgotEmailInput = document.getElementById('forgot-email');
                    const userInput = forgotEmailInput ? forgotEmailInput.value : '';

                    // API çağrısı: doğrulama kodu kontrolü
                    fetch('/verify-code', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({
                            usernameOrEmail: userInput,
                            code: code
                        })
                    })
                        .then(response => response.json())
                        .then(data => {
                            verifyButton.classList.remove('loading');

                            if (data.success) {
                                // Doğrulama başarılı ise kod kaydediliyor
                                currentCode = code;
                                verifyButton.innerHTML = '<i class="fas fa-check"></i> Doğrulandı';
                                verifyButton.style.backgroundColor = '#1ed760';

                                // Doğrulama bölümünü gizle
                                verificationSection.style.display = 'none';

                                // Şifre sıfırlama bölümünü göster
                                if (passwordResetSection) {
                                    passwordResetSection.style.display = 'block';
                                }
                            } else {
                                verifyButton.innerHTML = '<i class="fas fa-times"></i> ' + (data.message || 'Geçersiz kod');
                                verifyButton.style.backgroundColor = '#e74c3c';
                                setTimeout(() => {
                                    verifyButton.innerHTML = 'Kodu Doğrula';
                                    verifyButton.style.backgroundColor = '';
                                }, 3000);
                            }
                        })
                        .catch(error => {
                            console.error('Verification error:', error);
                            verifyButton.classList.remove('loading');
                            verifyButton.innerHTML = '<i class="fas fa-times"></i> Bir hata oluştu';
                            verifyButton.style.backgroundColor = '#e74c3c';
                            setTimeout(() => {
                                verifyButton.innerHTML = 'Kodu Doğrula';
                                verifyButton.style.backgroundColor = '';
                            }, 3000);
                        });
                }
            });
        }
    }

    // ============================
    // Forgot Password'den Girişe Dönüş
    // ============================
    if (backToLoginLink) {
        backToLoginLink.addEventListener('click', (e) => {
            e.preventDefault();
            forgotPasswordModal.classList.remove('active');
            loginModal.classList.add('active');
            setTimeout(() => {
                const emailInput = document.getElementById('email');
                if (emailInput) emailInput.focus();
            }, 400);
        });
    }

    // ============================
    // Kullanıcı Giriş Durumunu Kontrol Et ve Profil Oluştur
    // ============================
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

    // ============================
    // Modal Açma İşlemleri
    // ============================
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

    // ============================
    // Modal Kapatma İşlemleri
    // ============================
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

    // ============================
    // Form Input Odaklanma ve Çıkma İşlemleri
    // ============================
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

    // ============================
    // Giriş Yapma İşlemleri
    // ============================
    loginSubmit.addEventListener('click', async function () {
        const usernameOrEmail = document.getElementById('email').value;
        const password = document.getElementById('password').value;

        if (!usernameOrEmail || !password) {
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

    // Enter tuşuyla giriş gönderme desteği
    const passwordInput = document.getElementById('password');
    if (passwordInput) {
        passwordInput.addEventListener('keyup', (e) => {
            if (e.key === 'Enter') {
                loginSubmit.click();
            }
        });
    }

    // ============================
    // E-Posta Doğrulama Yardımcı Fonksiyonu
    // ============================
    function isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    // ============================
    // Kayıt Olma İşlemleri
    // ============================
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
