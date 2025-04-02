document.addEventListener('DOMContentLoaded', () => {
    // IntersectionObserver desteğini kontrol et
    if ('IntersectionObserver' in window) {
        // About sections: Her bölüm için animasyon gecikmeleri ayarla
        const aboutSections = document.querySelectorAll('.about-section');
        aboutSections.forEach((section, index) => {
            section.style.setProperty('--item-index', index);

            // Bölüm başlıklarına tıklanınca ripple efekti ekle
            const sectionTitle = section.querySelector('.section-title');
            if (sectionTitle) {
                sectionTitle.addEventListener('click', (event) => {
                    const ripple = document.createElement('div');
                    ripple.className = 'ripple-effect';
                    sectionTitle.appendChild(ripple);
                    const rect = sectionTitle.getBoundingClientRect();
                    const size = Math.max(rect.width, rect.height);
                    ripple.style.width = ripple.style.height = `${size}px`;
                    ripple.style.left = `${event.clientX - rect.left - size / 2}px`;
                    ripple.style.top = `${event.clientY - rect.top - size / 2}px`;
                    setTimeout(() => ripple.remove(), 600);
                });
            }
        });

        // Intersection Observer: Scroll animasyonları için
        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('in-view');
                }
            });
        }, { threshold: 0.1 });

        // Gözlemlenecek öğeleri ekle
        aboutSections.forEach((section) => observer.observe(section));
        document.querySelectorAll('.about-text p, .team-member, .feature-card, .contact-item, .about-content').forEach(
            (element) => observer.observe(element)
        );

        // Hero bölümündeki öğeleri animasyona sok
        const heroElements = document.querySelectorAll('.about-hero-content h1, .about-hero-content p');
        heroElements.forEach((el, index) => {
            el.style.animationDelay = `${index * 0.2}s`;
            el.classList.add('animate-in');
        });
    } else {
        // IntersectionObserver desteği yoksa, tüm öğelere direkt 'in-view' sınıfı ekle
        document.querySelectorAll('.about-section, .about-text p, .team-member, .feature-card, .contact-item, .about-content').forEach(
            (element) => element.classList.add('in-view')
        );
    }
});
