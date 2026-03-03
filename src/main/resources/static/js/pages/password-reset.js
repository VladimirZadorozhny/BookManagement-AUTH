/**
 * Controller for Password Reset functionality.
 */
class PasswordReset {
    constructor() {
        this.linkForgotPassword = byId('linkForgotPassword');
        this.requestModalEl = byId('password-reset-request-modal');
        this.requestModal = this.requestModalEl ? new bootstrap.Modal(this.requestModalEl) : null;
        this.requestForm = byId('passwordResetRequestForm');
        this.btnSubmitRequest = byId('btnSubmitResetRequest');

        // Reset form (on the reset-password page)
        this.resetForm = byId('resetPasswordForm');
        this.btnSubmitReset = byId('btnSubmitReset');
    }

    init() {
        this.bindEvents();
    }

    bindEvents() {
        this.linkForgotPassword?.addEventListener('click', (e) => {
            e.preventDefault();
            this.requestModal.show();
        });

        this.btnSubmitRequest?.addEventListener('click', () => this.handleRequest());

        this.resetForm?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleReset();
        });
    }

    async handleRequest() {
        const payload = serializeForm(this.requestForm);
        if (!payload.email) return modal.error("Please enter your email.");

        await withLoading(this.btnSubmitRequest, async () => {
            try {
                const response = await authApi.requestPasswordReset(payload.email);

                // Hide modal FIRST to avoid overlap with the alert/error
                this.requestModal.hide();
                this.requestForm.reset();

                if (response.ok) {
                    await modal.alert("If an account exists for that email, a reset link has been sent.", "Request Sent");
                } else {
                    await api.showError(response, "Failed to request password reset.");
                }
            } catch (error) {
                // api.request already handles network errors with a modal
            }
        });
    }


    async handleReset() {
        const urlParams = new URLSearchParams(window.location.search);
        const token = urlParams.get('token');
        const payload = serializeForm(this.resetForm);

        if (!token) return modal.error("Invalid reset link. Missing token.");

        if (payload.newPassword !== payload.confirmPassword) {
            return modal.error("Passwords do not match.");
        }

        const data = {
            token: token,
            newPassword: payload.newPassword
        };

        await withLoading(this.btnSubmitReset, async () => {
            try {
                const response = await authApi.confirmPasswordReset(data);

                if (response.ok) {
                    await modal.alert("Your password has been reset successfully. You can now log in.", "Success");
                    window.location.href = '/login';
                } else {
                    await api.showError(response, "Failed to reset password.");
                }
            } catch (error) {
                // Network errors are handled by api.js modal
            }
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.passwordReset = new PasswordReset();
    window.passwordReset.init();
});
