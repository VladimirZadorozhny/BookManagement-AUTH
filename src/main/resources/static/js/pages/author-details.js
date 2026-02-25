/**
 * Controller for the Author Details page.
 */
class AuthorDetailsPage {
    constructor() {
        const root = document.querySelector('section');
        this.authorId = root.dataset.authorId;
        
        // State
        this.authorData = null; // Will be populated after fetch

        // UI Elements
        this.loadingState = byId('loadingState');
        this.staticDetails = byId('staticDetails');
        this.tabTitle = byId('tabTitle');
        this.breadcrumbName = byId('breadcrumbName');
        this.displayName = byId('displayName');
        this.displayBirthdate = byId('displayBirthdate');
        this.linkAllBooks = byId('linkAllBooks');

        // Form Elements
        this.editFormContainer = byId('editFormContainer');
        this.editForm = byId('editAuthorForm');
        this.btnSave = byId('btnSaveAuthor');
    }

    async init() {
        this.bindEvents();
        await this.fetchAndDisplayDetails();
    }

    bindEvents() {
        byId('btnShowEdit')?.addEventListener('click', () => this.showEditForm());
        byId('btnCancelEdit')?.addEventListener('click', () => this.hideEditForm());
        byId('btnDelete')?.addEventListener('click', () => this.handleDelete());

        this.editForm?.addEventListener('input', () => this.checkFormChanges());
        this.editForm?.addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleUpdate();
        });
    }

    async fetchAndDisplayDetails() {
        try {
            const response = await authorsApi.get(this.authorId);
            if (!response.ok) {
                this.loadingState.innerHTML = '<div class="alert alert-danger">Author not found.</div>';
                return;
            }
            this.authorData = await response.json();
            this.renderDetails();
        } catch (error) {
            this.loadingState.innerHTML = '<div class="alert alert-danger">Network error.</div>';
        }
    }

    renderDetails() {
        const a = this.authorData;
        const formattedDate = new Date(a.birthdate).toLocaleDateString('en-US', { month: '2-digit', day: '2-digit', year: 'numeric' });
        
        this.tabTitle.textContent = `${a.name} - Details`;
        this.breadcrumbName.textContent = a.name;
        this.displayName.textContent = a.name;
        this.displayBirthdate.textContent = formattedDate;
        this.linkAllBooks.href = `/books?authorId=${a.id}&authorName=${encodeURIComponent(a.name)}`;

        this.loadingState.classList.add('d-none');
        this.staticDetails.classList.remove('d-none');
    }

    showEditForm() {
        this.editForm.elements['name'].value = this.authorData.name;
        this.editForm.elements['birthdate'].value = this.authorData.birthdate;

        this.staticDetails.classList.add('d-none');
        this.editFormContainer.classList.remove('d-none');
        this.btnSave.disabled = true;
    }

    hideEditForm() {
        this.editForm.reset();
        this.editFormContainer.classList.add('d-none');
        this.staticDetails.classList.remove('d-none');
    }

    checkFormChanges() {
        const payload = serializeForm(this.editForm);
        const hasChanges = 
            payload.name !== this.authorData.name ||
            payload.birthdate !== this.authorData.birthdate;

        this.btnSave.disabled = !hasChanges;
    }

    async handleUpdate() {
        const payload = serializeForm(this.editForm);
        
        if (!payload.name) return modal.error("Name cannot be empty.");
        if (new Date(payload.birthdate) > new Date()) {
            return modal.error("Birthdate cannot be in the future.");
        }

        await withLoading(this.btnSave, async () => {
            const response = await authorsApi.update(this.authorId, payload);
            if (response.ok) {
                await modal.alert("Author updated successfully!");
                window.location.reload();
            } else {
                await api.showError(response, "Update failed.");
            }
        });
    }

    async handleDelete() {
        const confirmed = await modal.confirm("Are you sure you want to delete this author? This will fail if they have books.");
        if (!confirmed) return;

        await authorsApi.delete(this.authorId)
            .then(async (response) => {
                if (response.ok) {
                    window.location.href = '/authors';
                } else {
                    await api.showError(response, "Cannot delete author because they have associated books in the system.");
                }
            })
            .catch(() => {
                modal.error("A network error occurred. Please try again.");
            });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const root = document.querySelector('section');
    const authorId = root.dataset.authorId;

    // Ensure staticDetails is always hidden initially
    byId('staticDetails').classList.add('d-none');

    if (root && authorId && authorId !== '0') {
        window.authorDetailsPage = new AuthorDetailsPage();
        window.authorDetailsPage.init();
    } else {
        byId('loadingState').innerHTML = '<div class="alert alert-danger">Author not found or invalid ID.</div>';
    }
});
