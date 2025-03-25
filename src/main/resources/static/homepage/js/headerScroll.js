// Header background change on scroll
export function initHeaderScroll() {
    window.addEventListener('scroll', function () {
        if (window.scrollY > 50) {
            document.getElementById('header').classList.add('scrolled');
        } else {
            document.getElementById('header').classList.remove('scrolled');
        }
    });
}