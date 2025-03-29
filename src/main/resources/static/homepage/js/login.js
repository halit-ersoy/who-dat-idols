export function initLogin() {
    // DOM element selections
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

    // Open login modal on login button click
    loginBtn.addEventListener('click', function (e) {
        e.preventDefault();
        loginModal.classList.add('active');
        document.body.style.overflow = 'hidden';
        setTimeout(() => {
            document.getElementById('email').focus();
        }, 400);
    });

    // Open register modal when "Hesap oluştur" is clicked
    registerLink.addEventListener('click', function (e) {
        e.preventDefault();
        loginModal.classList.remove('active');
        registerModal.classList.add('active');
        setTimeout(() => {
            document.getElementById('first-name').focus();
        }, 400);
    });

    // Switch back to login modal from register modal
    if (loginLink) {
        loginLink.addEventListener('click', function (e) {
            e.preventDefault();
            registerModal.classList.remove('active');
            loginModal.classList.add('active');
            setTimeout(() => {
                document.getElementById('email').focus();
            }, 400);
        });
    }

    // Close login modal on close button click
    closeLoginModal.addEventListener('click', function () {
        loginModal.classList.remove('active');
        document.body.style.overflow = '';
    });

    // Close register modal on close button click
    if (closeRegisterModal) {
        closeRegisterModal.addEventListener('click', function () {
            registerModal.classList.remove('active');
            document.body.style.overflow = '';
        });
    }

    // Close modals when clicking outside the modal content
    loginModal.addEventListener('click', function (e) {
        if (e.target === loginModal) {
            loginModal.classList.remove('active');
            document.body.style.overflow = '';
        }
    });
    if (registerModal) {
        registerModal.addEventListener('click', function (e) {
            if (e.target === registerModal) {
                registerModal.classList.remove('active');
                document.body.style.overflow = '';
            }
        });
    }

    // Add focus effects to form inputs
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

    // Handle login API call on login submit button click
    loginSubmit.addEventListener('click', function () {
        const usernameOrEmail = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        if (!usernameOrEmail || !password) {
            alert('Lütfen kullanıcı adı/e-posta ve şifre giriniz.');
            return;
        }
        this.classList.add('loading');
        this.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        const loginData = {
            usernameOrEmail: usernameOrEmail,
            password: password
        };
        fetch('/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(loginData)
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    this.classList.remove('loading');
                    this.innerHTML = '<i class="fas fa-check"></i> Başarılı';
                    this.style.backgroundColor = '#1ed760';
                    localStorage.setItem('auth_cookie', data.cookie);
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
            })
            .catch(error => {
                this.classList.remove('loading');
                this.innerHTML = '<i class="fas fa-times"></i> Hata';
                this.style.backgroundColor = '#e74c3c';
                setTimeout(() => {
                    this.innerHTML = 'Giriş Yap';
                    this.style.backgroundColor = '';
                }, 2000);
            });
    });

    // Enable "Enter" key submission for login (password field)
    document.getElementById('password').addEventListener('keyup', function (e) {
        if (e.key === 'Enter') {
            loginSubmit.click();
        }
    });

    function isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    // Handle register API call on register submit button click
    if (registerSubmit) {
        registerSubmit.addEventListener('click', function () {
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
            // Validate email format
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
            const userData = {
                name: firstName,
                surname: lastName,
                nickname: nickname,
                email: email,
                password: password
            };
            fetch('/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(userData)
            })
                .then(response => {
                    if (!response.ok) {
                        return response.json().then(err => {
                            throw new Error(err.error || 'Kayıt işlemi başarısız oldu.');
                        });
                    }
                    return response.json();
                })
                .then(data => {
                    this.classList.remove('loading');
                    this.innerHTML = '<i class="fas fa-check"></i> Başarılı';
                    this.style.backgroundColor = '#1ed760';
                    setTimeout(() => {
                        registerModal.classList.remove('active');
                        document.body.style.overflow = '';
                        setTimeout(() => {
                            this.innerHTML = 'Kayıt Ol';
                            this.style.backgroundColor = '';
                            document.getElementById('first-name').value = '';
                            document.getElementById('last-name').value = '';
                            document.getElementById('nickname').value = '';
                            document.getElementById('register-email').value = '';
                            document.getElementById('register-password').value = '';
                        }, 500);
                    }, 1000);
                })
                .catch(error => {
                    this.classList.remove('loading');
                    // Display the error message directly in the button instead of using alert
                    this.innerHTML = '<i class="fas fa-times"></i> ' + error.message;
                    this.style.backgroundColor = '#e74c3c';
                    // Extend the timeout to give users enough time to read the error message
                    setTimeout(() => {
                        this.innerHTML = 'Kayıt Ol';
                        this.style.backgroundColor = '';
                    }, 3000);
                });
        });

        // Enable "Enter" key submission for register (register password field)
        document.getElementById('register-password').addEventListener('keyup', function (e) {
            if (e.key === 'Enter') {
                registerSubmit.click();
            }
        });
    }

    // Duplicate "Enter" key submission for login (password field)
    document.getElementById('password').addEventListener('keyup', function (e) {
        if (e.key === 'Enter') {
            loginSubmit.click();
        }
    });
}
