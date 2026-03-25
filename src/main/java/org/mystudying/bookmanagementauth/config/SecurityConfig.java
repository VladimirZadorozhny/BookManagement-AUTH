package org.mystudying.bookmanagementauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mystudying.bookmanagementauth.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JsonAuthenticationSuccessHandler successHandler;
    private final JsonAuthenticationFailureHandler failureHandler;
    private final CustomLogoutSuccessHandler logoutSuccessHandler;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(JsonAuthenticationSuccessHandler successHandler,
                          JsonAuthenticationFailureHandler failureHandler,
                          CustomLogoutSuccessHandler logoutSuccessHandler, CustomAccessDeniedHandler customAccessDeniedHandler) {
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationProvider authenticationProvider, AuthenticationEntryPoint authenticationEntryPoint) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        // set the name of the attribute the CsrfToken will be populated on
        requestHandler.setCsrfRequestAttributeName(null);

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/api/auth/login"), // Handled by formLogin
                                new AntPathRequestMatcher("/api/auth/register"), // Allow initial registration if needed, though usually CSRF is fine here
                                new AntPathRequestMatcher("/swagger-ui/**"),
                                new AntPathRequestMatcher("/v3/api-docs/**")
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. PUBLIC READ (Catalog & UI)
                        .requestMatchers(HttpMethod.GET, "/api/books/**", "/api/authors/**", "/api/genres/**").permitAll()
                        .requestMatchers("/", "/books", "/books/{id}", "/authors", "/authors/{id}", "/login", "/register", "/verify", "/reset-password", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        // 2. PUBLIC STATIC RESOURCES
                        .requestMatchers("/css/**", "/js/**", "/favicon.ico").permitAll()

                        // 3. PUBLIC AUTH (Login/Register APIs)
                        .requestMatchers("/api/auth/login", "/api/auth/register",
                                "/api/auth/password-reset-request",
                                "/api/auth/reset-password").permitAll()

                        // 4. ADMIN-ONLY MUTATIONS (Catalog Management)
                        .requestMatchers(HttpMethod.POST, "/api/books/**", "/api/authors/**", "/api/genres/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/books/**", "/api/authors/**", "/api/genres/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/books/**", "/api/authors/**", "/api/genres/**").hasRole("ADMIN")

                        // Specialized Inventory Endpoints (also covered by /api/books/** above, but good for clarity)
                        .requestMatchers("/api/books/*/inventory/**").hasRole("ADMIN")

                        // 5. ADMIN-ONLY REPORTS & USER MANAGEMENT
                        .requestMatchers("/api/reports/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                        .requestMatchers("/api/users/search").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/failed-mails/**").hasRole("ADMIN")

                        // 6. EVERYTHING ELSE (Authenticated)
                        // Ownership checks (e.g. /api/users/{id}/**) are enforced via @PreAuthorize in UserController
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login") // Keep standard URL for frontend
                        .loginProcessingUrl("/api/auth/login") // Specific API endpoint
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .authenticationProvider(authenticationProvider)
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .addFilterAfter(new RequestLoggingFilter(), SecurityContextHolderFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                );

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

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, authException) -> {

            if (request.getRequestURI().startsWith("/api/")) {
                // API → return JSON 401
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                ErrorResponse errorResponse = new ErrorResponse(
                        OffsetDateTime.now(ZoneOffset.UTC),
                        HttpStatus.UNAUTHORIZED.value(),
                        HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                        "Authentication required",
                        request.getRequestURI(),
                        "UNAUTHORIZED"
                );

                objectMapper.writeValue(response.getWriter(), errorResponse);

            } else {
                // UI → redirect to login
                response.sendRedirect("/login");
            }
        };
    }

    /**
     * Filter to subscribe to the CSRF token and ensure it is sent in a cookie.
     */
    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                response.setHeader(csrfToken.getHeaderName(), csrfToken.getToken());
            }
            filterChain.doFilter(request, response);
        }
    }

    private static final class RequestLoggingFilter extends OncePerRequestFilter {

        private final static Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            long start = System.currentTimeMillis();

            filterChain.doFilter(request, response);

            long time = System.currentTimeMillis() - start;

            String uri = request.getRequestURI();
            String query = request.getQueryString();
            String fullPath = query != null ? uri + "?" + query : uri;

            logger.info("{} {} -> {} ({} ms)", request.getMethod(), fullPath, response.getStatus(), time);
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String uri = request.getRequestURI();
            return uri.startsWith("/css/")
                    || uri.startsWith("/js/")
                    || uri.equals("/favicon.ico");
        }
    }
}
