package org.mystudying.bookmanagementauth.mappers;

import org.mystudying.bookmanagementauth.domain.Author;
import org.mystudying.bookmanagementauth.dto.AuthorDto;
import org.springframework.stereotype.Component;

@Component
public class AuthorMapper {

    public AuthorDto toDto(Author author) {
        return new AuthorDto(author.getId(), author.getName(), author.getBirthdate());
    }
}
