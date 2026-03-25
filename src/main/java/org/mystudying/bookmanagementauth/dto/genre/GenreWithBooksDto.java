package org.mystudying.bookmanagementauth.dto.genre;

import org.mystudying.bookmanagementauth.dto.book.BookDto;

import java.util.List;

public record GenreWithBooksDto(Long id, String name, List<BookDto> books, long totalBooks) {
}

