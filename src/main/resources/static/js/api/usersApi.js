/**
 * Service layer for User-related API interactions.
 */
window.usersApi = {
    list() {
        return api.get("/api/users");
    },

    get(id) {
        return api.get(`/api/users/${id}`);
    },

    search(query) {
        return api.get(`/api/users/search?by=${encodeURIComponent(query)}`);
    },

    create(data) {
        return api.post("/api/users", data);
    },

    update(id, data) {
        return api.put(`/api/users/${id}`, data);
    },

    delete(id) {
        return api.delete(`/api/users/${id}`);
    },

    activate(id) {
        return api.post(`/api/users/${id}/activate`);
    },

    deactivate(id) {
        return api.post(`/api/users/${id}/deactivate`);
    },

    listBookings(id, status = '') {
        let url = `/api/users/${id}/bookings`;
        if (status) url += `?status=${status}`;
        return api.get(url);
    },

    payFine(userId, bookingId) {
        return api.post(`/api/users/${userId}/bookings/${bookingId}/pay`);
    }
};
