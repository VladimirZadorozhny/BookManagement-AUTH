/**
 * Controller for the Authors list page.
 */
class AuthorsPage {
    constructor() {
        this.pageTitle = byId('pageTitle');
        this.authorsGrid = byId('authorsGrid');
        this.createModalEl = byId('createAuthorModal');
        this.createModal = this.createModalEl ? new bootstrap.Modal(this.createModalEl) : null;
        
        this.isAdmin = document.querySelector('section').dataset.isAdmin === 'true';
    }

    init() {
        this.bindEvents();
        const urlParams = new URLSearchParams(window.location.search);
        if (!urlParams.has('name')) {
            this.loadAuthors('/api/authors');
        }
    }

    bindEvents() {
        byId('btnAllAuthors')?.addEventListener('click', () => this.loadAuthors('/api/authors'));
        byId('btnSearchAuthor')?.addEventListener('click', () => this.handleSearch());
        byId('btnShowCreateAuthor')?.addEventListener('click', () => this.createModal.show());
        byId('btnSubmitCreateAuthor')?.addEventListener('click', () => this.submitCreateAuthor());
    }

    async loadAuthors(url, title = 'Authors') {
        try {
            const response = await api.get(url);
            const authors = await response.json();
            this.pageTitle.textContent = title;
            this.renderAuthors(authors);
        } catch (error) {
            console.error('Error loading authors:', error);
            modal.error('Failed to load authors');
        }
    }

    renderAuthors(authors) {
        this.authorsGrid.innerHTML = '';
        if (!authors || authors.length === 0) {
            this.authorsGrid.innerHTML = '<div class="col-12 text-center py-5"><h3>No authors found</h3></div>';
            return;
        }

        const fragment = document.createDocumentFragment();
        authors.forEach(author => {
            const col = document.createElement('div');
            col.className = 'col';
            
            col.innerHTML = `
                <div class="col">
                    <div class="card h-100 shadow-sm border-primary">
                        <div class="card-body text-center">
                            <h5 class="card-title text-black">${author.name}</h5>
                        </div>
                        <div class="card-footer bg-transparent border-top-0 pb-3">
                            <div class="d-grid"><a href="/authors/${author.id}" class="btn btn-outline-primary">Details & Books</a></div>
                        </div>
                    </div>
                </div>`;
            fragment.appendChild(col);
        });
        this.authorsGrid.appendChild(fragment);
    }

    handleSearch() {
        const name = byId('searchAuthorName').value.trim();
        if (name) {
            this.loadAuthors(`/api/authors?name=${encodeURIComponent(name)}`, `Search results for: ${name}`);
        }
    }

    async submitCreateAuthor() {
        const form = byId('createAuthorForm');
        const payload = serializeForm(form);

        if (!payload.name || !payload.birthdate) {
            return modal.error("Please fill all fields.");
        }

        const bdate = new Date(payload.birthdate);
        if (bdate > new Date()) {
            return modal.error("Birthdate cannot be in the future.");
        }

        await withLoading(byId('btnSubmitCreateAuthor'), async () => {
            const resp = await authorsApi.create(payload);
            if (resp.ok) {
                await modal.alert("Author created successfully!");
                this.createModal.hide();
                form.reset();
                this.loadAuthors('/api/authors');
            } else {
                await api.showError(resp, "Failed to create author.");
            }
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.authorsPage = new AuthorsPage();
    window.authorsPage.init();
});
