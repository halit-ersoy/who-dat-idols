// comments.js
export function initCommentsSection(videoId) {
    const commentForm  = document.getElementById('commentForm');
    const commentsList = document.getElementById('comments-list');
    const commentText  = document.getElementById('commentText');

    if (!commentsList) return;

    setupCharCounter();
    commentForm?.addEventListener('submit', onSubmitComment);
    loadComments();

    function setupCharCounter() {
        if (!commentText) return;
        const counter = document.createElement('div');
        counter.className = 'char-counter';
        commentText.parentNode.appendChild(counter);

        commentText.addEventListener('input', () => {
            const len = commentText.value.length;
            counter.textContent = `${len}/500`;
            counter.style.color = len > 500 ? '#ff3860' : '#888';
        });

        commentText.addEventListener('focus', () => {
            if (!localStorage.getItem('wdiUserToken')) {
                commentText.blur();
                commentText.style.borderColor = '#ff3860';
                commentText.placeholder = 'Oturum açmalısınız!';
                setTimeout(() => commentText.placeholder = 'Düşüncelerinizi paylaşın...', 2000);
            }
        });

        commentText.addEventListener('blur', () => {
            commentText.placeholder = 'Bu dizi hakkında ne düşünüyorsunuz?';
        });
    }

    async function onSubmitComment(e) {
        e.preventDefault();
        const text = commentText?.value.trim() || '';
        if (!text || text.length > 500) {
            if (commentText) {
                commentText.style.borderColor = '#ff3860';
                setTimeout(() => commentText.style.borderColor = '', 1000);
            }
            return;
        }

        try {
            const res = await fetch(`/api/video/comment?id=${videoId}&spoiler=false`, {
                method: 'POST',
                headers: {'Content-Type': 'text/plain'},
                credentials: 'include',
                body: text
            });
            if (!res.ok) throw new Error();
            prependComment({ nickname: 'Kullanıcı', datetime: new Date().toISOString(), text, spoiler: false });
            incrementCommentCount();
            commentText.value = '';
        } catch {
            alert('Yorum gönderilemedi.');
        }
    }

    async function loadComments() {
        try {
            const res = await fetch(`/api/video/comments?id=${videoId}`);
            if (!res.ok) throw new Error();
            const data = await res.json();
            setCommentCount(data.length);
            commentsList.innerHTML = '';
            data
                .sort((a,b) => new Date(b.datetime) - new Date(a.datetime))
                .forEach(c => prependComment({
                    nickname: c.nickname,
                    datetime: c.datetime,
                    text: c.text,
                    spoiler: c.spoiler === 1
                }));
        } catch {
            commentsList.innerHTML = '<div class="error-message">Yorumlar yüklenirken hata oluştu.</div>';
        }
    }

    function prependComment({ nickname, datetime, text, spoiler }) {
        const card = document.createElement('div');
        card.className = 'comment-card';
        const dateStr = new Date(datetime).toLocaleDateString('tr-TR', {
            year:'numeric', month:'long', day:'numeric',
            hour:'2-digit', minute:'2-digit'
        });

        card.innerHTML = `
      <div class="comment-header">
        <div class="user-avatar"><i class="fas fa-user"></i></div>
        <div class="comment-user">${nickname}</div>
        <div class="comment-date">${dateStr}</div>
      </div>
      ${spoiler
            ? `<div class="spoiler-warning">
             <i class="fas fa-exclamation-triangle"></i> Spoiler
             <button class="show-spoiler-btn">Göster</button>
           </div>
           <div class="comment-content hidden">${text}</div>`
            : `<div class="comment-content">${text}</div>`}
      <div class="comment-actions">
        <button class="action-button like-btn"><i class="far fa-heart"></i> <span>0</span></button>
        <button class="action-button"><i class="far fa-comment"></i> Yanıtla</button>
      </div>
    `;

        // Beğeni butonu
        const likeBtn = card.querySelector('.like-btn');
        likeBtn.onclick = () => {
            const icon = likeBtn.querySelector('i');
            const cnt  = likeBtn.querySelector('span');
            const liked = icon.classList.contains('far');
            icon.classList.toggle('far', !liked);
            icon.classList.toggle('fas', liked);
            likeBtn.classList.toggle('liked', liked);
            cnt.textContent = +cnt.textContent + (liked ? 1 : -1);
            icon.classList.add('like-animation');
            setTimeout(() => icon.classList.remove('like-animation'), 500);
        };

        // Spoiler aç
        const spoilerBtn = card.querySelector('.show-spoiler-btn');
        spoilerBtn?.addEventListener('click', () => {
            card.querySelector('.spoiler-warning').remove();
            card.querySelector('.comment-content').classList.remove('hidden');
        });

        commentsList.prepend(card);
    }

    function setCommentCount(count) {
        let span = document.querySelector('.comment-count');
        if (span) {
            span.textContent = count;
        } else {
            document.querySelector('.comments-section h3')
                .insertAdjacentHTML('beforeend', `<span class="comment-count">${count}</span>`);
        }
    }

    function incrementCommentCount() {
        const span = document.querySelector('.comment-count');
        if (span) span.textContent = +span.textContent + 1;
    }
}
