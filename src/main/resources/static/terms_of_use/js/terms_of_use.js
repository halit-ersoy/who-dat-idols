document.addEventListener('DOMContentLoaded', () => {

    // Terms sections: Add animation delays to each section
    const termsSections = document.querySelectorAll('.terms-content');
    termsSections.forEach((section, index) => {
        section.style.setProperty('--item-index', index);

        // Add ripple effect to section titles when clicked
        const sectionTitle = section.previousElementSibling;
        if (sectionTitle && sectionTitle.classList.contains('section-title')) {
            sectionTitle.addEventListener('click', (event) => {
                // Create ripple effect
                const ripple = document.createElement('div');
                ripple.className = 'ripple-effect';
                sectionTitle.appendChild(ripple);
                const rect = sectionTitle.getBoundingClientRect();
                const size = Math.max(rect.width, rect.height);
                ripple.style.width = ripple.style.height = `${size}px`;
                ripple.style.left = `${event.clientX - rect.left - size / 2}px`;
                ripple.style.top = `${event.clientY - rect.top - size / 2}px`;
                setTimeout(() => ripple.remove(), 600);

                // Toggle section visibility if needed
                section.classList.toggle('active');
            });
        }
    });

    // Intersection Observer for scroll animations
    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            if (entry.isIntersecting) {
                entry.target.classList.add('in-view');
            }
        });
    }, { threshold: 0.1 });

    // Observe all terms sections and paragraphs
    termsSections.forEach((section) => observer.observe(section));
    document.querySelectorAll('.terms-text p, .terms-text ul, .contact-item').forEach(
        (element) => observer.observe(element)
    );

    // Animate hero section elements
    const heroElements = document.querySelectorAll('.terms-hero-content h1, .terms-hero-content p');
    heroElements.forEach((el, index) => {
        el.style.animationDelay = `${index * 0.2}s`;
        el.classList.add('animate-in');
    });

    // Helper: Check if element is in viewport
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