package org.mystudying.bookmanagementauth.services;

import org.mystudying.bookmanagementauth.domain.Genre;
import org.mystudying.bookmanagementauth.dto.book.BookDto;
import org.mystudying.bookmanagementauth.dto.genre.CreateGenreRequestDto;
import org.mystudying.bookmanagementauth.dto.genre.GenreDto;
import org.mystudying.bookmanagementauth.dto.genre.GenreWithBooksDto;
import org.mystudying.bookmanagementauth.exceptions.GenreHasBooksException;
import org.mystudying.bookmanagementauth.exceptions.GenreNotFoundException;
import org.mystudying.bookmanagementauth.mappers.BookMapper;
import org.mystudying.bookmanagementauth.mappers.GenreMapper;
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
    private final GenreMapper genreMapper;
    private final BookMapper bookMapper;

    public GenreService(GenreRepository genreRepository,
                        BookRepository bookRepository,
                        GenreMapper genreMapper,
                        BookMapper bookMapper) {
        this.genreRepository = genreRepository;
        this.bookRepository = bookRepository;
        this.genreMapper = genreMapper;
        this.bookMapper = bookMapper;
    }

    public List<GenreDto> findAll() {
        return genreRepository.findAll(Sort.by("name")).stream()
                .map(genreMapper::toDto)
                .toList();
    }

    public Page<GenreDto> findAll(Pageable pageable) {
        return genreRepository.findAll(pageable).map(genreMapper::toDto);

    }

    public Optional<GenreDto> findById(long id) {
        return genreRepository.findById(id)
                .map(genreMapper::toDto);
    }

    public List<BookDto> findBooksByGenre(String genreName) {
        if (!genreRepository.existsByNameIgnoreCase(genreName)) {
            throw new GenreNotFoundException(genreName);
        }
        return bookRepository.findByGenres_NameIgnoreCase(genreName)
                .stream()
                .map(bookMapper::toDto)
                .toList();
    }

    public Page<BookDto> findBooksByGenre(String genreName, Pageable pageable) {
        if (!genreRepository.existsByNameIgnoreCase(genreName)) {
            throw new GenreNotFoundException(genreName);
        }
        return bookRepository.findByGenres_NameIgnoreCase(genreName, pageable)
                .map(bookMapper::toDto);
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
                            .map(bookMapper::toDto)
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
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public GenreDto save(CreateGenreRequestDto requestDto) {
        Genre genre = new Genre(null, requestDto.name());
        Genre saved = genreRepository.save(genre);
        return genreMapper.toDto(saved);
    }

    @Transactional
    public GenreDto update(long id, CreateGenreRequestDto requestDto) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new GenreNotFoundException(id));
        genre.setName(requestDto.name());
        return genreMapper.toDto(genre);
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

