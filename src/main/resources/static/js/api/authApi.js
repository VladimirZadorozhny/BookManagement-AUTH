/**
 * Service layer for Authentication related API interactions.
 */
window.authApi = {
    /**
     * Standard login using form-encoded data.
     * @param {Object} credentials - Object with username and password.
     */
    login(credentials) {
        // Convert plain object to URLSearchParams for standard Spring Form Login
        const params = new URLSearchParams();
        for (const key in credentials) {
            params.append(key, credentials[key]);
        }

        // Use api.request to get consistent network error handling
        return api.request('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: params
        });
    },

    register(payload) {
        return api.post('/api/auth/register', payload);
    },

    requestPasswordReset(email) {
        return api.post('/api/auth/password-reset-request', { email });
    },

    confirmPasswordReset(payload) {
        return api.post('/api/auth/reset-password', payload);
    },

    getCurrentUser() {
        return api.get('/api/auth/me');
    }
};
