document.addEventListener('DOMContentLoaded', () => {

    // FAQ Items: Her FAQ item için animasyon gecikmesi ayarlanıyor
    const faqItems = document.querySelectorAll('.faq-item');
    faqItems.forEach((item, index) => {
        item.style.setProperty('--item-index', index);
        const question = item.querySelector('.faq-question');
        if (question) {
            question.addEventListener('click', (event) => {
                // Ripple effect oluşturma
                const ripple = document.createElement('div');
                ripple.className = 'ripple-effect';
                question.appendChild(ripple);
                const rect = question.getBoundingClientRect();
                const size = Math.max(rect.width, rect.height);
                ripple.style.width = ripple.style.height = `${size}px`;
                // Güvenlik için event.target yerine event.clientX/Y
                ripple.style.left = `${event.clientX - rect.left - size / 2}px`;
                ripple.style.top = `${event.clientY - rect.top - size / 2}px`;
                setTimeout(() => ripple.remove(), 600);

                // SSS aç/kapa işlemi
                if (item.classList.contains('active')) {
                    item.classList.remove('active');
                } else {
                    // Diğerleri kapanıyor
                    faqItems.forEach((otherItem) => {
                        if (otherItem !== item && otherItem.classList.contains('active')) {
                            otherItem.classList.remove('active');
                        }
                    });
                    item.classList.add('active');
                    if (!isElementInViewport(item)) {
                        item.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    }
                }
            });
        }
    });

    // İlk FAQ öğesini aç
    if (faqItems.length > 0) {
        faqItems[0].classList.add('active');
    }

    // Intersection Observer ile scroll animasyonlarını tetikleme
    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            if (entry.isIntersecting) {
                entry.target.classList.add('in-view');
            }
        });
    }, { threshold: 0.1 });

    faqItems.forEach((item) => observer.observe(item));

    // Yardımcı: Elementin viewport'ta olup olmadığını kontrol eder
    function isElementInViewport(el) {
        const rect = el.getBoundingClientRect();
        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
            rect.right <= (window.innerWidth || document.documentElement.clientWidth)
        );
    }
});
