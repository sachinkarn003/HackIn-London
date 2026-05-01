package com.karn01.productservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AdminAuthorizationFilter extends OncePerRequestFilter {
    private static final String ADMIN_ROLE = "ADMIN";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (requiresAdmin(request)) {
            String role = request.getHeader("X-User-Role");
            if (!ADMIN_ROLE.equalsIgnoreCase(role)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Admin access required\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresAdmin(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.startsWith("/actuator") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            return false;
        }

        if (path.startsWith("/admin/")) {
            return true;
        }

        if (path.startsWith("/products/internal/")) {
            return false;
        }

        return path.startsWith("/products")
                && !HttpMethod.GET.matches(method);
    }
}
