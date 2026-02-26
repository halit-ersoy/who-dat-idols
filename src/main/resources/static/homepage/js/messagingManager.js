/* messagingManager.js */

export function initMessagingManager() {
    updateMessageBadge();

    const isMessagesPage = window.location.pathname.includes('/messages');
    const pollInterval = isMessagesPage ? 5000 : 60000;

    // Periodically update badge based on current page
    setInterval(updateMessageBadge, pollInterval);
}

export function updateMessageBadge() {
    const badge = document.getElementById('message-badge');
    if (!badge) return;

    fetch('/api/messages/conversations')
        .then(res => {
            if (!res.ok) throw new Error('Not logged in');
            return res.json();
        })
        .then(conversations => {
            const myNickname = localStorage.getItem('wdiUserNickname');
            const unreadCount = conversations.filter(c => !c.read && c.receiverNickname === myNickname).length;

            if (unreadCount > 0) {
                badge.textContent = unreadCount > 99 ? '99+' : unreadCount;
                badge.style.display = 'flex';
            } else {
                badge.style.display = 'none';
            }
        })
        .catch(() => {
            badge.style.display = 'none';
        });
}
