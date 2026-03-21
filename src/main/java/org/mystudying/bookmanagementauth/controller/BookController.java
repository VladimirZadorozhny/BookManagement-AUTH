package org.mystudying.bookmanagementauth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.mystudying.bookmanagementauth.domain.Book;
import org.mystudying.bookmanagementauth.dto.*;
import org.mystudying.bookmanagementauth.exceptions.BookNotFoundException;
import org.mystudying.bookmanagementauth.services.BookService;
import org.mystudying.bookmanagementauth.services.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

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
    public Page<BookDto> getAllBooks(@RequestParam Optional<Boolean> available,
                                     @RequestParam Optional<Integer> year,
                                     @RequestParam Optional<String> authorName,
                                     @RequestParam Optional<String> title,
                                     @RequestParam Optional<String> authorPartName,
                                     @RequestParam Optional<Long> genreId,
                                     @PageableDefault(size = 9) Pageable pageable) {
        if (available.isPresent()) {
            return bookService.findByAvailability(available.get(), pageable).map(this::toDto);
        }
        if (genreId.isPresent()) {
            return bookService.findByGenreId(genreId.get(), pageable).map(this::toDto);
        }
        if (year.isPresent()) {
            return bookService.findByYear(year.get(), pageable).map(this::toDto);
        }
        if (title.isPresent()) {
            return bookService.findByTitleContaining(title.get(), pageable).map(this::toDto);
        }
        if (authorPartName.isPresent()) {
            return bookService.findByAuthorNameContaining(authorPartName.get(), pageable).map(this::toDto);
        }
        if (authorName.isPresent()) {
            return bookService.findByAuthorName(authorName.get(), pageable).map(this::toDto);
        }
        return bookService.findAll(pageable).map(this::toDto);
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
