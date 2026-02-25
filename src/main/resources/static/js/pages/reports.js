/**
 * Controller for the Admin Reports page.
 */
class ReportsPage {
    constructor() {
        this.reportTypeSelect = byId('reportType');
        this.daysInputContainer = byId('daysInputContainer');
        this.minActiveBooksInputContainer = byId('minActiveBooksInputContainer');
        this.btnRunReport = byId('btnRunReport');
        
        this.reportTitle = byId('report-title');
        this.reportContent = byId('report-content');
        this.reportHeader = byId('report-header');
        this.reportBody = byId('report-body');
        this.paginationControls = byId("pagination-controls");
        this.prevPageButton = byId("prev-page");
        this.nextPageButton = byId("next-page");
        this.pageInfoSpan = byId("page-info");
        
        this.currentPage = 0;
        this.totalPages = 0;
    }

    init() {
        this.bindEvents();
        // Set initial visibility
        this.handleReportTypeChange();
    }

    bindEvents() {
        this.reportTypeSelect?.addEventListener('change', () => this.handleReportTypeChange());
        this.btnRunReport?.addEventListener('click', () => {
            this.currentPage = 0;
            this.loadReport();
        });

        this.prevPageButton?.addEventListener("click", () => {
            if (this.currentPage > 0) {
                this.currentPage--;
                this.loadReport();
            }
        });

        this.nextPageButton?.addEventListener("click", () => {
            if (this.currentPage < this.totalPages - 1) {
                this.currentPage++;
                this.loadReport();
            }
        });
    }

    handleReportTypeChange() {
        const selectedType = this.reportTypeSelect.value;
        if (this.daysInputContainer) this.daysInputContainer.style.display = selectedType === 'DUE_SOON' ? 'block' : 'none';
        if (this.minActiveBooksInputContainer) this.minActiveBooksInputContainer.style.display = selectedType === 'HEAVY_USERS' ? 'block' : 'none';
    }

    async loadReport() {
        const type = this.reportTypeSelect.value;
        const filters = {};

        if (type === 'DUE_SOON') filters.days = byId('days').value;
        if (type === 'HEAVY_USERS') filters.minActiveBooks = byId('minActiveBooks').value;
        
        this.reportTitle.textContent = `${this.reportTypeSelect.options[this.reportTypeSelect.selectedIndex].text} Report`;
        this.reportContent.classList.add("loading");

        try {
            const response = await reportsApi.fetchBookingReport(type, {
                page: this.currentPage,
                size: 10,
                filters
            });

            if (response.ok) {
                const pageData = await response.json();
                this.renderReport(pageData);
            } else {
                await api.showError(response, "Failed to load report.");
            }
        } catch (error) {
            console.error('Error fetching report:', error);
        } finally {
            this.reportContent.classList.remove("loading");
        }
    }

    renderReport(pageData) {
        const content = pageData.content;
        const headers = ["User", "Email", "Book", "Borrowed", "Returned", "Overdue", "Fine", "Fine Paid", "Status"];
        this.reportHeader.innerHTML = headers.map(h => `<th>${h}</th>`).join('');
        
        if (!content || content.length === 0) {
            this.reportBody.innerHTML = `<tr><td colspan="${headers.length}" class="text-center py-5 text-muted">No records found for this report.</td></tr>`;
            this.paginationControls.style.display = 'none';
            return;
        }

        const fragment = document.createDocumentFragment();
        content.forEach(b => {
            const tr = document.createElement('tr');
            const { userName, userEmail, bookTitle, borrowedAt, returnedAt, overdueDays, fine, finePaid } = b;

            const isReturned = !!returnedAt;
            const overdueDisplay = overdueDays > 0 ? `${overdueDays} days` : "-";
            const overdueClass = overdueDays > 0 ? "status-overdue" : "";

            const fineDisplay = fine && fine > 0 ? `$${fine.toFixed(2)}` : "-";
            const fineClass = fine > 0 ? "status-overdue" : "";

            const finePaidText = finePaid ? "Yes" : (fine > 0 ? "No" : "-");
            const finePaidClass = (finePaidText === "No") ? "status-overdue" : "";

            let statusText = isReturned ? "Returned" : "Active";
            let statusClass = isReturned ? "bg-success" : "bg-info text-dark";

            if (overdueDays > 0) {
                statusText += ", Overdue";
                if (!finePaid && fine > 0) statusClass = "bg-danger";
            }
            
            if (fine > 0) {
                statusText += finePaid ? ", Fine Paid" : ", Unpaid Fine";
            }

            tr.innerHTML = `
                <td>${userName || 'N/A'}</td>
                <td>${userEmail || 'N/A'}</td>
                <td>${bookTitle || 'N/A'}</td>
                <td>${new Date(borrowedAt).toLocaleDateString()}</td>
                <td>${returnedAt ? new Date(returnedAt).toLocaleDateString() : "-"}</td>
                <td class="${overdueClass}">${overdueDisplay}</td>
                <td class="${fineClass}">${fineDisplay}</td>
                <td class="${finePaidClass}">${finePaidText}</td>
                <td><span class="badge ${statusClass}">${statusText}</span></td>
            `;
            fragment.appendChild(tr);
        });
        
        this.reportBody.innerHTML = '';
        this.reportBody.appendChild(fragment);
        this.updatePagination(pageData);
    }

    updatePagination(pageData) {
        this.totalPages = pageData.totalPages;
        this.currentPage = pageData.number;
        this.pageInfoSpan.innerText = `Page ${this.currentPage + 1} of ${this.totalPages}`;
        this.prevPageButton.disabled = (this.currentPage === 0);
        this.nextPageButton.disabled = (this.currentPage >= this.totalPages - 1);
        this.paginationControls.style.display = 'flex';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.reportsPage = new ReportsPage();
    window.reportsPage.init();
});
