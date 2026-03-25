package org.mystudying.bookmanagementauth.dto.book;

public record BookDto(
        long id,
        String title,
        int year,
        int available
) {
}


