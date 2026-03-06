/**
 * Api - Centralized fetch wrapper for consistent headers and error handling.
 */
class Api {
    async request(url, options = {}) {
        try {
            const method = options.method || "GET";
            const headers = {
                "Content-Type": "application/json",
                ...options.headers
            };

            // Automatically include CSRF token for non-GET requests
            if (method !== "GET") {
                const csrfToken = this.getCookie("XSRF-TOKEN");
                if (csrfToken) {
                    headers["X-XSRF-TOKEN"] = csrfToken;
                }
            }

            const response = await fetch(url, {
                ...options,
                headers: headers
            });

            return response;
        } catch (err) {
            console.error("API Network Error:", err);
            if (window.modal) {
                await window.modal.error("A network error occurred. Please check your connection.");
            }
            throw err;
        }
    }

    get(url) {
        return this.request(url);
    }

    post(url, body) {
        return this.request(url, {
            method: "POST",
            body: body instanceof FormData ? body : JSON.stringify(body),
            // FormData should not have Content-Type: application/json
            headers: body instanceof FormData ? {} : { "Content-Type": "application/json" }
        });
    }

    put(url, body) {
        return this.request(url, {
            method: "PUT",
            body: JSON.stringify(body)
        });
    }

    delete(url) {
        return this.request(url, {
            method: "DELETE"
        });
    }

    /**
     * Extracts and displays a user-friendly error message from the API response.
     */
    async showError(response, fallback = "An unexpected error occurred.") {
        let message = fallback;

        try {
            const json = await response.json();
            if (json && json.message) {
                message = json.message;
            }
        } catch (e) {
            console.warn("Could not parse error response JSON", e);
        }

        if (window.modal) {
            await window.modal.error(message);
        } else {
            alert(message);
        }
    }

    /**
     * Simple cookie reader helper.
     */
    getCookie(name) {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) return parts.pop().split(';').shift();
        return null;
    }
}

// Initialize and attach to window for global access
window.api = new Api();
