/**
 * Service layer for Author-related API interactions.
 */
window.authorsApi = {
    list() {
        return api.get("/api/authors");
    },

    get(id) {
        return api.get(`/api/authors/${id}`);
    },

    create(data) {
        return api.post("/api/authors", data);
    },

    update(id, data) {
        return api.put(`/api/authors/${id}`, data);
    },

    delete(id) {
        return api.delete(`/api/authors/${id}`);
    }
};
