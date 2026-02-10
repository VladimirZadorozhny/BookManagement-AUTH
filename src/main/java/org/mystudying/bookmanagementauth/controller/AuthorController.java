package org.mystudying.bookmanagementauth.controller;

import jakarta.validation.Valid;
import org.mystudying.bookmanagementauth.domain.Author;
import org.mystudying.bookmanagementauth.domain.Book;
import org.mystudying.bookmanagementauth.dto.AuthorDto;
import org.mystudying.bookmanagementauth.dto.BookDto;
import org.mystudying.bookmanagementauth.dto.CreateAuthorRequestDto;
import org.mystudying.bookmanagementauth.dto.UpdateAuthorRequestDto;
import org.mystudying.bookmanagementauth.exceptions.AuthorNotFoundException;
import org.mystudying.bookmanagementauth.services.AuthorService;
import org.mystudying.bookmanagementauth.services.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/authors")
public class AuthorController {

    private final AuthorService authorService;
    private final BookService bookService;

    public AuthorController(AuthorService authorService, BookService bookService) {
        this.authorService = authorService;
        this.bookService = bookService;
    }

    @GetMapping
    public List<AuthorDto> getAllAuthors() {
        return authorService.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public AuthorDto getAuthorById(@PathVariable long id) {
        return authorService.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new AuthorNotFoundException(id));
    }

    @GetMapping("/name/{name}")
    public AuthorDto getAuthorByName(@PathVariable String name) {
        return authorService.findByName(name)
                .map(this::toDto)
                .orElseThrow(() -> new AuthorNotFoundException(name));
    }

    @GetMapping("/{id}/books")
    public List<BookDto> getBooksByAuthorId(@PathVariable long id) {
        return bookService.findByAuthorId(id).stream()
                .map(this::toBookDto)
                .collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorDto createAuthor(@Valid @RequestBody CreateAuthorRequestDto authorDto) {
        var author = authorService.save(authorDto);
        return toDto(author);
    }

    @PutMapping("/{id}")
    public AuthorDto updateAuthor(@PathVariable long id, @Valid @RequestBody UpdateAuthorRequestDto authorDto) {
        Author authorToUpdate = authorService.update(id, authorDto);
        return toDto(authorToUpdate);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAuthor(@PathVariable long id) {

        authorService.deleteById(id);
    }

    private AuthorDto toDto(Author author) {
        return new AuthorDto(author.getId(), author.getName(), author.getBirthdate());
    }

    private BookDto toBookDto(Book book) {
        return new BookDto(book.getId(), book.getTitle(), book.getYear(), book.getAvailable());
    }
}


