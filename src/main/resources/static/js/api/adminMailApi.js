/**
 * Service layer for Admin Mail related API interactions.
 */
window.adminMailApi = {
    notifyHeavyUsers(subject, body, minBooks) {
        return api.post("/api/reports/notify-heavy-users", {
            subject: subject,
            body: body,
            minBooksBorrowed: minBooks
        });
    },

    notifyOverdueUsers(subject, body) {
        return api.post("/api/reports/notify-overdue-users", {
            subject: subject,
            body: body
        });
    },

    notifySingleUser(userId, subject, body) {
        return api.post(`/api/reports/notify-user/${userId}`, {
            subject: subject,
            body: body
        });
    }
};
