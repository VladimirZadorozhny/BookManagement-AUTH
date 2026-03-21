/**
 * Controller for the Admin Failed Mails page.
 */
class FailedMailsPage {
    constructor() {
        this.mailListBody = byId('mail-list-body');
        this.totalCountSpan = byId('total-count');
        
        this.paginationControls = byId("pagination-controls");
        this.prevPageButton = byId("prev-page");
        this.nextPageButton = byId("next-page");
        this.pageInfoSpan = byId("page-info");

        this.filterEmailInput = byId('filterEmail');
        this.startDateInput = byId('startDate');
        this.endDateInput = byId('endDate');
        this.btnApplyFilter = byId('btnApplyMailFilter');

        this.currentPage = 0;
        this.totalPages = 0;
        this.pageSize = 10;
        this.filters = {
            toEmail: '',
            start: '',
            end: ''
        };
    }

    init() {
        this.bindEvents();
        this.loadMails();
    }

    bindEvents() {
        this.btnApplyFilter?.addEventListener('click', () => {
            this.filters.toEmail = this.filterEmailInput.value.trim();
            this.filters.start = this.startDateInput.value ? new Date(this.startDateInput.value).toISOString() : '';
            this.filters.end = this.endDateInput.value ? new Date(this.endDateInput.value).toISOString() : '';
            this.currentPage = 0;
            this.loadMails();
        });

        // Pagination Events
        this.prevPageButton?.addEventListener("click", () => {
            if (this.currentPage > 0) {
                this.currentPage--;
                this.loadMails();
            }
        });

        this.nextPageButton?.addEventListener("click", () => {
            if (this.currentPage < this.totalPages - 1) {
                this.currentPage++;
                this.loadMails();
            }
        });

        // Table Actions (Retry/Delete)
        this.mailListBody.addEventListener('click', (e) => {
            const retryBtn = e.target.closest('.btn-retry');
            const deleteBtn = e.target.closest('.btn-delete');

            if (retryBtn) {
                this.handleRetry(retryBtn.dataset.id);
            } else if (deleteBtn) {
                this.handleDelete(deleteBtn.dataset.id);
            }
        });
    }

    async loadMails() {
        try {
            const response = await failedMailApi.fetchFailedMails({
                page: this.currentPage,
                size: this.pageSize,
                filters: this.filters
            });

            if (response.ok) {
                const pageData = await response.json();
                this.renderMails(pageData.content || []);
                this.updatePagination(pageData);
                this.totalCountSpan.textContent = `${pageData.totalElements} records`;
            } else {
                await api.showError(response, 'Failed to load failed mails');
            }
        } catch (error) {
            this.mailListBody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-4">Network error while loading data.</td></tr>';
        }
    }

    renderMails(mails) {
        this.mailListBody.innerHTML = '';

        if (!mails || mails.length === 0) {
            this.mailListBody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-5">No failed mail records found.</td></tr>';
            return;
        }

        const fragment = document.createDocumentFragment();
        mails.forEach(mail => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${mail.id}</td>
                <td class="small"><code>${mail.toEmail}</code></td>
                <td>${mail.subject}</td>
                <td class="text-center"><span class="badge bg-warning text-dark">${mail.attemptCount}</span></td>
                <td class="small">${mail.createdAt ? new Date(mail.createdAt).toLocaleString("nl-BE") : 'N/A'}</td>
                <td class="small">${mail.lastAttemptAt ? new Date(mail.lastAttemptAt).toLocaleString("nl-BE") : 'N/A'}</td>
                <td>
                    <div class="text-truncate text-danger small" style="max-width: 200px;" title="${mail.errorMessage || 'Unknown error'}">
                        ${mail.errorMessage || 'N/A'}
                    </div>
                </td>
                <td>
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-success btn-retry" data-id="${mail.id}" title="Retry Sending">
                            Retry
                        </button>
                        <button class="btn btn-outline-danger btn-delete" data-id="${mail.id}" title="Delete Log">
                            Delete
                        </button>
                    </div>
                </td>
            `;
            fragment.appendChild(tr);
        });
        this.mailListBody.appendChild(fragment);
    }

    updatePagination(pageData) {
        this.totalPages = pageData.totalPages;
        this.currentPage = pageData.number;
        this.pageInfoSpan.innerText = `Page ${this.currentPage + 1} of ${this.totalPages}`;
        this.prevPageButton.disabled = (this.currentPage === 0);
        this.nextPageButton.disabled = (this.currentPage >= this.totalPages - 1);
        this.paginationControls.style.display = (this.totalPages > 1) ? 'flex' : 'none';
    }

    async handleRetry(id) {
        const btn = this.mailListBody.querySelector(`.btn-retry[data-id="${id}"]`);
        await withLoading(btn, async () => {
            try {
                const response = await failedMailApi.retry(id);
                if (response.ok) {
                    await modal.alert("Email sent successfully! The record has been removed.");
                    this.loadMails();
                } else {
                    await api.showError(response, "Retry failed again. Check error message in log.");
                    this.loadMails();
                }
            } catch (error) {
                // Handled by api.js
            }
        });
    }

    async handleDelete(id) {
        const confirmed = await modal.confirm("Are you sure you want to delete this log entry?");
        if (confirmed) {
            try {
                const response = await failedMailApi.delete(id);
                if (response.ok) {
                    this.loadMails();
                } else {
                    await api.showError(response, "Failed to delete log entry.");
                }
            } catch (error) {
                // Handled by api.js
            }
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.failedMailsPage = new FailedMailsPage();
    window.failedMailsPage.init();
});
