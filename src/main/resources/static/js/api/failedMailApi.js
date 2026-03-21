/**
 * Service layer for Failed Mail Management API interactions.
 */
window.failedMailApi = {
    fetchFailedMails(params = {}) {

    const activeFilters = Object.fromEntries(
        Object.entries(params.filters || {})
            .filter(([_, val]) => val !== '' && val !== null)
        );

        const queryParams = new URLSearchParams({
            page: params.page || 0,
            size: params.size || 10,
            ...activeFilters
        });

        return api.get(`/api/admin/failed-mails?${queryParams.toString()}`);
    },

    retry(id) {
        return api.post(`/api/admin/failed-mails/${id}/retry`);
    },

    delete(id) {
        return api.delete(`/api/admin/failed-mails/${id}`);
    }
};
