package org.mystudying.bookmanagementauth.dto.book;

public record BookSearchCriteria(
        Boolean available,
        Integer year,
        String authorName,
        String title,
        String authorPartName,
        Long genreId
) {
}
