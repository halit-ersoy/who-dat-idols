(function () {
    var savedTheme = localStorage.getItem('wdi-theme') || 'green';
    if (savedTheme !== 'green') {
        document.documentElement.setAttribute('data-theme', savedTheme);
    }

    // --- CSRF PROTECTION INTEGRATION ---
    
    // 1. Helper to extract cookies
    function getCookie(name) {
        var value = "; " + document.cookie;
        var parts = value.split("; " + name + "=");
        if (parts.length === 2) return parts.pop().split(";").shift();
    }

    // 2. Global Fetch Interceptor to append X-XSRF-TOKEN header to state-changing requests
    var originalFetch = window.fetch;
    window.fetch = function (input, init) {
        init = init || {};
        var method = (init.method || 'GET').toUpperCase();
        
        // Intercept state-changing requests
        if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
            var token = getCookie('XSRF-TOKEN');
            if (token) {
                init.headers = init.headers || {};
                if (init.headers instanceof Headers) {
                    init.headers.set('X-XSRF-TOKEN', token);
                } else if (Array.isArray(init.headers)) {
                    if (!init.headers.some(function(h) { return h[0].toUpperCase() === 'X-XSRF-TOKEN'; })) {
                        init.headers.push(['X-XSRF-TOKEN', token]);
                    }
                } else {
                    init.headers['X-XSRF-TOKEN'] = token;
                }
            }
        }
        return originalFetch(input, init);
    };

    // 3. Global Submit Event Listener to inject hidden _csrf input field to native POST forms
    document.addEventListener('submit', function (e) {
        var form = e.target;
        if (form && form.method && form.method.toUpperCase() === 'POST') {
            var csrfInput = form.querySelector('input[name="_csrf"]');
            if (!csrfInput) {
                var token = getCookie('XSRF-TOKEN');
                if (token) {
                    csrfInput = document.createElement('input');
                    csrfInput.type = 'hidden';
                    csrfInput.name = '_csrf';
                    csrfInput.value = token;
                    form.appendChild(csrfInput);
                }
            }
        }
    });
})();
