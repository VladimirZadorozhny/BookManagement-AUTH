/**
 * Controller for the User Details / Profile page.
 */
class UserDetailsPage {
    constructor() {
        const root = document.querySelector('section');
        this.userId = root.dataset.userId;
        this.isAdmin = root.dataset.isAdmin === 'true';
        
        // State
        this.userData = null;

        // UI Elements
        this.loadingState = byId('loadingState');
        this.personalInfoContent = byId('personalInfoContent');
        this.bookingsContent = byId('bookingsContent');
        this.dynamicFormContent = byId('dynamicFormContent');
        this.bookingsTableBody = byId('bookings-table-body');
        this.bookingsTableHeader = byId('bookings-table-header');
        this.bookingsListTitle = byId('bookingsListTitle');
        this.sidebarList = byId('user-sidebar-list');

        // Display Fields
        this.displayName = byId('displayName');
        this.displayEmail = byId('displayEmail');
        this.displayActiveStatus = byId('displayActiveStatus');
    }

    async init() {
        this.bindEvents();
        await this.fetchAndDisplayUserDetails();
    }

    bindEvents() {
        if (this.sidebarList) {
            this.sidebarList.addEventListener('click', (e) => {
                const btn = e.target.closest('.list-group-item-action');
                if (btn) {
                    this.activateSidebarButton(btn);
                    this.displayContent(btn.id);
                }
            });
        }

        // Actions (Delegation)
        this.bookingsTableBody.addEventListener('click', (e) => {
            const returnBtn = e.target.closest('.return-btn');
            const payBtn = e.target.closest('.pay-fine-btn');

            if (returnBtn) this.handleReturn(returnBtn.dataset.bookId);
            if (payBtn) this.handlePayFine(payBtn.dataset.bookingId);
        });
    }

    async fetchAndDisplayUserDetails() {
        try {
            const response = await usersApi.get(this.userId);
            if (!response.ok) {
                this.loadingState.innerHTML = '<div class="alert alert-danger">User not found.</div>';
                return;
            }
            this.userData = await response.json();
            this.renderUserHeader();
            this.handleInitialParams();
        } catch (error) {
            this.loadingState.innerHTML = '<div class="alert alert-danger">Network error.</div>';
        }
    }

    renderUserHeader() {
        this.displayName.textContent = this.userData.name;
        this.displayEmail.textContent = this.userData.email;
        
        const active = this.userData.active;
        this.displayActiveStatus.textContent = active ? 'Active' : 'Inactive';
        this.displayActiveStatus.className = `badge ${active ? 'bg-success' : 'bg-secondary'}`;

        // Update Toggle Button Text if Admin
        const toggleBtn = byId('btnToggleActiveStatus');
        if (toggleBtn) {
            toggleBtn.textContent = active ? 'Deactivate User' : 'Activate User';
        }

        this.loadingState.classList.add('d-none');
        this.personalInfoContent.classList.remove('d-none');
    }

    activateSidebarButton(button) {
        this.sidebarList.querySelectorAll('.list-group-item-action').forEach(b => b.classList.remove('active'));
        button.classList.add('active');
    }

    async displayContent(contentType) {
        // Hide all content containers using class 'd-none'
        this.personalInfoContent.classList.add('d-none');
        this.bookingsContent.classList.add('d-none');
        this.dynamicFormContent.classList.add('d-none');

        switch (contentType) {
            case 'btnPersonalInfo':
                this.personalInfoContent.classList.remove('d-none');
                break;
            case 'btnAllBookings':
                this.bookingsListTitle.textContent = 'All Bookings';
                await this.fetchAndRenderBookings();
                this.bookingsContent.classList.remove('d-none');
                break;
            case 'btnActiveBookings':
                this.bookingsListTitle.textContent = 'Borrowed books';
                await this.fetchAndRenderBookings(true);
                this.bookingsContent.classList.remove('d-none');
                break;
            case 'btnToggleActiveStatus':
                this.personalInfoContent.classList.remove('d-none');
                await this.handleToggleActiveStatus();
                break;
        }
    }

    async fetchAndRenderBookings(filterBorrowed = false) {
        try {
            const response = await usersApi.listBookings(this.userId);
            if (!response.ok) throw new Error('Failed to fetch bookings');
            
            let bookings = await response.json();
            
            if (filterBorrowed) {
                // Logic: Show only active bookings OR bookings with unpaid fines.
                bookings = bookings.filter(b => !b.returnedAt || (b.fine > 0 && !b.finePaid));
            }

            this.renderBookingsTable(bookings);
        } catch (e) {
            this.bookingsTableBody.innerHTML = `<tr><td colspan="6" class="text-center text-danger py-4">Failed to load bookings.</td></tr>`;
        }
    }

    renderBookingsTable(bookings) {
        this.bookingsTableBody.innerHTML = '';
        const headers = ["Book", "Borrowed", "Due Date", "Returned", "Fine", "Actions"];
        this.bookingsTableHeader.innerHTML = headers.map(h => `<th>${h}</th>`).join('');

        if (!bookings || bookings.length === 0) {
            this.bookingsTableBody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">No records found.</td></tr>`;
            return;
        }

        const fragment = document.createDocumentFragment();
        bookings.forEach(booking => {
            const tr = document.createElement('tr');
            
            const isReturned = !!booking.returnedAt;
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            const dueDate = new Date(booking.dueAt);
            dueDate.setHours(0, 0, 0, 0);
            const isOverdue = !isReturned && (dueDate < today);
            const isNearDue = !isReturned && ((dueDate - today) / (1000 * 60 * 60 * 24) <= 3);

            let dueClass = "";
            if (isOverdue) dueClass = "status-overdue";
            else if (isNearDue) dueClass = "status-near-due";

            const fineDisplay = booking.fine ? `<span class="text-danger fw-bold">$${booking.fine.toFixed(2)}</span>` : '-';
            const returnedDateDisplay = booking.returnedAt ? new Date(booking.returnedAt).toLocaleDateString() : '-';
            
            let actionsHtml = '';
            if (!isReturned) {
                actionsHtml = `<button class="btn btn-sm btn-warning return-btn" data-book-id="${booking.bookId}">Return</button>`;
            } else if (booking.fine > 0 && !booking.finePaid) {
                actionsHtml = `<button class="btn btn-sm btn-danger pay-fine-btn" data-booking-id="${booking.id}">Pay Fine</button>`;
            }

            tr.innerHTML = `
                <td><a href="/books/${booking.bookId}">${booking.bookTitle}</a></td>
                <td>${new Date(booking.borrowedAt).toLocaleDateString()}</td>
                <td class="${dueClass}">${new Date(booking.dueAt).toLocaleDateString()}</td>
                <td>${returnedDateDisplay}</td>
                <td>${fineDisplay}</td>
                <td>${actionsHtml}</td>
            `;
            fragment.appendChild(tr);
        });
        this.bookingsTableBody.appendChild(fragment);
    }

    async handleReturn(bookId) {
        const confirmed = await modal.confirm("Are you sure you want to return this book?");
        if (!confirmed) return;

        try {
            const response = await booksApi.returnBook(this.userId, bookId);
            if (response.ok) {
                await modal.alert("Book returned successfully!");
                this.refreshCurrentView();
            } else {
                await api.showError(response, "Return failed.");
            }
        } catch (error) {
            modal.error("An error occurred during return.");
        }
    }

    async handlePayFine(bookingId) {
        const confirmed = await modal.confirm("Are you sure you want to pay this fine?");
        if (!confirmed) return;

        try {
            const response = await usersApi.payFine(this.userId, bookingId);
            if (response.ok) {
                await modal.alert("Fine paid successfully!");
                this.refreshCurrentView();
            } else {
                await api.showError(response, "Payment failed.");
            }
        } catch (error) {
            modal.error("An error occurred during payment.");
        }
    }

    async handleToggleActiveStatus() {
        const action = this.userData.active ? 'deactivate' : 'activate';
        const confirmed = await modal.confirm(`Are you sure you want to ${action} this user?`);
        
        if (!confirmed) {
            // Restore previous active button state if confirmation canceled
            const currentActiveButton = this.sidebarList.querySelector('.active');
            if (currentActiveButton) this.activateSidebarButton(currentActiveButton);
            return;
        }

        try {
            const response = (action === 'activate') ? await usersApi.activate(this.userId) : await usersApi.deactivate(this.userId);
            if (response.ok) {
                await modal.alert(`User ${action}d successfully!`);
                window.location.reload();
            } else {
                await api.showError(response, `Operation failed.`);
                this.refreshCurrentView();
            }
        } catch (error) {
            modal.error("An error occurred.");
            this.refreshCurrentView();
        }
    }

    refreshCurrentView() {
        const currentActiveButton = this.sidebarList.querySelector('.active');
        if (currentActiveButton) this.displayContent(currentActiveButton.id);
    }

    handleInitialParams() {
        const urlParams = new URLSearchParams(window.location.search);
        const viewParam = urlParams.get('view');
        
        if (viewParam === 'active_bookings') {
            const btn = byId('btnActiveBookings');
            if (btn) btn.click();
        } else {
            this.displayContent('btnPersonalInfo');
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const root = document.querySelector('section');
    if (root && root.dataset.userId) {
        window.userDetailsPage = new UserDetailsPage();
        window.userDetailsPage.init();
    }
});
