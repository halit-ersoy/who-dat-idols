// calendar.js
export function initCalendar() {
    const calendarModal = document.getElementById('calendar-modal');
    const closeCalendarButton = document.getElementById('close-calendar-modal');

    // Find the Takvim link
    const navLinks = document.querySelectorAll('a.nav-link');
    let takvimLink;

    navLinks.forEach(link => {
        if (link.textContent.trim() === 'Takvim') {
            takvimLink = link;
        }
    });

    if (takvimLink) {
        takvimLink.addEventListener('click', (e) => {
            e.preventDefault();
            loadCalendarData();
            calendarModal.style.display = 'flex';
            document.body.style.overflow = 'hidden';
        });
    }

    if (closeCalendarButton) {
        closeCalendarButton.addEventListener('click', () => {
            calendarModal.style.display = 'none';
            document.body.style.overflow = '';
        });
    }

    // Close when clicking outside the modal
    calendarModal.addEventListener('click', (e) => {
        if (e.target === calendarModal) {
            calendarModal.style.display = 'none';
            document.body.style.overflow = '';
        }
    });

    // Add keyboard support for accessibility
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && calendarModal.style.display === 'flex') {
            calendarModal.style.display = 'none';
            document.body.style.overflow = '';
        }
    });

    function loadCalendarData() {
        fetch('/api/calendar')
            .then(response => response.json())
            .then(calendarData => {
                // Setup the drawer structure
                createDrawerStructure(calendarData);
            })
            .catch(error => console.error('Error loading calendar data:', error));
    }

    function createDrawerStructure(calendarData) {
        const weeklyCalendar = document.querySelector('.weekly-calendar');
        if (!weeklyCalendar) return;

        // Clear previous content
        weeklyCalendar.innerHTML = '';

        const dayNames = {
            'monday': 'Pazartesi',
            'tuesday': 'Salı',
            'wednesday': 'Çarşamba',
            'thursday': 'Perşembe',
            'friday': 'Cuma',
            'saturday': 'Cumartesi',
            'sunday': 'Pazar'
        };

        const daysOrder = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday'];
        const days = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday'];
        const today = days[new Date().getDay()];

        // Create drawer for each day in specific order
        daysOrder.forEach(day => {
            const shows = calendarData[day] || [];
            const dayEl = document.createElement('div');
            dayEl.className = 'calendar-day';
            dayEl.dataset.day = day;

            // Create header (clickable part)
            const header = document.createElement('div');
            header.className = 'calendar-day-header';
            header.innerHTML = `
                <h3>${dayNames[day]} <span class="show-count">${shows.length}</span></h3>
                <i class="fas fa-chevron-down"></i>
            `;

            // Create content container
            const content = document.createElement('div');
            content.className = 'calendar-day-content';
            content.id = `${day}-shows`;

            if (shows.length === 0) {
                content.innerHTML = '<p class="no-shows">Bugün için program yok</p>';
            } else {
                shows.forEach(show => {
                    const showEl = document.createElement('div');
                    showEl.className = 'calendar-show';
                    showEl.innerHTML = `
                        <div class="calendar-show-info">
                            <h4 class="calendar-show-title">${show.title}</h4>
                            <p class="calendar-show-episode">${show.episode}</p>
                            <p class="calendar-show-time">${show.time}</p>
                        </div>
                    `;
                    content.appendChild(showEl);
                });
            }

            dayEl.appendChild(header);
            dayEl.appendChild(content);
            weeklyCalendar.appendChild(dayEl);

            // Add click event
            header.addEventListener('click', function () {
                toggleDrawer(day);
            });
        });

        // Open today's drawer by default
        setTimeout(() => {
            toggleDrawer(today);
        }, 100);
    }

    function toggleDrawer(dayToOpen) {
        const allDays = document.querySelectorAll('.calendar-day');

        // Close all drawers first
        allDays.forEach(day => {
            const header = day.querySelector('.calendar-day-header');
            const content = day.querySelector('.calendar-day-content');
            const icon = header.querySelector('i');

            day.classList.remove('active');
            content.style.maxHeight = null;
            icon.className = 'fas fa-chevron-down';
        });

        // Open the selected drawer
        const selectedDay = document.querySelector(`.calendar-day[data-day="${dayToOpen}"]`);
        if (selectedDay) {
            const header = selectedDay.querySelector('.calendar-day-header');
            const content = selectedDay.querySelector('.calendar-day-content');
            const icon = header.querySelector('i');

            selectedDay.classList.add('active');
            content.style.maxHeight = content.scrollHeight + 'px';
            icon.className = 'fas fa-chevron-up';
        }
    }
}