/**
 * Controller for the Login page.
 */
class LoginPage {
    constructor() {
        this.loginForm = byId('loginForm');
        this.errorDiv = byId('loginError');
        this.submitBtn = this.loginForm?.querySelector('button[type="submit"]');
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
        const credentials = serializeForm(this.loginForm);
        
        await withLoading(this.submitBtn, async () => {
            try {
                const response = await authApi.login(credentials);

                if (response.ok) {
                    window.location.href = '/';
                } else {
                    await api.showError(response, 'Invalid email or password.');
                }
            } catch (error) {
                // api.request handles network error modals
            }
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if (byId('loginForm')) {
        new LoginPage().init();
    }
});
