// Main functionality for Terms of Use page
document.addEventListener('DOMContentLoaded', function() {
    // Initialize header scroll effect
    initHeaderScroll();

    // Initialize login functionality if needed
    initLogin();
});

// Header background change on scroll
function initHeaderScroll() {
    window.addEventListener('scroll', function () {
        if (window.scrollY > 50) {
            document.getElementById('header').classList.add('scrolled');
        } else {
            document.getElementById('header').classList.remove('scrolled');
        }
    });
}

// Login functionality - Can be imported from main login.js if needed
function initLogin() {
    const loginBtn = document.querySelector('.login-btn');

    if (loginBtn) {
        loginBtn.addEventListener('click', function() {
            // Redirect to homepage with login modal open or handle login
            window.location.href = '/?login=open';
        });
    }
}