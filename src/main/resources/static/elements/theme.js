(function () {
    var savedTheme = localStorage.getItem('wdi-theme') || 'green';
    if (savedTheme !== 'green') {
        document.documentElement.setAttribute('data-theme', savedTheme);
    }
})();
