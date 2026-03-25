package org.mystudying.bookmanagementauth.dto.author;

import java.time.LocalDate;

public record AuthorDto(
        long id,
        String name,
        LocalDate birthdate
) {
}


