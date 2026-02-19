package org.mystudying.bookmanagementauth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.mystudying.bookmanagementauth.domain.Book;
import org.mystudying.bookmanagementauth.dto.*;
import org.mystudying.bookmanagementauth.exceptions.BookNotFoundException;
import org.mystudying.bookmanagementauth.services.BookService;
import org.mystudying.bookmanagementauth.services.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/books")
@Tag(name = "Books", description = "Management of books catalog")
public class BookController {

    private final BookService bookService;
    private final InventoryService inventoryService;

    public BookController(BookService bookService, InventoryService inventoryService) {
        this.bookService = bookService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<BookDto> getAllBooks(@RequestParam Optional<Boolean> available,
                                     @RequestParam Optional<Integer> year,
                                     @RequestParam Optional<String> authorName,
                                     @RequestParam Optional<String> title,
                                     @RequestParam Optional<String> authorPartName,
                                     @RequestParam Optional<Long> genreId) {
        if (available.isPresent()) {
            return bookService.findByAvailability(available.get()).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        if (genreId.isPresent()) {
            return bookService.findByGenreId(genreId.get()).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        if (year.isPresent()) {
            return bookService.findByYear(year.get()).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        if (title.isPresent()) {
            return bookService.findByTitleContaining(title.get()).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        if (authorPartName.isPresent()) {
            return bookService.findByAuthorNameContaining(authorPartName.get()).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        if (authorName.isPresent()) {
            return bookService.findByAuthorName(authorName.get()).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
        return bookService.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public BookDto getBookById(@PathVariable long id) {
        return bookService.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @GetMapping("/{id}/details")
    public BookDetailDto getBookDetailsById(@PathVariable long id) {
        return bookService.findBookDetailsById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @GetMapping("/title/{title}")
    public BookDto getBookByTitle(@PathVariable String title) {
        return bookService.findByTitle(title)
                .map(this::toDto)
                .orElseThrow(() -> new BookNotFoundException(title));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public BookDto createBook(@Valid @RequestBody CreateBookRequestDto bookDto) {
        return toDto(bookService.save(bookDto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BookDto updateBook(@PathVariable long id, @Valid @RequestBody UpdateBookRequestDto bookDto) {
        return toDto(bookService.update(id, bookDto));
    }

    @PostMapping("/{id}/inventory/replenish")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void replenish(@PathVariable long id, @Valid @RequestBody InventoryRequestDto request) {
        inventoryService.replenish(id, request.amount());
    }

    @PostMapping("/{id}/inventory/write-off")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void writeOff(@PathVariable long id, @Valid @RequestBody InventoryRequestDto request) {
        inventoryService.writeOff(id, request.amount());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteBook(@PathVariable long id) {
           bookService.deleteById(id);
    }

    private BookDto toDto(Book book) {
        return new BookDto(book.getId(), book.getTitle(), book.getYear(), book.getAvailable());
    }
}
