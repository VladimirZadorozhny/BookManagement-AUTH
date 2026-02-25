/**
 * Controller for the Register page.
 */
class RegisterPage {
    constructor() {
        this.registerForm = byId('registerForm');
        this.errorDiv = byId('registerError');
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
        
        try {
            const response = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (response.ok) {
                await modal.alert('Registration successful! You can now log in.', 'Success');
                window.location.href = '/login';
            } else {
                const err = await response.json();
                this.errorDiv.textContent = err.message || 'Registration failed.';
                this.errorDiv.classList.remove('d-none');
            }
        } catch (error) {
            console.error("Register Network Error:", error);
            this.errorDiv.textContent = 'A network error occurred. Please try again.';
            this.errorDiv.classList.remove('d-none');
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if (byId('registerForm')) {
        new RegisterPage().init();
    }
});
