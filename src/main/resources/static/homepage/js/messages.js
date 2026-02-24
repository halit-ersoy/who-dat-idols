/* messages.js */
import { showReportModal } from '../../elements/premium-modals.js';


document.addEventListener('DOMContentLoaded', () => {
    const userSearchInput = document.getElementById('user-search-input');
    const userSearchResults = document.getElementById('user-search-results');
    const conversationsList = document.getElementById('conversations-list');
    const chatArea = document.getElementById('chat-area');

    let currentReceiver = null;
    let currentReceiverRole = null;
    let chatInterval = null;

    function getRoleBadge(role) {
        if (role === 'ADMIN') {
            return '<span class="admin-badge"><i class="fas fa-shield-alt"></i> ADMİN</span>';
        } else if (role === 'SUPER_ADMIN') {
            return '<span class="super-admin-badge"><i class="fas fa-crown"></i> SUPER ADMİN</span>';
        } else if (role === 'KURUCU') {
            return '<span class="founder-badge"><i class="fas fa-crown"></i> KURUCU</span>';
        } else if (role === 'GELISTIRICI') {
            return '<span class="developer-badge"><i class="fas fa-code"></i> GELİŞTİRİCİ</span>';
        } else if (role === 'CEVIRMEN') {
            return '<span class="translator-badge"><i class="fas fa-language"></i> ÇEVİRMEN</span>';
        } else if (role === 'USER' || !role) {
            return '';
        }
        return '';
    }

    // 1. Initial Load
    fetchConversations();

    // 2. User Search Logic
    let searchTimeout = null;
    userSearchInput.addEventListener('input', () => {
        clearTimeout(searchTimeout);
        const query = userSearchInput.value.trim();

        if (query.length < 2) {
            userSearchResults.classList.remove('active');
            return;
        }

        searchTimeout = setTimeout(() => {
            fetch(`/api/users/search-messaging?q=${query}`)
                .then(res => res.json())
                .then(users => {
                    renderSearchResults(users);
                });
        }, 300);
    });

    function renderSearchResults(users) {
        if (!users || users.length === 0) {
            userSearchResults.innerHTML = '<div style="padding: 10px; font-size: 0.8rem; color: #888;">Sonuç bulunamadı.</div>';
        } else {
            userSearchResults.innerHTML = users.map(user => `
                <div class="search-item" data-nickname="${user.nickname}" data-role="${user.role || ''}">
                    <div class="avatar">${user.nickname[0].toUpperCase()}</div>
                    <div class="info">
                        <div style="display:flex; align-items:center; gap:5px;">
                            <div class="nickname">${user.nickname}</div>
                            ${getRoleBadge(user.role)}
                        </div>
                        <div class="name" style="font-size: 0.75rem; color: #888;">${user.name} ${user.surname}</div>
                    </div>
                </div>
            `).join('');

            userSearchResults.querySelectorAll('.search-item').forEach(item => {
                item.addEventListener('click', () => {
                    const nickname = item.dataset.nickname;
                    const role = item.dataset.role;
                    startConversation(nickname, role);
                    userSearchResults.classList.remove('active');
                    userSearchInput.value = '';
                });
            });
        }
        userSearchResults.classList.add('active');
    }

    // Close search on click outside
    document.addEventListener('click', (e) => {
        if (!userSearchInput.contains(e.target) && !userSearchResults.contains(e.target)) {
            userSearchResults.classList.remove('active');
        }
    });

    // 3. Conversations Logic
    function fetchConversations() {
        fetch('/api/messages/conversations')
            .then(res => res.json())
            .then(conversations => {
                renderConversations(conversations);
            });
    }

    function renderConversations(conversations) {
        if (!conversations || conversations.length === 0) {
            conversationsList.innerHTML = '<div class="chat-empty-state" style="padding: 20px;"><p style="font-size: 0.9rem;">Henüz bir sohbet yok.</p></div>';
            return;
        }

        const myNickname = localStorage.getItem('wdiUserNickname');

        conversationsList.innerHTML = conversations.map(c => {
            const otherUser = c.senderNickname === myNickname ? c.receiverNickname : c.senderNickname;
            const isActive = otherUser === currentReceiver;
            const isUnread = !c.read && c.receiverNickname === myNickname;
            const time = formatTime(new Date(c.timestamp));
            const isSentByMe = c.senderNickname === myNickname;
            const statusIcon = isSentByMe ? (c.read ? '<i class="fas fa-check-double message-status-tick read" style="font-size: 0.7rem; margin-right: 3px;"></i>' : '<i class="fas fa-check message-status-tick" style="font-size: 0.7rem; margin-right: 3px;"></i>') : '';

            return `
                <div class="conversation-item ${isActive ? 'active' : ''} ${isUnread ? 'unread' : ''}" data-nickname="${otherUser}">
                    <div class="avatar">${otherUser[0].toUpperCase()}</div>
                    <div class="conversation-info">
                        <div class="conversation-top">
                            <div style="display: flex; align-items: center; gap: 4px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; flex: 1;">
                                <span class="nickname" style="flex-shrink: 0;">${otherUser}</span>
                                ${getRoleBadge(c.senderNickname === myNickname ? c.receiverRole : c.senderRole)}
                            </div>
                            <span class="time" style="flex-shrink: 0; margin-left: 8px;">${time}</span>
                        </div>
                        <div class="last-message">${statusIcon}${c.content}</div>
                    </div>
                </div>
            `;
        }).join('');

        conversationsList.querySelectorAll('.conversation-item').forEach(item => {
            item.addEventListener('click', () => {
                const nickname = item.dataset.nickname;
                const conv = conversations.find(c => (c.senderNickname === myNickname ? c.receiverNickname : c.senderNickname) === nickname);
                const role = conv ? (conv.senderNickname === myNickname ? conv.receiverRole : conv.senderRole) : null;
                startConversation(nickname, role);
            });
        });
    }

    // 4. Chat Logic
    function startConversation(nickname, role = null) {
        currentReceiver = nickname;
        currentReceiverRole = role;

        // Update UI
        chatArea.innerHTML = `
            <div class="chat-header">
                <div class="receiver-avatar">${nickname[0].toUpperCase()}</div>
                <div class="receiver-info">
                    <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                        <h3 style="margin: 0; font-size: 1.1rem;">${nickname}</h3>
                        ${getRoleBadge(role)}
                    </div>
                </div>
                <div class="chat-header-actions" id="chat-actions">
                    <button class="action-btn block-btn" id="block-btn" title="Kullanıcıyı Engelle">
                        <i class="fas fa-ban"></i>
                    </button>
                    <button class="action-btn report-btn" id="report-btn" title="Kullanıcıyı Bildir">
                        <i class="fas fa-flag"></i>
                    </button>
                </div>
            </div>
            <div class="chat-messages" id="chat-messages-container">
                <div style="text-align:center; padding: 20px; color: #888;">Yükleniyor...</div>
            </div>
            <div class="chat-input-area" id="chat-input-area">
                <div class="chat-input-wrapper">
                    <textarea id="message-input" placeholder="Bir mesaj yazın..." rows="1" maxlength="400"></textarea>
                    <div class="char-counter"><span id="char-count">0</span> / 400</div>
                    <button class="send-btn" id="send-message-btn" disabled>
                        <i class="fas fa-paper-plane"></i>
                    </button>
                </div>
            </div>
        `;

        updateBlockStatus(nickname);

        const blockBtn = document.getElementById('block-btn');
        const reportBtn = document.getElementById('report-btn');

        blockBtn.addEventListener('click', () => toggleBlock(nickname));
        reportBtn.addEventListener('click', () => showReportModalHelper(nickname));

        renderConversations([]); // Refresh sidebar highlights
        fetchConversations();

        loadChatHistory(nickname);

        // Setup interval for real-time-like updates
        clearInterval(chatInterval);
        chatInterval = setInterval(() => {
            if (currentReceiver === nickname) {
                loadChatHistory(nickname, true);
            }
        }, 5000);

        // Setup input event
        const input = document.getElementById('message-input');
        const sendBtn = document.getElementById('send-message-btn');

        input.addEventListener('input', () => {
            const val = input.value;
            document.getElementById('char-count').innerText = val.length;
            sendBtn.disabled = !val.trim() || val.length > 400;
            // Auto resize textarea
            input.style.height = 'auto';
            input.style.height = (input.scrollHeight) + 'px';
        });

        sendBtn.addEventListener('click', () => sendMessage(nickname, input.value.trim()));
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendBtn.click();
            }
        });
    }

    function loadChatHistory(nickname, silent = false) {
        fetch(`/api/messages/history/${nickname}`)
            .then(res => res.json())
            .then(messages => {
                const container = document.getElementById('chat-messages-container');
                if (!container) return;

                const myNickname = localStorage.getItem('wdiUserNickname');
                const wasAtBottom = container.scrollHeight - container.scrollTop <= container.clientHeight + 100;

                container.innerHTML = messages.map(m => {
                    const isSent = m.senderNickname === myNickname;
                    const statusIcon = m.read ? '<i class="fas fa-check-double message-status-tick read"></i>' : '<i class="fas fa-check message-status-tick"></i>';

                    return `
                        <div class="message-bubble ${isSent ? 'sent' : 'received'}">
                            ${m.content}
                            <div class="message-footer">
                                <span class="message-time">${formatTime(new Date(m.timestamp))}</span>
                                ${isSent ? statusIcon : ''}
                            </div>
                        </div>
                    `;
                }).join('');

                if (!silent || wasAtBottom) {
                    container.scrollTop = container.scrollHeight;
                }
            });
    }

    function sendMessage(nickname, content) {
        if (!content) return;

        const sendBtn = document.getElementById('send-message-btn');
        const input = document.getElementById('message-input');

        sendBtn.disabled = true;

        fetch('/api/messages/send', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ receiverNickname: nickname, content: content })
        })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    input.value = '';
                    input.style.height = 'auto';
                    loadChatHistory(nickname);
                    fetchConversations();
                } else {
                    alert("Mesaj gönderilemedi: " + data.message);
                    sendBtn.disabled = false;
                }
            });
    }

    function formatTime(date) {
        const now = new Date();
        const isToday = date.toDateString() === now.toDateString();

        if (isToday) {
            return date.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' });
        } else {
            return date.toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit' });
        }
    }

    /* Safety & Blocking */
    function updateBlockStatus(nickname) {
        fetch(`/api/messages/block-status/${nickname}`)
            .then(res => res.json())
            .then(status => {
                const blockBtn = document.getElementById('block-btn');
                const chatInputArea = document.getElementById('chat-input-area');
                if (!blockBtn || !chatInputArea) return;

                if (status.blockedByMe) {
                    blockBtn.innerHTML = '<i class="fas fa-user-slash"></i>';
                    blockBtn.classList.add('active');
                    blockBtn.title = "Engeli Kaldır";
                } else {
                    blockBtn.innerHTML = '<i class="fas fa-ban"></i>';
                    blockBtn.classList.remove('active');
                    blockBtn.title = "Kullanıcıyı Engelle";
                }

                if (status.blockedByMe || status.blockedByThem) {
                    chatInputArea.innerHTML = `
                        <div class="blocked-message">
                            ${status.blockedByMe ? 'Bu kullanıcıyı engellediniz.' : 'Bu kullanıcı sizi engelledi.'}
                            Mesaj gönderemezsiniz.
                        </div>
                    `;
                }
            });
    }

    function toggleBlock(nickname) {
        const btn = document.getElementById('block-btn');
        const isBlocked = btn.classList.contains('active');
        const endpoint = isBlocked ? '/api/messages/unblock' : '/api/messages/block';
        const msg = isBlocked ? "Engeli kaldırmak istiyor musunuz?" : "Bu kullanıcıyı engellemek istiyor musunuz? Birbirinize mesaj gönderemezsiniz.";

        if (!confirm(msg)) return;

        fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nickname })
        }).then(res => res.json()).then(data => {
            if (data.success) {
                startConversation(nickname);
            }
        });
    }

    function showReportModalHelper(nickname) {
        showReportModal(nickname, 'Chat', (reason) => {
            return fetch('/api/user/report', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ nickname, reason, context: 'Chat' })
            }).then(res => res.ok);
        });
    }
});
