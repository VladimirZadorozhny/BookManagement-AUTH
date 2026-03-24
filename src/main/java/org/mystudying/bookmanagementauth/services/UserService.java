package org.mystudying.bookmanagementauth.services;

import org.mystudying.bookmanagementauth.domain.Role;
import org.mystudying.bookmanagementauth.domain.User;
import org.mystudying.bookmanagementauth.dto.CreateUserRequestDto;
import org.mystudying.bookmanagementauth.dto.UpdateUserRequestDto;
import org.mystudying.bookmanagementauth.dto.UserDto;
import org.mystudying.bookmanagementauth.exceptions.EmailAlreadyExistsException;
import org.mystudying.bookmanagementauth.exceptions.UserHasBookingsException;
import org.mystudying.bookmanagementauth.exceptions.UserNotFoundException;
import org.mystudying.bookmanagementauth.repositories.RoleRepository;
import org.mystudying.bookmanagementauth.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserDto> findAll() {
        return userRepository.findAll(Sort.by("name")).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Page<UserDto> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toDto);
    }

    public Optional<UserDto> findById(long id) {
        return userRepository.findById(id).map(this::toDto);
    }

    public Optional<UserDto> findByName(String name) {
        return userRepository.findByName(name).map(this::toDto);
    }

    public Optional<UserDto> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toDto);
    }

    public Optional<UserDto> findByNameOrEmail(String value) {
        Optional<UserDto> user = findByName(value);
        if (user.isEmpty()) {
            user = findByEmail(value);
        }
        return user;
    }

    @Transactional
    public UserDto save(CreateUserRequestDto createUserRequestDto) {
        try {
            User user = new User(null,
                    createUserRequestDto.name(),
                    createUserRequestDto.email(),
                    passwordEncoder.encode(createUserRequestDto.password()));
            user.setActive(true);

            roleRepository.findByName("ROLE_USER").ifPresent(user::addRole);

            User savedUser = userRepository.save(user);
            return toDto(savedUser);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException(createUserRequestDto.email());
        }
    }

    @Transactional
    public UserDto update(long id, UpdateUserRequestDto updateUserRequestDto) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        try {
            user.setName(updateUserRequestDto.name());
            user.setEmail(updateUserRequestDto.email());

            if (updateUserRequestDto.password() != null && !updateUserRequestDto.password().isBlank()) {
                user.setPassword(passwordEncoder.encode(updateUserRequestDto.password()));
            }

            if (updateUserRequestDto.active() != null) {
                user.setActive(updateUserRequestDto.active());
            }

            return toDto(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException(user.getEmail());
        }
    }

    @Transactional
    public void deleteById(long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        if (!user.getBookings().isEmpty()) {
            throw new UserHasBookingsException(id);
        }
        userRepository.delete(user);
    }

    @Transactional
    public void activateUser(long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        user.setActive(true);
    }

    @Transactional
    public void deactivateUser(long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        user.setActive(false);
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isActive(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet())
        );
    }
}
