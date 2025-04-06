// register.js

export function initRegister() {
    // DOM Element References
    const registerLink = document.querySelector('.register-link a');
    const loginLink = document.getElementById('login-link');
    const registerModal = document.getElementById('register-modal');
    const loginModal = document.getElementById('login-modal');
    const closeRegisterModal = document.getElementById('close-register-modal');
    const registerSubmit = document.getElementById('register-submit');
    const formInputs = document.querySelectorAll('.form-control');

    // Register link (open register modal)
    registerLink.addEventListener('click', (e) => {
        e.preventDefault();
        loginModal.classList.remove('active');
        registerModal.classList.add('active');
        document.body.style.overflow = 'hidden';
        setTimeout(() => {
            const firstNameInput = document.getElementById('first-name');
            if (firstNameInput) firstNameInput.focus();
        }, 400);
    });

    // Login link (from register modal)
    loginLink.addEventListener('click', (e) => {
        e.preventDefault();
        registerModal.classList.remove('active');
        loginModal.classList.add('active');
    });

    // Close register modal
    closeRegisterModal.addEventListener('click', () => {
        registerModal.classList.remove('active');
        document.body.style.overflow = '';
    });

    registerModal.addEventListener('click', (e) => {
        if (e.target === registerModal) {
            registerModal.classList.remove('active');
            document.body.style.overflow = '';
        }
    });

    // Form input focus/blur handling
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

    // Registration submission
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

        this.classList.add('loading');
        this.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        const registerData = {
            name: firstName,
            surname: lastName,
            nickname,
            email,
            password
        };

        try {
            const response = await fetch('/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(registerData)
            });
            const data = await response.json();

            if (response.ok) {
                this.classList.remove('loading');
                this.innerHTML = '<i class="fas fa-check"></i> Başarılı';
                this.style.backgroundColor = '#1ed760';

                if (data.cookie) {
                    localStorage.setItem('wdiUserToken', data.cookie);
                    localStorage.setItem('wdiUserNickname', nickname);
                    const expiryDate = new Date();
                    expiryDate.setDate(expiryDate.getDate() + 30);
                    document.cookie = `wdiAuth=${data.cookie}; expires=${expiryDate.toUTCString()}; path=/`;
                }

                setTimeout(() => {
                    registerModal.classList.remove('active');
                    document.body.style.overflow = '';
                    window.location.reload();
                }, 1500);
            } else {
                this.classList.remove('loading');
                this.innerHTML = '<i class="fas fa-times"></i> ' + (data.error || 'Kayıt başarısız');
                this.style.backgroundColor = '#e74c3c';
                setTimeout(() => {
                    this.innerHTML = 'Kayıt Ol';
                    this.style.backgroundColor = '';
                }, 3000);
            }
        } catch (error) {
            this.classList.remove('loading');
            this.innerHTML = '<i class="fas fa-times"></i> Hata';
            this.style.backgroundColor = '#e74c3c';
            setTimeout(() => {
                this.innerHTML = 'Kayıt Ol';
                this.style.backgroundColor = '';
            }, 3000);
        }
    });
}