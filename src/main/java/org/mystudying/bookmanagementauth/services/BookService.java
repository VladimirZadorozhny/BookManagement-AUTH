package org.mystudying.bookmanagementauth.services;

import org.mystudying.bookmanagementauth.domain.Book;
import org.mystudying.bookmanagementauth.domain.Genre;
import org.mystudying.bookmanagementauth.dto.book.BookDetailDto;
import org.mystudying.bookmanagementauth.dto.book.BookDto;
import org.mystudying.bookmanagementauth.dto.book.BookSearchCriteria;
import org.mystudying.bookmanagementauth.dto.book.CreateBookRequestDto;
import org.mystudying.bookmanagementauth.dto.book.UpdateBookRequestDto;
import org.mystudying.bookmanagementauth.exceptions.AuthorNotFoundException;
import org.mystudying.bookmanagementauth.exceptions.BookHasBookingsException;
import org.mystudying.bookmanagementauth.exceptions.BookNotFoundException;
import org.mystudying.bookmanagementauth.exceptions.GenreNotFoundException;
import org.mystudying.bookmanagementauth.mappers.BookMapper;
import org.mystudying.bookmanagementauth.repositories.AuthorRepository;
import org.mystudying.bookmanagementauth.repositories.BookRepository;
import org.mystudying.bookmanagementauth.repositories.GenreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BookService {
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final BookMapper bookMapper;

    public BookService(BookRepository bookRepository,
                       AuthorRepository authorRepository,
                       GenreRepository genreRepository,
                       BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.genreRepository = genreRepository;
        this.bookMapper = bookMapper;
    }

    public List<BookDto> findAll() {
        return bookRepository.findAll(Sort.by("title")).stream()
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
    }

    public Page<BookDto> findAll(Pageable pageable) {
        return bookRepository.findAll(pageable).map(bookMapper::toDto);
    }

    public Page<BookDto> findByCriteria(BookSearchCriteria criteria, Pageable pageable) {
        if (criteria.available() != null) {
            return bookRepository.findByAvailability(criteria.available(), pageable).map(bookMapper::toDto);
        }
        if (criteria.genreId() != null) {
            return bookRepository.findByGenres_Id(criteria.genreId(), pageable).map(bookMapper::toDto);
        }
        if (criteria.year() != null) {
            return bookRepository.findByYear(criteria.year(), pageable).map(bookMapper::toDto);
        }
        if (criteria.title() != null) {
            return bookRepository.findByTitleContaining(criteria.title(), pageable).map(bookMapper::toDto);
        }
        if (criteria.authorPartName() != null) {
            return bookRepository.findByAuthorNameContaining(criteria.authorPartName(), pageable).map(bookMapper::toDto);
        }
        if (criteria.authorName() != null) {
            return bookRepository.findByAuthorName(criteria.authorName(), pageable).map(bookMapper::toDto);
        }
        return findAll(pageable);
    }

    public Optional<BookDto> findById(long id) {
        return bookRepository.findById(id).map(bookMapper::toDto);
    }

    public Optional<BookDetailDto> findBookDetailsById(long id) {
        return bookRepository.findBookDetailsById(id)
                .map(dto -> {
                    dto.setGenres(genreRepository.findNamesByBookId(id));
                    return dto;
                });
    }

    public Optional<BookDto> findByTitle(String title) {
        return bookRepository.findByTitle(title).map(bookMapper::toDto);
    }

    @Transactional
    public BookDto save(CreateBookRequestDto createBookRequestDto) {
        // Validation of Author existence
        var author = authorRepository.findById(createBookRequestDto.authorId())
                .orElseThrow(() -> new AuthorNotFoundException(createBookRequestDto.authorId()));

        List<Long> requestedIds = createBookRequestDto.genreIds();

        List<Genre> genres = genreRepository.findAllById(requestedIds);

//        IDs that actually exist in DB
        Set<Long> foundIds = genres.stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());

        Set<Long> missingIds = new HashSet<>(requestedIds);

//        Remove all IDs that were found
        missingIds.removeAll(foundIds);

//        If anything remains -> those genres don't exist
        if (!missingIds.isEmpty()) {
            throw new GenreNotFoundException(missingIds.iterator().next());
        }


        Book book = new Book(null, createBookRequestDto.title(), createBookRequestDto.year(),
                author, createBookRequestDto.available());
        book.setGenres(new HashSet<>(genres));

        return bookMapper.toDto(bookRepository.save(book));
    }

    @Transactional
    public BookDto update(long id, UpdateBookRequestDto updateBookRequestDto) {
        var book = bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
        var author = authorRepository.findById(updateBookRequestDto.authorId()).orElseThrow(() ->
                new AuthorNotFoundException(updateBookRequestDto.authorId()));

        List<Long> requestedIds = updateBookRequestDto.genreIds();

        List<Genre> genres = genreRepository.findAllById(requestedIds);

//        IDs that actually exist in DB
        Set<Long> foundIds = genres.stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());

        Set<Long> missingIds = new HashSet<>(requestedIds);

//        Remove all IDs that were found
        missingIds.removeAll(foundIds);

//        If anything remains -> those genres don't exist
        if (!missingIds.isEmpty()) {
            throw new GenreNotFoundException(missingIds.iterator().next());
        }

        book.setTitle(updateBookRequestDto.title());
        book.setYear(updateBookRequestDto.year());
        book.setAuthor(author);
        book.setGenres(new HashSet<>(genres));

        return bookMapper.toDto(book);

    }

    @Transactional
    public void deleteById(long id) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
        if (!book.getBookings().isEmpty()) {
            throw new BookHasBookingsException(id);
        }
        bookRepository.delete(book);
    }
}
