package com.authservice.infrastructure.security;

import com.authservice.application.exception.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private static final String HEADER_API_KEY = "X-API-KEY";
    private static final String HEADER_API_KEY_ALIAS = "X-API_key";

    private final TenantResolver tenantResolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "/actuator/health".equals(uri)
                || ("/tenants".equals(uri) && "POST".equalsIgnoreCase(request.getMethod()))
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || "/swagger-ui.html".equals(uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(HEADER_API_KEY);
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = request.getHeader(HEADER_API_KEY_ALIAS);
        }

        try {
            var tenant = tenantResolver.resolve(apiKey);
            TenantContextHolder.setTenantId(tenant.getId());
            filterChain.doFilter(request, response);
        } catch (UnauthorizedException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + ex.getMessage() + "\"}");
        } finally {
            TenantContextHolder.clear();
        }
    }
}
