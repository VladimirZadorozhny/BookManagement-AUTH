package org.mystudying.bookmanagementauth.services;

import org.mystudying.bookmanagementauth.domain.Author;
import org.mystudying.bookmanagementauth.dto.author.AuthorDto;
import org.mystudying.bookmanagementauth.dto.book.BookDto;
import org.mystudying.bookmanagementauth.dto.author.CreateAuthorRequestDto;
import org.mystudying.bookmanagementauth.dto.author.UpdateAuthorRequestDto;
import org.mystudying.bookmanagementauth.exceptions.AuthorHasBooksException;
import org.mystudying.bookmanagementauth.exceptions.AuthorNotFoundException;
import org.mystudying.bookmanagementauth.mappers.AuthorMapper;
import org.mystudying.bookmanagementauth.mappers.BookMapper;
import org.mystudying.bookmanagementauth.repositories.AuthorRepository;
import org.mystudying.bookmanagementauth.repositories.BookRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AuthorService {
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final AuthorMapper authorMapper;
    private final BookMapper bookMapper;

    public AuthorService(AuthorRepository authorRepository,
                         BookRepository bookRepository,
                         AuthorMapper authorMapper,
                         BookMapper bookMapper) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
        this.authorMapper = authorMapper;
        this.bookMapper = bookMapper;
    }

    public List<AuthorDto> findAll() {
        return authorRepository.findAll(Sort.by("name")).stream()
                .map(authorMapper::toDto)
                .toList();
    }

    public Optional<AuthorDto> findById(long id) {
        return authorRepository.findById(id).map(authorMapper::toDto);
    }

    public Optional<AuthorDto> findByName(String name) {
        return authorRepository.findByName(name).map(authorMapper::toDto);
    }

    @Transactional
    public AuthorDto save(CreateAuthorRequestDto authorDto) {
        return authorMapper.toDto(
                authorRepository.save(new Author(null, authorDto.name(), authorDto.birthdate()))
        );
    }

    @Transactional
    public AuthorDto update(long id, UpdateAuthorRequestDto authorDto) {

        var author = authorRepository.findById(id).orElseThrow(() -> new AuthorNotFoundException(id));
        author.setName(authorDto.name());
        author.setBirthdate(authorDto.birthdate());

        return authorMapper.toDto(author);
    }

    @Transactional
    public void deleteById(long id) {
        var author = authorRepository.findById(id).orElseThrow(() -> new AuthorNotFoundException(id));
        if (bookRepository.existsByAuthor(author)) {
            throw new AuthorHasBooksException(id);
        }
        authorRepository.deleteById(author.getId());
    }

    public List<BookDto> findBooksByAuthorId(long id) {
        authorRepository.findById(id).orElseThrow(() -> new AuthorNotFoundException(id));
        return bookRepository.findByAuthor_Id(id).stream()
                .map(bookMapper::toDto)
                .toList();
    }
}
