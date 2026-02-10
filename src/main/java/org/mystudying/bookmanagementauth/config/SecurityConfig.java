package org.mystudying.bookmanagementauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints
                .requestMatchers(HttpMethod.GET,"/api/authors/**").permitAll() // Authors GETs
                    .requestMatchers(HttpMethod.GET,"/api/books/**").permitAll()    // Books GETs
                    .requestMatchers(HttpMethod.GET,"/api/genres/**").permitAll() // Genres GETs

//                    static resources
                    .requestMatchers("/login", "/register", "/css/**", "/js/**", "/", "/index.html", "/books.html",
                                "/authors.html", "/genres.html", "/reports.html", "/user.html", "/users.html", "/author.html",
                                 "/book.html").permitAll() // Frontend resources & login/register pages


                // Admin-only endpoints
                .requestMatchers(
                    "/api/authors/**", // POST, PUT, DELETE for authors
                    "/api/books/**",     // POST, PUT, DELETE for books
                    "/api/genres/**",   // POST, PUT, DELETE for genres (need to be added later)
                    "/api/reports/bookings",             // Admin reports
                    "/api/users/**"      // POST, PUT, DELETE for users (admin management)
                ).hasRole("ADMIN")

                // Authenticated users (ROLE_USER or ROLE_ADMIN) can access
                .requestMatchers(
                    "/api/users/{id}/rent", "/api/users/{id}/return", "/api/users/{userId}/bookings/{bookingId}/pay",
                    "/api/users/{id}/bookings", "/api/users/{id}/books", "/api/users/search"
                ).authenticated() // More specific authorization for these will come with @PreAuthorize

                // Catch-all for any other authenticated paths
                .anyRequest().authenticated()
            )
            .formLogin(withDefaults()) // Use default form login
            .httpBasic(withDefaults()); // Enable HTTP Basic for API clients
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}