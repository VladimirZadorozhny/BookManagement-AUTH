/**
 * Controller for the Register page.
 */
class RegisterPage {
    constructor() {
        this.registerForm = byId('registerForm');
        this.errorDiv = byId('registerError');
        this.submitBtn = this.registerForm?.querySelector('button[type="submit"]');
    }

    init() {
        this.bindEvents();
    }

    bindEvents() {
        this.registerForm?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleRegister();
        });
    }

    async handleRegister() {
        this.errorDiv.classList.add('d-none');
        const payload = serializeForm(this.registerForm);
        
        await withLoading(this.submitBtn, async () => {
            try {
                const response = await authApi.register(payload);

                if (response.ok) {
                    await modal.alert('Registration successful! Please check your email for a verification link to activate your account.', 'Verify Your Email');
                    window.location.href = '/login';
                } else {
                    await api.showError(response, 'Registration failed.');
                }
            } catch (error) {
                // Network error handled by api.js
            }
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if (byId('registerForm')) {
        new RegisterPage().init();
    }
});
