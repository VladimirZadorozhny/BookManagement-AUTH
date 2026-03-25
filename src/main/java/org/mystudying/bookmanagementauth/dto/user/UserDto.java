package org.mystudying.bookmanagementauth.dto.user;

import java.util.Set;

public record UserDto(
        long id,
        String name,
        String email,
        boolean active,
        Set<String> roles
) {
}