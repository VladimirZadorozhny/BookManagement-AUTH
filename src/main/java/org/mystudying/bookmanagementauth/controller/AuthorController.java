package org.mystudying.bookmanagementauth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.mystudying.bookmanagementauth.dto.author.AuthorDto;
import org.mystudying.bookmanagementauth.dto.book.BookDto;
import org.mystudying.bookmanagementauth.dto.author.CreateAuthorRequestDto;
import org.mystudying.bookmanagementauth.dto.author.UpdateAuthorRequestDto;
import org.mystudying.bookmanagementauth.exceptions.AuthorNotFoundException;
import org.mystudying.bookmanagementauth.services.AuthorService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authors")
@Tag(name = "Authors", description = "Management of authors")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping
    public List<AuthorDto> getAllAuthors() {
        return authorService.findAll();
    }

    @GetMapping("/{id}")
    public AuthorDto getAuthorById(@PathVariable long id) {
        return authorService.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException(id));
    }

    @GetMapping("/name/{name}")
    public AuthorDto getAuthorByName(@PathVariable String name) {
        return authorService.findByName(name)
                .orElseThrow(() -> new AuthorNotFoundException(name));
    }

    @GetMapping("/{id}/books")
    public List<BookDto> getBooksByAuthorId(@PathVariable long id) {
        return authorService.findBooksByAuthorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public AuthorDto createAuthor(@Valid @RequestBody CreateAuthorRequestDto authorDto) {
        return authorService.save(authorDto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AuthorDto updateAuthor(@PathVariable long id, @Valid @RequestBody UpdateAuthorRequestDto authorDto) {
        return authorService.update(id, authorDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteAuthor(@PathVariable long id) {
        authorService.deleteById(id);
    }

}
