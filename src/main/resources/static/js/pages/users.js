/**
 * Controller for the User Management page.
 */
class UsersPage {
    constructor() {
        this.userListBody = byId('user-list-body');
        this.createModalEl = byId('createUserModal');
        this.createModal = this.createModalEl ? new bootstrap.Modal(this.createModalEl) : null;
        this.searchInput = byId('searchInput');
    }

    init() {
        this.bindEvents();
        this.fetchAndRenderUsers();
    }

    bindEvents() {
        byId('btnShowAll')?.addEventListener('click', () => this.fetchAndRenderUsers());
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
    }

    async fetchAndRenderUsers(url = '/api/users') {
        try {
            const response = await api.get(url);
            if (!response.ok) throw new Error('Failed to fetch users');
            
            let users = await response.json();
            
            // Normalize single search result into array
            if (!Array.isArray(users)) {
                users = [users];
            }

            this.renderUsers(users);
        } catch (error) {
            console.error('Error loading users:', error);
            this.userListBody.innerHTML = '<tr><td colspan="5" class="text-center text-danger py-4">Failed to load users.</td></tr>';
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
            const response = await usersApi.create(payload);
            if (response.ok) {
                await modal.alert("User created successfully!");
                this.createModal.hide();
                this.fetchAndRenderUsers();
            } else {
                await api.showError(response, "Failed to create user.");
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
                    this.fetchAndRenderUsers();
                } else {
                    await api.showError(response, `Operation failed.`);
                }
            } catch (error) {
                modal.error("An error occurred.");
            }
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.usersPage = new UsersPage();
    window.usersPage.init();
});
