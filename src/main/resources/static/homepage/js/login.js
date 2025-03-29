// src/main/resources/static/homepage/js/login.js

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

    // Open login modal when login button is clicked
    loginBtn.addEventListener('click', function(e) {
        e.preventDefault();
        loginModal.classList.add('active');
        document.body.style.overflow = 'hidden'; // Prevent scrolling

        // Focus on first input with small delay for animation
        setTimeout(() => {
            document.getElementById('email').focus();
        }, 400);
    });

    // Open register modal when "Hesap oluştur" is clicked
    registerLink.addEventListener('click', function(e) {
        e.preventDefault();
        loginModal.classList.remove('active');
        registerModal.classList.add('active');

        // Focus on first input with small delay for animation
        setTimeout(() => {
            document.getElementById('first-name').focus();
        }, 400);
    });

    // Go back to login modal from register modal
    if (loginLink) {
        loginLink.addEventListener('click', function(e) {
            e.preventDefault();
            registerModal.classList.remove('active');
            loginModal.classList.add('active');

            // Focus on first input with small delay for animation
            setTimeout(() => {
                document.getElementById('email').focus();
            }, 400);
        });
    }

    // Close login modal
    closeLoginModal.addEventListener('click', function() {
        loginModal.classList.remove('active');
        document.body.style.overflow = '';
    });

    // Close register modal
    if (closeRegisterModal) {
        closeRegisterModal.addEventListener('click', function() {
            registerModal.classList.remove('active');
            document.body.style.overflow = '';
        });
    }

    // Close modals when clicking outside
    loginModal.addEventListener('click', function(e) {
        if (e.target === loginModal) {
            loginModal.classList.remove('active');
            document.body.style.overflow = '';
        }
    });

    if (registerModal) {
        registerModal.addEventListener('click', function(e) {
            if (e.target === registerModal) {
                registerModal.classList.remove('active');
                document.body.style.overflow = '';
            }
        });
    }

    // Handle input focus effects
    formInputs.forEach(input => {
        input.addEventListener('focus', function() {
            this.parentElement.classList.add('active');
        });

        input.addEventListener('blur', function() {
            if (!this.value) {
                this.parentElement.classList.remove('active');
            }
        });
    });

    // Handle login button click with API call to backend
    loginSubmit.addEventListener('click', function() {
        // Get form values
        const usernameOrEmail = document.getElementById('email').value;
        const password = document.getElementById('password').value;

        // Basic validation
        if (!usernameOrEmail || !password) {
            alert('Lütfen kullanıcı adı/e-posta ve şifre giriniz.');
            return;
        }

        // Show loading state
        this.classList.add('loading');
        this.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';

        // Create login data
        const loginData = {
            usernameOrEmail: usernameOrEmail,
            password: password
        };

        // Make API call to login endpoint
        fetch('/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(loginData)
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    // Show success animation
                    this.classList.remove('loading');
                    this.innerHTML = '<i class="fas fa-check"></i> Başarılı';
                    this.style.backgroundColor = '#1ed760';

                    // Store cookie in local storage for session management
                    localStorage.setItem('auth_cookie', data.cookie);

                    // Close modal after success
                    setTimeout(() => {
                        loginModal.classList.remove('active');
                        document.body.style.overflow = '';

                        // Reset form
                        document.getElementById('email').value = '';
                        document.getElementById('password').value = '';

                        // Optionally refresh page or update UI to show logged in state
                        window.location.reload();
                    }, 1500);
                } else {
                    // Show failure animation
                    this.classList.remove('loading');
                    this.innerHTML = '<i class="fas fa-times"></i> Başarısız';
                    this.style.backgroundColor = '#e74c3c';

                    // Reset button after delay
                    setTimeout(() => {
                        this.innerHTML = 'Giriş Yap';
                        this.style.backgroundColor = '';
                    }, 2000);
                }
            })
            .catch(error => {
                // Show error animation
                this.classList.remove('loading');
                this.innerHTML = '<i class="fas fa-times"></i> Hata';
                this.style.backgroundColor = '#e74c3c';

                // Reset button after delay
                setTimeout(() => {
                    this.innerHTML = 'Giriş Yap';
                    this.style.backgroundColor = '';
                }, 2000);
            });
    });

// Enable "Enter" key to submit login
    document.getElementById('password').addEventListener('keyup', function(e) {
        if (e.key === 'Enter') {
            loginSubmit.click();
        }
    });

    // Handle register button click with API call to backend
    if (registerSubmit) {
        registerSubmit.addEventListener('click', function() {
            // Get form values
            const firstName = document.getElementById('first-name').value;
            const lastName = document.getElementById('last-name').value;
            const nickname = document.getElementById('nickname').value;
            const email = document.getElementById('register-email').value;
            const password = document.getElementById('register-password').value;

            // Basic validation
            if (!firstName || !lastName || !nickname || !email || !password) {
                alert('Lütfen tüm alanları doldurunuz.');
                return;
            }

            // Show loading state
            this.classList.add('loading');

            // Create user data object matching Person model
            const userData = {
                name: firstName,
                surname: lastName,
                nickname: nickname,
                email: email,
                password: password
            };

            // Make API call to register endpoint
            fetch('/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
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
                    // Show success animation
                    this.classList.remove('loading');
                    this.innerHTML = '<i class="fas fa-check"></i> Başarılı';
                    this.style.backgroundColor = '#1ed760';

                    // Close modal after success
                    setTimeout(() => {
                        registerModal.classList.remove('active');
                        document.body.style.overflow = '';

                        // Reset button
                        setTimeout(() => {
                            this.innerHTML = 'Kayıt Ol';
                            this.style.backgroundColor = '';

                            // Reset form fields
                            document.getElementById('first-name').value = '';
                            document.getElementById('last-name').value = '';
                            document.getElementById('nickname').value = '';
                            document.getElementById('register-email').value = '';
                            document.getElementById('register-password').value = '';
                        }, 500);
                    }, 1000);
                })
                .catch(error => {
                    // Show error animation
                    this.classList.remove('loading');
                    this.innerHTML = '<i class="fas fa-times"></i> Hata';
                    this.style.backgroundColor = '#e74c3c';

                    // Show error message
                    alert(error.message);

                    // Reset button after a delay
                    setTimeout(() => {
                        this.innerHTML = 'Kayıt Ol';
                        this.style.backgroundColor = '';
                    }, 2000);
                });
        });

        // Enable "Enter" key to submit registration
        document.getElementById('register-password').addEventListener('keyup', function(e) {
            if (e.key === 'Enter') {
                registerSubmit.click();
            }
        });
    }

    // Enable "Enter" key to submit login
    document.getElementById('password').addEventListener('keyup', function(e) {
        if (e.key === 'Enter') {
            loginSubmit.click();
        }
    });
}