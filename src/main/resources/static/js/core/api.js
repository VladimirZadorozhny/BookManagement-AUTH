/**
 * Api - Centralized fetch wrapper for consistent headers and error handling.
 */
class Api {
    async request(url, options = {}) {
        try {
            const response = await fetch(url, {
                headers: {
                    "Content-Type": "application/json",
                    ...options.headers
                },
                ...options
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
}

// Initialize and attach to window for global access
window.api = new Api();
