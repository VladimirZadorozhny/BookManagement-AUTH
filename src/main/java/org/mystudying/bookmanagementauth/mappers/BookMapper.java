package org.mystudying.bookmanagementauth.mappers;

import org.mystudying.bookmanagementauth.domain.Book;
import org.mystudying.bookmanagementauth.dto.book.BookDto;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    public BookDto toDto(Book book) {
        return new BookDto(book.getId(), book.getTitle(), book.getYear(), book.getAvailable());
    }
}
