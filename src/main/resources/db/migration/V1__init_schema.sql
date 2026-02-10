-- Flyway migration: Initial Consolidated Schema & Data

-- ============================================================
-- SCHEMA
-- ============================================================

CREATE TABLE IF NOT EXISTS authors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    birthdate DATE
);

CREATE TABLE IF NOT EXISTS genres (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS books (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    year INT NOT NULL,
    author_id BIGINT NOT NULL,
    available INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_books_author FOREIGN KEY (author_id) REFERENCES authors(id)
);

CREATE TABLE IF NOT EXISTS book_genres (
    book_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    PRIMARY KEY (book_id, genre_id),
    CONSTRAINT fk_book_genres_book FOREIGN KEY (book_id) REFERENCES books(id),
    CONSTRAINT fk_book_genres_genre FOREIGN KEY (genre_id) REFERENCES genres(id)
);

CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    borrowed_at DATE NOT NULL,
    due_at DATE NOT NULL,
    returned_at DATE,
    fine DECIMAL(10,2) DEFAULT 0,
    fine_paid BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_bookings_book FOREIGN KEY (book_id) REFERENCES books(id)
);

-- ============================================================
-- DATA SEEDING
-- ============================================================

-- AUTHORS
INSERT INTO authors (name, birthdate) VALUES
('Isaac Asimov', '1920-01-02'),
('J.K. Rowling', '1965-07-31'),
('George R.R. Martin', '1948-09-20'),
('Arthur C. Clarke', '1917-12-16'),
('Agatha Christie', '1890-09-15');

-- GENRES
INSERT INTO genres (name) VALUES
('Science Fiction'),
('Fantasy'),
('Classic'),
('Mystery'),
('Young Adult');

-- USERS
INSERT INTO users (name, email) VALUES
('Alice Johnson', 'alice@example.com'),
('Bob Smith', 'bob@example.com'),
('Charlie Brown', 'charlie@example.com'),
('Diana Prince', 'diana@example.com'),
('Ethan Clark', 'ethan@example.com'),
('Fiona Adams', 'fiona@example.com'),
('George Miller', 'george@example.com'),
('Hannah White', 'hannah@example.com'),
('Ian Black', 'ian@example.com'),
('Julia Davis', 'julia@example.com');

-- BOOKS
INSERT INTO books (title, year, author_id, available) VALUES
('Foundation', 1951, (SELECT id FROM authors WHERE name='Isaac Asimov'), 3),
('Harry Potter and the Sorcerer''s Stone', 1997, (SELECT id FROM authors WHERE name='J.K. Rowling'), 5),
('A Game of Thrones', 1996, (SELECT id FROM authors WHERE name='George R.R. Martin'), 0),
('Childhood''s End', 1953, (SELECT id FROM authors WHERE name='Arthur C. Clarke'), 2),
('Murder on the Orient Express', 1934, (SELECT id FROM authors WHERE name='Agatha Christie'), 4),
('I, Robot', 1950, (SELECT id FROM authors WHERE name='Isaac Asimov'), 1),
('Harry Potter and the Chamber of Secrets', 1998, (SELECT id FROM authors WHERE name='J.K. Rowling'), 2),
('A Clash of Kings', 1998, (SELECT id FROM authors WHERE name='George R.R. Martin'), 0),
('Rendezvous with Rama', 1973, (SELECT id FROM authors WHERE name='Arthur C. Clarke'), 3),
('And Then There Were None', 1939, (SELECT id FROM authors WHERE name='Agatha Christie'), 5);

-- BOOK-GENRE MAPPING
INSERT INTO book_genres (book_id, genre_id)
SELECT b.id, g.id
FROM books b
JOIN genres g ON
    (b.title = 'Foundation' AND g.name IN ('Science Fiction', 'Classic')) OR
    (b.title LIKE 'Harry Potter%' AND g.name IN ('Fantasy', 'Young Adult')) OR
    (b.title LIKE 'A Game of Thrones%' AND g.name = 'Fantasy') OR
    (b.title LIKE 'A Clash of Kings%' AND g.name = 'Fantasy') OR
    (b.title = 'Childhood''s End' AND g.name = 'Science Fiction') OR
    (b.title = 'Rendezvous with Rama' AND g.name = 'Science Fiction') OR
    (b.title LIKE 'Murder on%' AND g.name = 'Mystery') OR
    (b.title = 'And Then There Were None' AND g.name = 'Mystery');

-- BOOKINGS (DEMO DATA)
INSERT INTO bookings (user_id, book_id, borrowed_at, due_at, returned_at, fine, fine_paid)
VALUES
    ((SELECT id FROM users WHERE email='alice@example.com'), (SELECT id FROM books WHERE title='Foundation'), CURRENT_DATE - INTERVAL 2 DAY, CURRENT_DATE + INTERVAL 12 DAY, NULL, 0, FALSE),
    ((SELECT id FROM users WHERE email='bob@example.com'), (SELECT id FROM books WHERE title='Harry Potter and the Sorcerer''s Stone'), CURRENT_DATE - INTERVAL 5 DAY, CURRENT_DATE + INTERVAL 9 DAY, NULL, 0, FALSE),
    ((SELECT id FROM users WHERE email='ian@example.com'), (SELECT id FROM books WHERE title='Foundation'), CURRENT_DATE - INTERVAL 12 DAY, CURRENT_DATE + INTERVAL 2 DAY, NULL, 0, FALSE),
    ((SELECT id FROM users WHERE email='charlie@example.com'), (SELECT id FROM books WHERE title='A Game of Thrones'), CURRENT_DATE - INTERVAL 30 DAY, CURRENT_DATE - INTERVAL 16 DAY, NULL, 0, FALSE),
    ((SELECT id FROM users WHERE email='diana@example.com'), (SELECT id FROM books WHERE title='Murder on the Orient Express'), CURRENT_DATE - INTERVAL 20 DAY, CURRENT_DATE - INTERVAL 6 DAY, NULL, 6, FALSE),
    ((SELECT id FROM users WHERE email='ethan@example.com'), (SELECT id FROM books WHERE title='I, Robot'), CURRENT_DATE - INTERVAL 18 DAY, CURRENT_DATE - INTERVAL 4 DAY, CURRENT_DATE - INTERVAL 1 DAY, 3, TRUE),
    ((SELECT id FROM users WHERE email='fiona@example.com'), (SELECT id FROM books WHERE title='Rendezvous with Rama'), CURRENT_DATE - INTERVAL 22 DAY, CURRENT_DATE - INTERVAL 8 DAY, CURRENT_DATE - INTERVAL 3 DAY, 5, TRUE),
    ((SELECT id FROM users WHERE email='ethan@example.com'), (SELECT id FROM books WHERE title='A Game of Thrones'), CURRENT_DATE - INTERVAL 20 DAY, CURRENT_DATE - INTERVAL 6 DAY, CURRENT_DATE - INTERVAL 1 DAY, 5, FALSE),
    ((SELECT id FROM users WHERE email='george@example.com'), (SELECT id FROM books WHERE title='And Then There Were None'), CURRENT_DATE - INTERVAL 10 DAY, CURRENT_DATE - INTERVAL 1 DAY, CURRENT_DATE - INTERVAL 1 DAY, 0, FALSE),
    ((SELECT id FROM users WHERE email='hannah@example.com'), (SELECT id FROM books WHERE title='Childhood''s End'), CURRENT_DATE - INTERVAL 7 DAY, CURRENT_DATE + INTERVAL 7 DAY, CURRENT_DATE - INTERVAL 2 DAY, 0, FALSE),
    ((SELECT id FROM users WHERE email='ian@example.com'), (SELECT id FROM books WHERE title='And Then There Were None'), CURRENT_DATE - INTERVAL 18 DAY, CURRENT_DATE - INTERVAL 4 DAY, NULL, 3, FALSE);