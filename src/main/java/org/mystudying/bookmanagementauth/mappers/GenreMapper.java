package org.mystudying.bookmanagementauth.mappers;

import org.mystudying.bookmanagementauth.domain.Genre;
import org.mystudying.bookmanagementauth.dto.genre.GenreDto;
import org.springframework.stereotype.Component;

@Component
public class GenreMapper {

    public GenreDto toDto(Genre genre) {
        return new GenreDto(genre.getId(), genre.getName());
    }
}
