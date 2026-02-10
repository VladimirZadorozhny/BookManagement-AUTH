package org.mystudying.bookmanagementauth.services;

import org.mystudying.bookmanagementauth.domain.Author;
import org.mystudying.bookmanagementauth.dto.CreateAuthorRequestDto;
import org.mystudying.bookmanagementauth.dto.UpdateAuthorRequestDto;
import org.mystudying.bookmanagementauth.exceptions.AuthorHasBooksException;
import org.mystudying.bookmanagementauth.exceptions.AuthorNotFoundException;
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

    public AuthorService(AuthorRepository authorRepository, BookRepository bookRepository) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
    }

    public List<Author> findAll() {
        return authorRepository.findAll(Sort.by("name"));
    }

    public Optional<Author> findById(long id) {
        return authorRepository.findById(id);
    }

    public Optional<Author> findByName(String name) {
        return authorRepository.findByName(name);
    }

    @Transactional
    public Author save(CreateAuthorRequestDto authorDto) {
        return  authorRepository.save(new Author(null, authorDto.name(), authorDto.birthdate()));
    }

    @Transactional
    public Author update(long id, UpdateAuthorRequestDto authorDto) {

        var author =  authorRepository.findById(id).orElseThrow(() -> new AuthorNotFoundException(id));
        author.setName(authorDto.name());
        author.setBirthdate(authorDto.birthdate());

        return author;
    }

    @Transactional
    public void deleteById(long id) {
        var author = authorRepository.findById(id).orElseThrow(() -> new AuthorNotFoundException(id));
        if (bookRepository.existsByAuthor(author)) {
            throw new AuthorHasBooksException(id);
        }
        authorRepository.deleteById(author.getId());
    }
}
