export function isUserLoggedIn() {
    const authToken = localStorage.getItem('wdiUserToken');
    const userNickname = localStorage.getItem('wdiUserNickname');
    return !!(authToken && userNickname);
}