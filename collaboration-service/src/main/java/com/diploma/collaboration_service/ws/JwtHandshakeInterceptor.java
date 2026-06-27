package com.diploma.collaboration_service.ws;

import com.diploma.collaboration_service.client.SessionClient;
import com.diploma.collaboration_service.security.JwtParser;
import com.diploma.collaboration_service.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    // Matches both the code-collab channel (/ws/sessions/{id}) and the call
    // signaling channel (/ws/calls/{id}); both are scoped to a session UUID.
    private static final Pattern SESSION_PATH = Pattern.compile(".*/ws/(?:sessions|calls)/([0-9a-fA-F-]{36})/?$");

    private final JwtParser jwtParser;
    private final SessionClient sessionClient;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        String path = request.getURI().getPath();
        Matcher matcher = SESSION_PATH.matcher(path);
        if (!matcher.matches()) {
            return reject(response, HttpStatus.NOT_FOUND, "Unknown WS path: " + path);
        }
        UUID sessionId;
        try {
            sessionId = UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException e) {
            return reject(response, HttpStatus.BAD_REQUEST, "Invalid sessionId in path");
        }

        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            return reject(response, HttpStatus.UNAUTHORIZED, "Missing token");
        }

        JwtPrincipal principal;
        try {
            principal = jwtParser.parse(token);
        } catch (JwtParser.InvalidTokenException e) {
            log.debug("JWT parse failed: {}", e.getMessage());
            return reject(response, HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        SessionClient.SessionInfo info = sessionClient.fetchAccessible(sessionId, token);
        if (info == null) {
            return reject(response, HttpStatus.FORBIDDEN, "Session not accessible");
        }

        attributes.put(HandshakeAttrs.PRINCIPAL, principal);
        attributes.put(HandshakeAttrs.SESSION_ID, sessionId);
        attributes.put(HandshakeAttrs.SESSION_INFO, info);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }

    private String extractToken(ServerHttpRequest request) {
        Map<String, List<String>> queryParams = UriComponentsBuilder
                .fromUri(request.getURI())
                .build()
                .getQueryParams();
        List<String> values = queryParams.get("token");
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        List<String> authHeader = request.getHeaders().get("Authorization");
        if (authHeader != null && !authHeader.isEmpty() && authHeader.get(0).startsWith("Bearer ")) {
            return authHeader.get(0).substring("Bearer ".length());
        }
        return null;
    }

    private boolean reject(ServerHttpResponse response, HttpStatus status, String reason) {
        log.debug("WS handshake rejected: {} - {}", status, reason);
        if (response instanceof ServletServerHttpResponse servlet) {
            servlet.getServletResponse().setStatus(status.value());
        } else {
            response.setStatusCode(status);
        }
        return false;
    }
}
