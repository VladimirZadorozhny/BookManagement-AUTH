package org.mystudying.bookmanagementauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 1. PUBLIC READ (Catalog)
                .requestMatchers(HttpMethod.GET, "/api/books/**", "/api/authors/**", "/api/genres/**").permitAll()

                // 2. PUBLIC WEB & STATIC RESOURCES
                .requestMatchers("/", "/index.html", "/login", "/register", "/css/**", "/js/**",
                                "/books.html", "/authors.html", "/genres.html", "/reports.html",
                                "/user.html", "/users.html", "/author.html", "/book.html").permitAll()

                // 3. PUBLIC AUTH (Login/Register APIs)
                .requestMatchers("/api/auth/**").permitAll()

                // 4. ADMIN-ONLY MUTATIONS (Catalog Management)
                .requestMatchers(HttpMethod.POST, "/api/books/**", "/api/authors/**", "/api/genres/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/books/**", "/api/authors/**", "/api/genres/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/books/**", "/api/authors/**", "/api/genres/**").hasRole("ADMIN")

                // 5. ADMIN-ONLY REPORTS & USER MANAGEMENT
                .requestMatchers("/api/reports/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                .requestMatchers("/api/users/search").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")

                // 6. EVERYTHING ELSE (Authenticated)
                // Ownership checks (e.g. /api/users/{id}/**) are enforced via @PreAuthorize in UserController
                .anyRequest().authenticated()
            )
            .formLogin(withDefaults())
            .httpBasic(withDefaults());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
