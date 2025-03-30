document.addEventListener('DOMContentLoaded', function() {
    // Header scroll effect (copied from headerScroll.js)
    window.addEventListener('scroll', function () {
        if (window.scrollY > 50) {
            document.getElementById('header').classList.add('scrolled');
        } else {
            document.getElementById('header').classList.remove('scrolled');
        }
    });

    // Add animation index to each FAQ item
    const faqItems = document.querySelectorAll('.faq-item');
    faqItems.forEach((item, index) => {
        item.style.setProperty('--item-index', index);

        const question = item.querySelector('.faq-question');

        question.addEventListener('click', () => {
            // Add click ripple effect
            const ripple = document.createElement('div');
            ripple.className = 'ripple-effect';
            question.appendChild(ripple);

            const rect = question.getBoundingClientRect();
            const size = Math.max(rect.width, rect.height);
            ripple.style.width = ripple.style.height = `${size}px`;
            ripple.style.left = `${event.clientX - rect.left - size/2}px`;
            ripple.style.top = `${event.clientY - rect.top - size/2}px`;

            // Remove ripple after animation completes
            setTimeout(() => {
                ripple.remove();
            }, 600);

            // Toggle current item with smooth animation
            if (item.classList.contains('active')) {
                item.classList.remove('active');
            } else {
                // Close all other items
                faqItems.forEach(otherItem => {
                    if (otherItem !== item && otherItem.classList.contains('active')) {
                        otherItem.classList.remove('active');
                    }
                });

                // Add active class to current item
                item.classList.add('active');

                // Smooth scroll to the item if it's not visible
                if (!isElementInViewport(item)) {
                    item.scrollIntoView({
                        behavior: 'smooth',
                        block: 'center'
                    });
                }
            }
        });
    });

    // Open first FAQ item by default
    if (faqItems.length > 0) {
        faqItems[0].classList.add('active');
    }

    // Add scroll animations
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('in-view');
            }
        });
    }, {
        threshold: 0.1
    });

    // Observe all FAQ items for scroll animation
    faqItems.forEach(item => {
        observer.observe(item);
    });

    // Helper function to check if element is in viewport
    function isElementInViewport(el) {
        const rect = el.getBoundingClientRect();
        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
            rect.right <= (window.innerWidth || document.documentElement.clientWidth)
        );
    }

    // Add CSS for the ripple effect
    const style = document.createElement('style');
    style.textContent = `
        .faq-question {
            position: relative;
            overflow: hidden;
        }
        
        .ripple-effect {
            position: absolute;
            border-radius: 50%;
            background-color: rgba(255, 255, 255, 0.3);
            transform: scale(0);
            animation: ripple 0.6s linear;
            z-index: 0;
        }
        
        @keyframes ripple {
            to {
                transform: scale(2);
                opacity: 0;
            }
        }
        
        .faq-item.in-view {
            opacity: 1;
            transform: translateY(0);
        }
    `;
    document.head.appendChild(style);
});