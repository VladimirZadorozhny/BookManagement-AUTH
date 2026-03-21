package org.mystudying.bookmanagementauth.services;

import org.mystudying.bookmanagementauth.domain.Genre;
import org.mystudying.bookmanagementauth.dto.*;
import org.mystudying.bookmanagementauth.exceptions.GenreHasBooksException;
import org.mystudying.bookmanagementauth.exceptions.GenreNotFoundException;
import org.mystudying.bookmanagementauth.repositories.BookRepository;
import org.mystudying.bookmanagementauth.repositories.GenreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GenreService {

    private final GenreRepository genreRepository;
    private final BookRepository bookRepository;

    public GenreService(GenreRepository genreRepository, BookRepository bookRepository) {
        this.genreRepository = genreRepository;
        this.bookRepository = bookRepository;
    }

    public List<GenreDto> findAll() {
        return genreRepository.findAll(Sort.by("name")).stream()
                .map(genre -> new GenreDto(genre.getId(), genre.getName()))
                .toList();
    }

    public Page<GenreDto> findAll(Pageable pageable) {
        return genreRepository.findAll(pageable).map(genre -> new GenreDto(genre.getId(), genre.getName()));

    }

    public Optional<GenreDto> findById(long id) {
        return genreRepository.findById(id)
                .map(genre -> new GenreDto(genre.getId(), genre.getName()));
    }

    public List<BookDto> findBooksByGenre(String genreName) {
        if (!genreRepository.existsByNameIgnoreCase(genreName)) {
            throw new GenreNotFoundException(genreName);
        }
        return bookRepository.findByGenres_NameIgnoreCase(genreName)
                .stream()
                .map(book -> new BookDto(
                        book.getId(),
                        book.getTitle(),
                        book.getYear(),
                        book.getAvailable()
                ))
                .toList();
    }

    public Page<BookDto> findBooksByGenre(String genreName, Pageable pageable) {
        if (!genreRepository.existsByNameIgnoreCase(genreName)) {
            throw new GenreNotFoundException(genreName);
        }
        return bookRepository.findByGenres_NameIgnoreCase(genreName, pageable)
                .map(book -> new BookDto(
                        book.getId(),
                        book.getTitle(),
                        book.getYear(),
                        book.getAvailable()
                ));
    }


//    public List<GenreWithBooksDto> findAllWithBooks() {
//        return genreRepository.findAllWithBooks().stream()
//                .map(genre -> new GenreWithBooksDto(
//                        genre.getId(),
//                        genre.getName(),
//                        genre.getBooks().stream()
//                                .map(book -> new BookDto(
//                                        book.getId(),
//                                        book.getTitle(),
//                                        book.getYear(),
//                                        book.getAvailable()
//                                ))
//                                .toList()
//                ))
//                .toList();
//    }

    public Page<GenreWithBooksDto> findAllWithBooks(Pageable pageable) {
        return genreRepository.findAllWithBooks(pageable)
                .map(genre -> {
                    List<BookDto> limitedBooks = genre.getBooks()
                            .stream()
                            .limit(5)
                            .map(book -> new BookDto(
                                    book.getId(),
                                    book.getTitle(),
                                    book.getYear(),
                                    book.getAvailable()
                            ))
                            .toList();

                    long total = bookRepository.countBooksByGenreId(genre.getId());

                    return new GenreWithBooksDto(
                            genre.getId(),
                            genre.getName(),
                            limitedBooks,
                            total
                    );
                });
    }

    public List<BookDto> findBooksByGenreId(long id) {
        genreRepository.findById(id)
                .orElseThrow(() -> new GenreNotFoundException(id));
        return bookRepository.findByGenres_Id(id).stream()
                .map(book -> new BookDto(book.getId(), book.getTitle(), book.getYear(), book.getAvailable()))
                .collect(Collectors.toList());
    }

    @Transactional
    public GenreDto save(CreateGenreRequestDto requestDto) {
        Genre genre = new Genre(null, requestDto.name());
        Genre saved = genreRepository.save(genre);
        return new GenreDto(saved.getId(), saved.getName());
    }

    @Transactional
    public GenreDto update(long id, CreateGenreRequestDto requestDto) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new GenreNotFoundException(id));
        genre.setName(requestDto.name());
        return new GenreDto(genre.getId(), genre.getName());
    }

    @Transactional
    public void deleteById(long id) {
        if (!genreRepository.existsById(id)) {
            throw new GenreNotFoundException(id);
        }
        if (bookRepository.existsByGenres_Id(id)) {
            throw new GenreHasBooksException(id);
        }
        genreRepository.deleteById(id);
    }
}

