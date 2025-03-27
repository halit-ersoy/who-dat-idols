// src/main/resources/static/homepage/js/loginModal.js

export function initLogin() {
    const loginBtn = document.querySelector('.login-btn');
    const loginModal = document.getElementById('login-modal');
    const closeModal = document.getElementById('close-modal');
    const loginSubmit = document.getElementById('login-submit');
    const formInputs = document.querySelectorAll('.form-control');

    // Open modal when login button is clicked
    loginBtn.addEventListener('click', function(e) {
        e.preventDefault();
        loginModal.classList.add('active');
        document.body.style.overflow = 'hidden'; // Prevent scrolling

        // Focus on first input with small delay for animation
        setTimeout(() => {
            document.getElementById('email').focus();
        }, 400);
    });

    // Close modal
    closeModal.addEventListener('click', function() {
        loginModal.classList.remove('active');
        document.body.style.overflow = '';
    });

    // Close modal when clicking outside
    loginModal.addEventListener('click', function(e) {
        if (e.target === loginModal) {
            loginModal.classList.remove('active');
            document.body.style.overflow = '';
        }
    });

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

    // Handle login button click with loading animation
    loginSubmit.addEventListener('click', function() {
        // Show loading state
        this.classList.add('loading');

        // Simulate login process (replace with actual login logic)
        setTimeout(() => {
            this.classList.remove('loading');

            // For demo: show success with animation
            this.innerHTML = '<i class="fas fa-check"></i> Başarılı';
            this.style.backgroundColor = '#1ed760';

            // Close modal after success
            setTimeout(() => {
                loginModal.classList.remove('active');
                document.body.style.overflow = '';

                // Reset button
                setTimeout(() => {
                    this.innerHTML = 'Giriş Yap';
                    this.style.backgroundColor = '';
                }, 500);
            }, 1000);
        }, 1500);
    });

    // Enable "Enter" key to submit
    document.getElementById('password').addEventListener('keyup', function(e) {
        if (e.key === 'Enter') {
            loginSubmit.click();
        }
    });
}