package org.mystudying.bookmanagementauth.dto;

public record BookDto(
        long id,
        String title,
        int year,
        int available
) {
}


