/**
 * Utility helpers for common UI and form tasks.
 */

/**
 * Serializes form data into a plain JavaScript object.
 * Handles multiple values for the same key (e.g. multi-select) by creating an array.
 */
window.serializeForm = function(form) {
    const data = {};
    const formData = new FormData(form);

    for (const [key, value] of formData.entries()) {
        if (!value && value !== 0) continue; // Skip empty fields

        if (data[key]) {
            if (!Array.isArray(data[key])) {
                data[key] = [data[key]];
            }
            data[key].push(value);
        } else {
            data[key] = value;
        }
    }

    return data;
};

/**
 * Wraps an async task with button loading state.
 */
window.withLoading = async function(button, task) {
    if (!button) return task();

    const originalContent = button.innerHTML;
    button.disabled = true;
    button.innerHTML = `<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Processing...`;

    try {
        return await task();
    } finally {
        button.disabled = false;
        button.innerHTML = originalContent;
    }
};

/**
 * Simple element selector shorthand.
 */
window.byId = (id) => document.getElementById(id);
