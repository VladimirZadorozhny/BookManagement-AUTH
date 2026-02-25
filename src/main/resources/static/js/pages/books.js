/**
 * Controller for the Book Catalog page.
 */
class BooksPage {
    constructor() {
        this.pageTitle = byId('pageTitle');
        this.booksGrid = byId('booksGrid');
        this.filterModalEl = byId('filterModal');
        this.createModalEl = byId('createBookModal');
        
        this.filterModal = this.filterModalEl ? new bootstrap.Modal(this.filterModalEl) : null;
        this.createModal = this.createModalEl ? new bootstrap.Modal(this.createModalEl) : null;
        
        this.currentFilterType = '';
        this.isAdmin = document.querySelector('section').dataset.isAdmin === 'true';
        this.isAuthenticated = document.querySelector('section').dataset.isAuthenticated === 'true';
    }

    init() {
        this.bindEvents();
        this.handleInitialParams();
    }

    bindEvents() {
        // Sidebar & Filter Events (using delegation or direct binding since these are unique)
        byId('btnAll')?.addEventListener('click', () => this.loadBooks('/api/books', 'All Books'));
        byId('btnAvailable')?.addEventListener('click', () => this.loadBooks('/api/books?available=true', 'Available Books'));
        
        byId('linkByYear')?.addEventListener('click', (e) => { e.preventDefault(); this.openFilterModal('Year', 'number'); });
        byId('linkByAuthor')?.addEventListener('click', (e) => { e.preventDefault(); this.openFilterModal('Author Name', 'text'); });
        byId('linkByGenre')?.addEventListener('click', (e) => { e.preventDefault(); this.openFilterModal('Genre Name', 'text'); });
        byId('linkByTitle')?.addEventListener('click', (e) => { e.preventDefault(); this.openFilterModal('Title', 'text'); });

        byId('btnApplyFilter')?.addEventListener('click', () => this.applyFilter());

        // Grouped View
        byId('btnGrouped')?.addEventListener('click', () => this.loadGroupedView());
        byId('btnGenresList')?.addEventListener('click', () => this.loadGenresList());

        // Create Book
        byId('btnShowCreateForm')?.addEventListener('click', () => this.openCreateModal());
        byId('btnSubmitCreateBook')?.addEventListener('click', () => this.submitCreateBook());

        // Rent Buttons (Event Delegation)
        this.booksGrid.addEventListener('click', (e) => {
            const rentBtn = e.target.closest('.rent-btn');
            if (rentBtn) {
                this.handleRent(rentBtn.dataset.bookId);
            }
        });
    }

    async loadBooks(url, title = 'Book Catalog') {
        try {
            const response = await api.get(url);
            const books = await response.json();
            this.pageTitle.textContent = title;
            this.renderBooks(books);
        } catch (error) {
            console.error('Error loading books:', error);
        }
    }

    renderBooks(books) {
        this.booksGrid.innerHTML = '';
        this.booksGrid.className = 'row row-cols-1 row-cols-md-3 g-4';
        
        if (!books || books.length === 0) {
            this.booksGrid.innerHTML = '<div class="col-12 text-center py-5"><h3>No books found matching criteria</h3></div>';
            return;
        }

        const fragment = document.createDocumentFragment();
        books.forEach(book => {
            const col = document.createElement('div');
            col.className = 'col';
            
            const badgeClass = book.available > 0 ? 'bg-success' : 'bg-danger';
            const availabilityText = this.isAdmin ? book.available : (book.available > 0 ? 'Yes' : 'No');
            
            let rentButtons = '';
            if (book.available > 0) {
                if (this.isAuthenticated) {
                    rentButtons = `<button class="btn btn-primary rent-btn" data-book-id="${book.id}">Rent Now</button>`;
                } else {
                    rentButtons = `<a href="/login" class="btn btn-primary">Login to Rent</a>`;
                }
            }

            col.innerHTML = `
                <div class="card h-100 shadow-sm">
                    <div class="card-body">
                        <h5 class="card-title text-black">${book.title}</h5>
                        <p class="card-text">
                            <strong>Year:</strong> ${book.year}<br>
                            <strong>Available:</strong> 
                            <span class="badge ${badgeClass}">${availabilityText}</span>
                        </p>
                    </div>
                    <div class="card-footer bg-transparent border-top-0 pb-3">
                        <div class="d-grid gap-2">
                            <a href="/books/${book.id}" class="btn btn-outline-primary">Details</a>
                            ${rentButtons}
                        </div>
                    </div>
                </div>
            `;
            fragment.appendChild(col);
        });
        this.booksGrid.appendChild(fragment);
    }

    openFilterModal(type, inputType) {
        this.currentFilterType = type;
        byId('filterModalLabel').textContent = `Find Book by ${type}`;
        byId('filterInputLabel').textContent = `${type}:`;
        const input = byId('filterInput');
        input.type = inputType;
        input.value = '';
        this.filterModal.show();
    }

    applyFilter() {
        const val = byId('filterInput').value;
        if (!val) return;
        
        let url = '/api/books?';
        if (this.currentFilterType === 'Year') url += `year=${val}`;
        else if (this.currentFilterType === 'Author Name') url += `authorPartName=${encodeURIComponent(val)}`;
        else if (this.currentFilterType === 'Genre Name') url = `/api/genres/name/${encodeURIComponent(val)}/books`;
        else if (this.currentFilterType === 'Title') url += `title=${encodeURIComponent(val)}`;
        
        this.filterModal.hide();
        this.loadBooks(url, `Filtered by ${this.currentFilterType}: ${val}`);
    }

    async openCreateModal() {
        try {
            const resp = await api.get('/api/genres');
            const genres = await resp.json();
            const select = byId('createGenres');
            select.innerHTML = genres.map(g => `<option value="${g.id}">${g.name}</option>`).join('');
            this.createModal.show();
        } catch (e) {
            modal.error("Failed to load genres list.");
        }
    }

    async submitCreateBook() {
        const form = byId('createBookForm');
        const payload = serializeForm(form);
        
        if (!payload.genreIds || (Array.isArray(payload.genreIds) && payload.genreIds.length === 0)) {
            return modal.error("Please select at least one genre.");
        }

        // Convert types
        payload.year = parseInt(payload.year);
        payload.authorId = parseInt(payload.authorId);
        payload.available = parseInt(payload.available);
        if (!Array.isArray(payload.genreIds)) payload.genreIds = [parseInt(payload.genreIds)];
        else payload.genreIds = payload.genreIds.map(id => parseInt(id));

        await withLoading(byId('btnSubmitCreateBook'), async () => {
            const resp = await booksApi.create(payload);
            if (resp.ok) {
                await modal.alert("Book created successfully!");
                this.createModal.hide();
                form.reset();
                this.loadBooks('/api/books');
            } else {
                await api.showError(resp, "Failed to create book.");
            }
        });
    }

    async handleRent(bookId) {
        try {
            const userResp = await api.get('/api/auth/me');
            if (!userResp.ok) {
                window.location.href = '/login';
                return;
            }
            const user = await userResp.json();
            
            const rentResp = await booksApi.rent(user.id, bookId);

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

    async loadGroupedView() {
        try {
            const response = await api.get('/api/genres/with-books');
            const genres = await response.json();
            this.renderGrouped(genres);
        } catch (error) {
            modal.error("Failed to load grouped books.");
        }
    }

    renderGrouped(genres) {
        this.pageTitle.textContent = 'Books Grouped by Genre';
        this.booksGrid.innerHTML = '';
        this.booksGrid.className = 'row row-cols-1 row-cols-md-3 g-4';
        
        const fragment = document.createDocumentFragment();
        genres.forEach(genre => {
            if (genre.books && genre.books.length > 0) {
                const col = document.createElement('div');
                col.className = 'col';
                const maxDisplay = 6;
                const displayedBooks = genre.books.slice(0, maxDisplay);
                const hasMore = genre.books.length > maxDisplay;
                
                let booksHtml = displayedBooks.map(book => `
                    <li class="text-truncate mb-1" title="${book.title}">
                        <a href="/books/${book.id}" class="text-decoration-none small text-black">• ${book.title}</a>
                    </li>`).join('');
                
                if (hasMore) booksHtml += `<li class="small text-muted ps-2">... and ${genre.books.length - maxDisplay} more</li>`;
                
                col.innerHTML = `
                    <div class="card h-100 shadow-sm">
                        <div class="card-header bg-light border-bottom-0 pt-3">
                            <h5 class="mb-0">
                                <a href="#" class="text-primary text-decoration-none genre-link" 
                                   data-genre-name="${genre.name}">${genre.name}</a>
                            </h5>
                        </div>
                        <div class="card-body"><ul class="list-unstyled mb-0">${booksHtml}</ul></div>
                    </div>`;
                fragment.appendChild(col);
            }
        });
        this.booksGrid.appendChild(fragment);

        // Add listeners to genre links
        this.booksGrid.querySelectorAll('.genre-link').forEach(link => {
            link.onclick = (e) => {
                e.preventDefault();
                const name = e.target.dataset.genreName;
                this.loadBooks(`/api/genres/name/${encodeURIComponent(name)}/books`, `Books for Genre: ${name}`);
            };
        });
    }

    async loadGenresList() {
        try {
            const response = await api.get('/api/genres');
            const genres = await response.json();
            this.renderGenres(genres);
        } catch (error) {
            modal.error("Failed to load genres.");
        }
    }

    renderGenres(genres) {
        this.pageTitle.textContent = 'Available Genres';
        this.booksGrid.innerHTML = '';
        const fragment = document.createDocumentFragment();
        genres.forEach(genre => {
            const col = document.createElement('div');
            col.className = 'col';
            col.innerHTML = `
                <div class="card h-100 shadow-sm border-primary">
                    <div class="card-body text-center d-flex flex-column justify-content-center">
                        <h5 class="card-title mb-3">${genre.name}</h5>
                        <div>
                            <button class="btn btn-sm btn-primary view-genre-btn" data-genre-name="${genre.name}">View Books</button>
                        </div>
                    </div>
                </div>`;
            fragment.appendChild(col);
        });
        this.booksGrid.appendChild(fragment);

        this.booksGrid.querySelectorAll('.view-genre-btn').forEach(btn => {
            btn.onclick = () => {
                const name = btn.dataset.genreName;
                this.loadBooks(`/api/genres/name/${encodeURIComponent(name)}/books`, `Books for Genre: ${name}`);
            };
        });
    }

    handleInitialParams() {
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.has('genreName')) {
            const gName = urlParams.get('genreName');
            this.loadBooks(`/api/genres/name/${encodeURIComponent(gName)}/books`, `Books for Genre: ${gName}`);
        } else if (urlParams.has('authorId')) {
            const aName = urlParams.get('authorName') || 'Author';
            this.loadBooks(`/api/books?authorName=${encodeURIComponent(aName)}`, `Books by ${aName}`);
        } else {
            this.loadBooks('/api/books');
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.booksPage = new BooksPage();
    window.booksPage.init();
});
