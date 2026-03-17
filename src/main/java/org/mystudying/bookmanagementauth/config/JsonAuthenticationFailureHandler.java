package org.mystudying.bookmanagementauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mystudying.bookmanagementauth.dto.ErrorResponse;
import org.mystudying.bookmanagementauth.listeners.AdminUserMailListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class JsonAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(JsonAuthenticationFailureHandler.class);
    private final ObjectMapper objectMapper;

    public JsonAuthenticationFailureHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {

        String username = request.getParameter("username");
        String safeUsername = username != null ? username.replaceAll("(^.).*(@.*$)", "$1***$2") : "unknown";

        log.warn("Authentication failed for {}: {}", safeUsername, exception.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Invalid credentials",
                request.getRequestURI(),
                "AUTH_INVALID_CREDENTIALS"
        );

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
