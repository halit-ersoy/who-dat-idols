document.addEventListener('DOMContentLoaded', () => {
    // Header scroll effect
    window.addEventListener('scroll', () => {
        const header = document.getElementById('header');
        if (header) {
            if (window.scrollY > 50) {
                header.classList.add('scrolled');
            } else {
                header.classList.remove('scrolled');
            }
        }
    });

    // Privacy items: Add animation delays to each item
    const privacyItems = document.querySelectorAll('.privacy-item');
    privacyItems.forEach((item, index) => {
        item.style.setProperty('--item-index', index);

        // Add ripple effect to item headers when clicked
        const itemHeader = item.querySelector('h2');
        if (itemHeader) {
            itemHeader.addEventListener('click', (event) => {
                // Create ripple effect
                const ripple = document.createElement('div');
                ripple.className = 'ripple-effect';
                itemHeader.appendChild(ripple);
                const rect = itemHeader.getBoundingClientRect();
                const size = Math.max(rect.width, rect.height);
                ripple.style.width = ripple.style.height = `${size}px`;
                ripple.style.left = `${event.clientX - rect.left - size / 2}px`;
                ripple.style.top = `${event.clientY - rect.top - size / 2}px`;
                setTimeout(() => ripple.remove(), 600);

                // Toggle item visibility if needed
                item.classList.toggle('active');
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

    // Observe all privacy items and paragraphs
    privacyItems.forEach((item) => observer.observe(item));
    document.querySelectorAll('.privacy-item p, .privacy-item ul, .contact-item').forEach(
        (element) => observer.observe(element)
    );

    // Animate hero section elements
    const heroElements = document.querySelectorAll('.privacy-hero-content h1, .privacy-hero-content p');
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