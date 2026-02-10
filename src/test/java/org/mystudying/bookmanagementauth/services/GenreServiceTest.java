package org.mystudying.bookmanagementauth.services;

import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.domain.Author;
import org.mystudying.bookmanagementauth.domain.Book;
import org.mystudying.bookmanagementauth.domain.Genre;
import org.mystudying.bookmanagementauth.dto.BookDto;
import org.mystudying.bookmanagementauth.dto.GenreDto;
import org.mystudying.bookmanagementauth.repositories.BookRepository;
import org.mystudying.bookmanagementauth.repositories.GenreRepository;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Slice test: JPA + one service
@DataJpaTest
@Import(GenreService.class)
class GenreServiceTest {

    private final GenreService genreService;


    private final GenreRepository genreRepository;


    private final BookRepository bookRepository;


    private final TestEntityManager em;

    public GenreServiceTest(GenreService genreService, GenreRepository genreRepository, BookRepository bookRepository, TestEntityManager em) {
        this.genreService = genreService;
        this.genreRepository = genreRepository;
        this.bookRepository = bookRepository;
        this.em = em;
    }

    @Test
    void findAllGenres_returnsAllGenres() {
        genreRepository.save(new Genre(null, "FantasyTest"));
        genreRepository.save(new Genre(null, "Science Fiction Test"));

        List<GenreDto> result = genreService.findAll();

        assertThat(result)
                .extracting(GenreDto::name)
                .containsAnyElementsOf(List.of("FantasyTest", "Science Fiction Test"));
    }

    @Test
    void findBooksByGenre_returnsBooks() {
        Genre fantasy = genreRepository.save(new Genre(null, "FantasyTest"));
        Author author = em.persist(new Author(null, "Author Test", LocalDate.now()));

        Book book = new Book(null, "Test Book", 2000, author, 2);
        book.addGenre(fantasy);
        bookRepository.save(book);
        em.flush();

        List<BookDto> books =
                genreService.findBooksByGenre("fantasytest");

        assertThat(books).hasSize(1);
        assertThat(books.get(0).title()).isEqualTo("Test Book");
    }

}
