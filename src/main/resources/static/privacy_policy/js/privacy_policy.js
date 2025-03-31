// privacy_policy.js - Basit scroll tabanlı header kontrolü ve diğer başlangıç işlemleri

window.addEventListener('scroll', () => {
    const header = document.getElementById('header');
    if (!header) return;
    if (window.scrollY > 50) {
        header.classList.add('scrolled');
    } else {
        header.classList.remove('scrolled');
    }
});
