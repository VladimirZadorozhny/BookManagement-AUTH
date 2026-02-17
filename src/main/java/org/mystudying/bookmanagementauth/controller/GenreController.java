package org.mystudying.bookmanagementauth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.mystudying.bookmanagementauth.dto.BookDto;
import org.mystudying.bookmanagementauth.dto.CreateGenreRequestDto;
import org.mystudying.bookmanagementauth.dto.GenreDto;
import org.mystudying.bookmanagementauth.dto.GenreWithBooksDto;
import org.mystudying.bookmanagementauth.exceptions.GenreNotFoundException;
import org.mystudying.bookmanagementauth.services.GenreService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
@Tag(name = "Genres", description = "Management of book genres")
public class GenreController {

    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping
    public List<GenreDto> getAllGenres() {
        return genreService.findAll();
    }

    @GetMapping("/{id}")
    public GenreDto getGenreById(@PathVariable long id) {
        return genreService.findById(id)
                .orElseThrow(() -> new GenreNotFoundException(id));
    }

    @GetMapping("/with-books")
    public List<GenreWithBooksDto> getAllGenresWithBooks() {
        return genreService.findAllWithBooks();
    }

    @GetMapping("/{id}/books")
    public List<BookDto> getBooksByGenreId(@PathVariable long id) {
        return genreService.findBooksByGenreId(id);
    }

    @GetMapping("/name/{name}/books")
    public List<BookDto> getBooksByGenre(@PathVariable String name) {
        return genreService.findBooksByGenre(name);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public GenreDto createGenre(@Valid @RequestBody CreateGenreRequestDto requestDto) {
        return genreService.save(requestDto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public GenreDto updateGenre(@PathVariable long id, @Valid @RequestBody CreateGenreRequestDto requestDto) {
        return genreService.update(id, requestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteGenre(@PathVariable long id) {
        genreService.deleteById(id);
    }
}
