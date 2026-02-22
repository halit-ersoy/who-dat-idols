export function initNotifications() {
    const notificationsBtn = document.getElementById('notifications-btn');
    const notificationsDropdown = document.getElementById('notifications-dropdown');
    const notificationBadge = document.getElementById('notification-badge');
    const notificationsList = document.getElementById('notifications-list');

    if (!notificationsBtn || !notificationsDropdown) return;

    // Fetch notifications on load
    fetchNotifications();

    // Toggle dropdown
    notificationsBtn.addEventListener('click', (e) => {
        // Prevent closing when clicking inside the dropdown
        if (e.target.closest('.notifications-dropdown')) return;

        notificationsBtn.classList.toggle('active');
        notificationsDropdown.classList.toggle('active');
    });

    // Close on click outside
    document.addEventListener('click', (e) => {
        if (!notificationsBtn.contains(e.target)) {
            notificationsBtn.classList.remove('active');
            notificationsDropdown.classList.remove('active');
        }
    });

    async function fetchNotifications() {
        try {
            const response = await fetch('/api/notifications?limit=10');
            const notifications = await response.json();

            updateNotificationUI(notifications);
        } catch (error) {
            console.error('Bildirimler yüklenemedi:', error);
        }
    }

    function updateNotificationUI(notifications) {
        if (!notifications || notifications.length === 0) {
            notificationsList.innerHTML = '<div class="notification-empty">Henüz bildirim yok.</div>';
            notificationBadge.style.display = 'none';
            return;
        }

        // We still show the badge for all notifications for now, but user said "just notifications"
        // I'll keep the unread count logic for the badge but removing the "mark as read" feature
        const unreadCount = notifications.filter(n => !n.read).length;
        if (unreadCount > 0) {
            notificationBadge.textContent = unreadCount;
            notificationBadge.style.display = 'flex';
        } else {
            notificationBadge.style.display = 'none';
        }

        notificationsList.innerHTML = '';
        notifications.forEach(notification => {
            const item = document.createElement('div');
            item.className = 'notification-item'; // Simple item class

            const timeStr = formatTime(new Date(notification.createdAt));
            const icon = notification.type === 'Movie' ? 'fas fa-film' : 'fas fa-tv';
            const link = `/${notification.slug || notification.contentId}`;

            item.innerHTML = `
                <div class="notification-icon">
                    <i class="${icon}"></i>
                </div>
                <div class="notification-content">
                    <div class="notification-title">${notification.title}</div>
                    <div class="notification-message">${notification.message}</div>
                    <div class="notification-time">${timeStr}</div>
                </div>
            `;

            item.addEventListener('click', () => {
                window.location.href = link;
            });

            notificationsList.appendChild(item);
        });
    }

    // Removed markAsRead function


    function formatTime(date) {
        const now = new Date();
        const diffInMs = now - date;
        const diffInMins = Math.floor(diffInMs / (1000 * 60));
        const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
        const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));

        if (diffInMins < 1) return 'Az önce';
        if (diffInMins < 60) return `${diffInMins} dakika önce`;
        if (diffInHours < 24) return `${diffInHours} saat önce`;
        if (diffInDays < 7) return `${diffInDays} gün önce`;

        return date.toLocaleDateString('tr-TR');
    }
}
