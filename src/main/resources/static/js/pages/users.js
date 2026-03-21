/**
 * Controller for the User Management page.
 */
class UsersPage {
    constructor() {
        this.userListBody = byId('user-list-body');
        this.createModalEl = byId('createUserModal');
        this.createModal = this.createModalEl ? new bootstrap.Modal(this.createModalEl) : null;
        this.searchInput = byId('searchInput');

        this.paginationControls = byId("pagination-controls");
        this.prevPageButton = byId("prev-page");
        this.nextPageButton = byId("next-page");
        this.pageInfoSpan = byId("page-info");

        this.currentPage = 0;
        this.totalPages = 0;
        this.pageSize = 10;
        this.currentUrl = '/api/users';
    }

    init() {
        this.bindEvents();
        this.fetchAndRenderUsers();
    }

    bindEvents() {
        byId('btnShowAll')?.addEventListener('click', () => {
            this.currentPage = 0;
            this.fetchAndRenderUsers('/api/users');
        });
        byId('btnSearch')?.addEventListener('click', () => this.handleSearch());
        byId('btnShowCreate')?.addEventListener('click', () => {
            byId('createUserForm').reset();
            this.createModal.show();
        });
        byId('btnSubmitCreate')?.addEventListener('click', () => this.submitCreateUser());

        // Status Toggle (Delegation)
        this.userListBody.addEventListener('click', (e) => {
            const toggleBtn = e.target.closest('.btn-toggle-active');
            if (toggleBtn) {
                this.handleStatusToggle(toggleBtn.dataset.userId, toggleBtn.dataset.active === 'true');
            }
        });

        // Pagination Events
        this.prevPageButton?.addEventListener("click", () => {
            if (this.currentPage > 0) {
                this.currentPage--;
                this.fetchAndRenderUsers(this.currentUrl, false);
            }
        });

        this.nextPageButton?.addEventListener("click", () => {
            if (this.currentPage < this.totalPages - 1) {
                this.currentPage++;
                this.fetchAndRenderUsers(this.currentUrl, false);
            }
        });
    }

    async fetchAndRenderUsers(url = '/api/users', resetPage = true) {
        if (resetPage) this.currentPage = 0;
        this.currentUrl = url;

        const separator = url.includes('?') ? '&' : '?';
        const finalUrl = `${url}${separator}page=${this.currentPage}&size=${this.pageSize}`;

        try {
            const response = await api.get(finalUrl);
            if (!response.ok) {
                await api.showError(response, 'Failed to fetch users');
                return;
            }

            const data = await response.json();

            // Check if it's a Page object or a single User object (from search)
            if (data && data.content !== undefined) {
                this.renderUsers(data.content);
                this.updatePagination(data);
            } else if (data) {
                // Single user from search
                this.renderUsers([data]);
                this.paginationControls.style.display = 'none';
            } else {
                this.renderUsers([]);
                this.paginationControls.style.display = 'none';
            }
        } catch (error) {
            // Network errors handled by api.js
            this.userListBody.innerHTML = '<tr><td colspan="5" class="text-center text-danger py-4">Failed to load users.</td></tr>';
            this.paginationControls.style.display = 'none';
        }
    }

    renderUsers(users) {
        this.userListBody.innerHTML = '';

        if (!users || users.length === 0) {
            this.userListBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4">No users found.</td></tr>';
            return;
        }

        const fragment = document.createDocumentFragment();
        users.forEach(user => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${user.id}</td>
                <td>${user.name}</td>
                <td>${user.email}</td>
                <td><span class="badge ${user.active ? 'bg-success' : 'bg-secondary'}">${user.active ? 'Active' : 'Inactive'}</span></td>
                <td>
                    <a href="/users/${user.id}" class="btn btn-sm btn-outline-primary">Details</a>
                    <button class="btn btn-sm ${user.active ? 'btn-outline-danger' : 'btn-outline-success'} btn-toggle-active"
                            data-user-id="${user.id}" data-active="${user.active}">
                        ${user.active ? 'Deactivate' : 'Activate'}
                    </button>
                </td>
            `;
            fragment.appendChild(tr);
        });
        this.userListBody.appendChild(fragment);
    }

    updatePagination(pageData) {
        this.totalPages = pageData.totalPages;
        this.currentPage = pageData.number;
        this.pageInfoSpan.innerText = `Page ${this.currentPage + 1} of ${this.totalPages}`;
        this.prevPageButton.disabled = (this.currentPage === 0);
        this.nextPageButton.disabled = (this.currentPage >= this.totalPages - 1);
        this.paginationControls.style.display = (this.totalPages > 1) ? 'flex' : 'none';
    }

    handleSearch() {
        const query = this.searchInput.value.trim();
        if (query) {
            this.fetchAndRenderUsers(`/api/users/search?by=${encodeURIComponent(query)}`);
        }
    }

    async submitCreateUser() {
        const form = byId('createUserForm');
        const payload = serializeForm(form);

        await withLoading(byId('btnSubmitCreate'), async () => {
            try {
                const response = await usersApi.create(payload);
                if (response.ok) {
                    await modal.alert("User created successfully!");
                    this.createModal.hide();
                    this.fetchAndRenderUsers();
                } else {
                    await api.showError(response, "Failed to create user.");
                }
            } catch (e) {
                // Handled by api.js
            }
        });
    }

    async handleStatusToggle(userId, isActive) {
        const action = isActive ? 'deactivate' : 'activate';
        const confirmed = await modal.confirm(`Are you sure you want to ${action} this user?`);

        if (confirmed) {
            try {
                const response = isActive ? await usersApi.deactivate(userId) : await usersApi.activate(userId);
                if (response.ok) {
                    await modal.alert(`User ${action}d successfully!`);
                    this.fetchAndRenderUsers(this.currentUrl, false);
                } else {
                    await api.showError(response, `Operation failed.`);
                }
            } catch (error) {
                // Handled by api.js
            }
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.usersPage = new UsersPage();
    window.usersPage.init();
});
