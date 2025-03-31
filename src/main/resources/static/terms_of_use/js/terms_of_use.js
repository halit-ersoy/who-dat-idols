// terms_of_use.js - Main functionality for Terms of Use page

document.addEventListener('DOMContentLoaded', () => {
    initHeaderScroll();
    initLogin();
});

// Header background change on scroll
function initHeaderScroll() {
    const header = document.getElementById('header');
    if (!header) return;
    window.addEventListener('scroll', () => {
        if (window.scrollY > 50) {
            header.classList.add('scrolled');
        } else {
            header.classList.remove('scrolled');
        }
    });
}

// Basic login functionality for Terms page
function initLogin() {
    const loginBtn = document.querySelector('.login-btn');
    if (loginBtn) {
        loginBtn.addEventListener('click', () => {
            // Redirect to homepage with login modal open, or handle login logic
            window.location.href = '/?login=open';
        });
    }
}
