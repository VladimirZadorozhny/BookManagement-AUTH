/**
 * ModalHelper - Centralized modal management using Bootstrap 5.
 * Resolves promises only after the 'hidden.bs.modal' event to ensure clean UI transitions.
 */
class ModalHelper {
    constructor() {
        this.modalEl = document.getElementById('modal-overlay');
        this.titleEl = document.getElementById('modal-title');
        this.messageEl = document.getElementById('modal-message');
        this.buttonsEl = document.getElementById('modal-buttons');

        if (this.modalEl) {
            this.instance = new bootstrap.Modal(this.modalEl);
        } else {
            console.warn("Modal overlay element not found. ModalHelper may not function correctly.");
        }
    }

    /**
     * Internal method to open the modal with specific configuration.
     */
    open({ title, message, buttons }) {
        if (!this.instance) return Promise.resolve(null);

        return new Promise(resolve => {
            this.titleEl.textContent = title;
            this.messageEl.textContent = message;
            this.buttonsEl.innerHTML = '';

            buttons.forEach(btnInfo => {
                const btn = document.createElement('button');
                btn.textContent = btnInfo.text;
                btn.className = `btn ${btnInfo.class || 'btn-primary'}`;

                btn.onclick = () => {
                    // Resolve ONLY after the modal is fully hidden to prevent animation conflicts
                    this.modalEl.addEventListener('hidden.bs.modal', () => {
                        resolve(btnInfo.value);
                    }, { once: true });

                    this.instance.hide();
                };

                this.buttonsEl.append(btn);
            });

            this.instance.show();
        });
    }

    /**
     * Simple alert modal.
     */
    alert(message, title = "Notice") {
        return this.open({
            title,
            message,
            buttons: [{ text: "OK", class: "btn-primary", value: true }]
        });
    }

    /**
     * Error modal with danger styling.
     */
    error(message, title = "Error") {
        return this.open({
            title,
            message,
            buttons: [{ text: "OK", class: "btn-danger", value: true }]
        });
    }

    /**
     * Confirm modal with two choices.
     */
    confirm(message, title = "Confirm") {
        return this.open({
            title,
            message,
            buttons: [
                { text: "Cancel", class: "btn-secondary", value: false },
                { text: "Confirm", class: "btn-danger", value: true }
            ]
        });
    }
}

// Initialize and attach to window for global access
window.modal = new ModalHelper();
