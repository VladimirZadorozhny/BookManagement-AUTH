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
        this.promptContainer = document.getElementById('modal-prompt-container');
        this.promptInput = document.getElementById('modal-prompt-input');

        if (this.modalEl) {
            this.instance = new bootstrap.Modal(this.modalEl);
        } else {
            console.warn("Modal overlay element not found. ModalHelper may not function correctly.");
        }
    }

    /**
     * Internal method to open the modal with specific configuration.
     */
    open({ title, message, buttons, isPrompt = false, defaultValue = "" }) {
        if (!this.instance) return Promise.resolve(null);

        return new Promise(resolve => {
            this.titleEl.textContent = title;
            this.messageEl.textContent = message;
            this.buttonsEl.innerHTML = '';

            // Accessibility: remove inert when showing
            this.modalEl.removeAttribute('inert');

            if (isPrompt) {
                this.promptContainer.classList.remove('d-none');
                this.promptInput.value = defaultValue;
                // Focus input after modal is shown
                this.modalEl.addEventListener('shown.bs.modal', () => {
                    this.promptInput.focus();
                }, { once: true });
            } else {
                this.promptContainer.classList.add('d-none');
            }

            buttons.forEach(btnInfo => {
                const btn = document.createElement('button');
                btn.textContent = btnInfo.text;
                btn.className = `btn ${btnInfo.class || 'btn-primary'}`;

                btn.onclick = () => {
                    let result = btnInfo.value;
                    if (isPrompt && result === true) {
                        result = this.promptInput.value;
                    }

                    btn.blur();

                    // Resolve ONLY after the modal is fully hidden to prevent animation conflicts
                    this.modalEl.addEventListener('hidden.bs.modal', () => {
                        // Accessibility: add inert when hidden
                        this.modalEl.setAttribute('inert', '');
                        resolve(result);
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

    /**
     * Prompt modal for text input.
     * Returns the input value if confirmed, or null if cancelled.
     */
    prompt(message, defaultValue = "", title = "Input Required") {
        return this.open({
            title,
            message,
            isPrompt: true,
            defaultValue,
            buttons: [
                { text: "Cancel", class: "btn-secondary", value: null },
                { text: "OK", class: "btn-primary", value: true }
            ]
        });
    }
}

// Initialize and attach to window for global access
window.modal = new ModalHelper();
