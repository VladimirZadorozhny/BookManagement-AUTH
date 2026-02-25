/**
 * Service layer for Reporting API interactions.
 */
window.reportsApi = {
    fetchBookingReport(type, params = {}) {
        const queryParams = new URLSearchParams({
            type,
            page: params.page || 0,
            size: params.size || 10,
            ...params.filters
        });

        return api.get(`/api/reports/bookings?${queryParams.toString()}`);
    }
};
