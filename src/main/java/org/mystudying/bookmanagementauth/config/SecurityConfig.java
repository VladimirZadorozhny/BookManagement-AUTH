package org.mystudying.bookmanagementauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationProvider authenticationProvider) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. PUBLIC READ (Catalog)
                        .requestMatchers(HttpMethod.GET, "/api/books/**", "/api/authors/**", "/api/genres/**").permitAll()

                        // 2. PUBLIC WEB & STATIC RESOURCES
                        .requestMatchers("/", "/index.html", "/login", "/register", "/css/**", "/js/**",
                                "/books.html", "/authors.html", "/genres.html", "/reports.html",
                                "/user.html", "/users.html", "/author.html", "/book.html",
                                "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

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
                .formLogin(form -> form
                        .loginPage("/login") // Keep standard URL for frontend
                        .loginProcessingUrl("/api/auth/login") // Specific API endpoint
                        .successHandler((request, response, authentication) -> {
                            response.setStatus(200);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\": \"Login successful\"}");
                        })
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            String json = String.format(
                                "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid credentials\",\"path\":\"%s\",\"code\":\"UNAUTHORIZED\"}",
                                java.time.OffsetDateTime.now().toString(),
                                request.getRequestURI()
                            );
                            response.getWriter().write(json);
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(204);
                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .httpBasic(withDefaults())
                .authenticationProvider(authenticationProvider);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                         PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        return daoAuthenticationProvider;

    }
}
