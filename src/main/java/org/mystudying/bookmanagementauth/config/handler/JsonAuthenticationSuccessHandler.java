package org.mystudying.bookmanagementauth.config.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mystudying.bookmanagementauth.config.security.UserPrincipal;
import org.mystudying.bookmanagementauth.events.LoginSuccessEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class JsonAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public JsonAuthenticationSuccessHandler(ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        String email = authentication.getName();
        String name = ((UserPrincipal) authentication.getPrincipal()).getName();
        String ip = request.getRemoteAddr();

        // Publish login success event
        eventPublisher.publishEvent(new LoginSuccessEvent(email, name, ip, LocalDateTime.now()));

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");

        objectMapper.writeValue(response.getWriter(), Map.of("message", "Login successful"));
    }
}
