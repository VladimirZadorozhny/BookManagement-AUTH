/**
 * Controller for the Book Details page.
 */
class BookDetailsPage {
    constructor() {
        const root = document.querySelector('section');
        this.bookId = root.dataset.bookId;
        
        // State
        this.bookData = null; // Will be populated after fetch

        // Static Elements
        this.loadingState = byId('loadingState');
        this.staticDetails = byId('staticDetails');
        this.breadcrumbTitle = byId('breadcrumbTitle');
        this.displayTitle = byId('displayTitle');
        this.displayAuthor = byId('displayAuthor');
        this.displayYear = byId('displayYear');
        this.displayGenres = byId('displayGenres');
        this.displayStatus = byId('displayStatus');
        this.displayAvailable = byId('displayAvailable');
        this.btnRent = byId('btnRent');
        this.btnLoginToRent = byId('btnLoginToRent');

        // Form Elements
        this.editFormContainer = byId('editFormContainer');
        this.editForm = byId('editBookForm');
        this.btnSave = byId('btnSaveBook');
        this.genreSelect = byId('editGenres');
    }

    async init() {
        this.bindEvents();
        await this.fetchAndDisplayDetails();
    }

    bindEvents() {
        this.btnRent?.addEventListener('click', () => this.handleRent());
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
            const response = await booksApi.getWithDetails(this.bookId);
            if (!response.ok) {
                this.loadingState.innerHTML = '<div class="alert alert-danger">Book not found.</div>';
                return;
            }
            this.bookData = await response.json();
            this.renderDetails();
        } catch (error) {
            this.loadingState.innerHTML = '<div class="alert alert-danger">Network error.</div>';
        }
    }

    renderDetails() {
        const b = this.bookData;
        document.getElementById('tabTitle').textContent = `${b.title} - Details`; // Update tab title
        this.breadcrumbTitle.textContent = b.title;
        this.displayTitle.textContent = b.title;
        this.displayAuthor.textContent = b.authorName;
        this.displayAuthor.href = `/authors/${b.authorId}`;
        this.displayYear.textContent = b.year;
        
        this.displayGenres.innerHTML = b.genres.map((g, index) => {
            const isLast = index === b.genres.length - 1;
            const separator = isLast ? '' : ', ';
            return `<a class="badge bg-light text-primary border border-primary text-decoration-none" href="/books?genreName=${encodeURIComponent(g)}">${g}</a>${separator}`;
        }).join('');

        const isAvailable = b.available > 0;
        this.displayStatus.textContent = isAvailable ? 'Available' : 'Out of Stock';
        this.displayStatus.className = isAvailable ? 'text-success fw-bold' : 'text-danger fw-bold';
        
        if (this.displayAvailable) {
            this.displayAvailable.textContent = ` (${b.available} copies)`;
        }

        if (isAvailable) {
            if (this.btnRent) this.btnRent.classList.remove('d-none');
            if (this.btnLoginToRent) this.btnLoginToRent.classList.remove('d-none');
        }

        this.loadingState.classList.add('d-none');
        this.staticDetails.classList.remove('d-none');
    }

    async handleRent() {
        try {
            const userResp = await api.get('/api/auth/me');
            if (!userResp.ok) {
                window.location.href = '/login';
                return;
            }
            const user = await userResp.json();
            
            const rentResp = await booksApi.rent(user.id, this.bookId);

            if (rentResp.ok) {
                await modal.alert("Book rented successfully!");
                window.location.href = '/me?view=active_bookings';
            } else {
                await api.showError(rentResp, "Check your fines or overdue books.");
                window.location.href = '/me?view=active_bookings';
            }
        } catch (error) {
            modal.error("An error occurred during rental.");
        }
    }

    async showEditForm() {
        await this.loadGenresForEdit();
        // Fill form
        this.editForm.elements['title'].value = this.bookData.title;
        this.editForm.elements['year'].value = this.bookData.year;
        this.editForm.elements['authorId'].value = this.bookData.authorId;

        this.staticDetails.classList.add('d-none');
        this.editFormContainer.classList.remove('d-none');
    }

    hideEditForm() {
        this.editForm.reset();
        this.editFormContainer.classList.add('d-none');
        this.staticDetails.classList.remove('d-none');
    }

    async loadGenresForEdit() {
        try {
            const resp = await api.get('/api/genres');
            const genres = await resp.json();
            this.genreSelect.innerHTML = genres.map(g => {
                const selected = this.bookData.genres.includes(g.name) ? 'selected' : '';
                return `<option value="${g.id}" ${selected}>${g.name}</option>`;
            }).join('');
        } catch (e) {
            console.error('Failed to load genres');
        }
    }

    checkFormChanges() {
        const payload = serializeForm(this.editForm);
        const selectedGenreNames = Array.from(this.genreSelect.selectedOptions).map(o => o.text).sort();
        const originalGenres = [...this.bookData.genres].sort();

        const hasChanges = 
            payload.title !== this.bookData.title ||
            parseInt(payload.year) !== parseInt(this.bookData.year) ||
            parseInt(payload.authorId) !== parseInt(this.bookData.authorId) ||
            JSON.stringify(selectedGenreNames) !== JSON.stringify(originalGenres);

        this.btnSave.disabled = !hasChanges;
    }

    async handleUpdate() {
        const payload = serializeForm(this.editForm);
        
        // Validation
        const currentYear = new Date().getFullYear();
        if (parseInt(payload.year) <= 0 || parseInt(payload.year) > currentYear) {
            return modal.error("Invalid year provided.");
        }

        // Convert types
        payload.year = parseInt(payload.year);
        payload.authorId = parseInt(payload.authorId);
        if (!Array.isArray(payload.genreIds)) payload.genreIds = [parseInt(payload.genreIds)];
        else payload.genreIds = payload.genreIds.map(id => parseInt(id));

        await withLoading(this.btnSave, async () => {
            const response = await booksApi.update(this.bookId, payload);
            if (response.ok) {
                await modal.alert("Book updated successfully!");
                window.location.reload();
            } else {
                await api.showError(response, "Update failed.");
            }
        });
    }

    async handleDelete() {
        const confirmed = await modal.confirm("Permanently delete this book?");
        if (!confirmed) return;

        await booksApi.delete(this.bookId)
            .then(async (response) => {
                if (response.ok) {
                    window.location.href = '/books';
                } else {
                    await api.showError(response, "Cannot delete book because it has active bookings or other dependencies.");
                }
            })
            .catch(() => {
                modal.error("A network error occurred. Please try again.");
            });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const root = document.querySelector('section');
    const bookId = root.dataset.bookId; // Ensure bookId is retrieved here

    // Ensure staticDetails is always hidden initially
    byId('staticDetails').classList.add('d-none');

    if (root && bookId && bookId !== '0') { // Check for valid bookId
        window.bookDetailsPage = new BookDetailsPage();
        window.bookDetailsPage.init();
    } else {
        // Handle case where bookId is not provided or is 0, indicating book not found.
        byId('loadingState').innerHTML = '<div class="alert alert-danger">Book not found or invalid ID.</div>';

});
