package com.authservice.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.authservice.application.service.IdentityProviderClient;
import com.authservice.application.service.model.TokenValidationResult;
import com.authservice.application.exception.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final IdentityProviderClient identityProviderClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length());
            try {
                TokenValidationResult validationResult = identityProviderClient.validateToken(token);

                Set<SimpleGrantedAuthority> authorities = new HashSet<>(validationResult.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                        .collect(Collectors.toSet()));
                authorities.addAll(validationResult.getScopes().stream()
                        .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                        .collect(Collectors.toSet()));

                String principal = validationResult.getSubject();
                if (principal == null || principal.isBlank()) {
                    principal = extractSubjectFromJwt(token);
                }
                if (principal == null || principal.isBlank()) {
                    principal = validationResult.getUsername();
                }
                if ((principal == null || principal.isBlank()) && validationResult.getClientId() != null && !validationResult.getClientId().isBlank()) {
                    principal = validationResult.getClientId();
                }

                if (validationResult.isMachineToken() && validationResult.getClientId() != null && !validationResult.getClientId().isBlank()) {
                    principal = validationResult.getClientId();
                }

                if (principal == null || principal.isBlank()) {
                    throw new UnauthorizedException("Token principal is missing");
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (UnauthorizedException ex) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + ex.getMessage() + "\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractSubjectFromJwt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }

            byte[] decodedPayload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = OBJECT_MAPPER.readTree(new String(decodedPayload, StandardCharsets.UTF_8));
            JsonNode sub = payload.get("sub");
            if (sub == null || sub.isNull()) {
                return null;
            }
            String subject = sub.asText();
            return subject == null || subject.isBlank() ? null : subject;
        } catch (Exception ex) {
            return null;
        }
    }
}
