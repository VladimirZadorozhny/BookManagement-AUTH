/**
 * Service layer for Book-related API interactions.
 */
window.booksApi = {
    list() {
        return api.get("/api/books");
    },

    get(id) {
        return api.get(`/api/books/${id}`);
    },

    create(data) {
        return api.post("/api/books", data);
    },

    update(id, data) {
        return api.put(`/api/books/${id}`, data);
    },

    delete(id) {
        return api.delete(`/api/books/${id}`);
    },

    rent(userId, bookId) {
        return api.post(`/api/users/${userId}/rent`, { bookId });
    },

    returnBook(userId, bookId) {
        return api.post(`/api/users/${userId}/return`, { bookId });
    },

    getWithDetails(id) {
        return api.get(`/api/books/${id}/details`);
    }
};
