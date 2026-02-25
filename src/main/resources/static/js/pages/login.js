/**
 * Controller for the Login page.
 */
class LoginPage {
    constructor() {
        this.loginForm = byId('loginForm');
        this.errorDiv = byId('loginError');
    }

    init() {
        this.bindEvents();
    }

    bindEvents() {
        this.loginForm?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleLogin();
        });
    }

    async handleLogin() {
        this.errorDiv.classList.add('d-none');
        const formData = new FormData(this.loginForm);
        
        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                body: formData
            });

            if (response.ok) {
                window.location.href = '/';
            } else {
                const err = await response.json();
                this.errorDiv.textContent = err.message || 'Invalid email or password.';
                this.errorDiv.classList.remove('d-none');
            }
        } catch (error) {
            console.error("Login Network Error:", error);
            this.errorDiv.textContent = 'A network error occurred. Please try again.';
            this.errorDiv.classList.remove('d-none');
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if (byId('loginForm')) {
        new LoginPage().init();
    }
});
