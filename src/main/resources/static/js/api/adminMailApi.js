/**
 * Service layer for Admin Mail related API interactions.
 */
window.adminMailApi = {
    notifyHeavyUsersAuto(minBooks) {
        const query = minBooks ? `?minActiveBooks=${encodeURIComponent(minBooks)}` : "";
        return api.post(`/api/reports/notify-heavy-users-auto${query}`);
    },

    notifyOverdueUsersAuto() {
        return api.post("/api/reports/notify-overdue-users-auto");
    },

    notifyUnpaidFinesUsersAuto() {
        return api.post("/api/reports/notify-unpaid-fines-users-auto");
    },

    notifySingleUser(userId, subject, body) {
        return api.post(`/api/reports/notify-user/${userId}`, {
            subject: subject,
            body: body
        });
    }
};
