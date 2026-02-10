package org.mystudying.bookmanagementauth.dto;

import java.util.List;

public record GenreWithBooksDto(Long id, String name, List<BookDto> books) {
}

