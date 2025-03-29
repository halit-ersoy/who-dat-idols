// faq.js
document.addEventListener('DOMContentLoaded', function() {
    // Header scroll effect (copied from headerScroll.js)
    window.addEventListener('scroll', function () {
        if (window.scrollY > 50) {
            document.getElementById('header').classList.add('scrolled');
        } else {
            document.getElementById('header').classList.remove('scrolled');
        }
    });

    // FAQ accordion functionality
    const faqItems = document.querySelectorAll('.faq-item');

    faqItems.forEach(item => {
        const question = item.querySelector('.faq-question');

        question.addEventListener('click', () => {
            // Close all other items
            faqItems.forEach(otherItem => {
                if (otherItem !== item && otherItem.classList.contains('active')) {
                    otherItem.classList.remove('active');
                }
            });

            // Toggle current item
            item.classList.toggle('active');
        });
    });

    // Open first FAQ item by default
    if (faqItems.length > 0) {
        faqItems[0].classList.add('active');
    }
});