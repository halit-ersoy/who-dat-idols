package com.ses.whodatidols.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;
import java.io.IOException;

/**
 * Filter that forces resolution of the CSRF token on every request.
 * This ensures that Spring Security 6 writes the CSRF token to the response cookie
 * (e.g. XSRF-TOKEN) so it can be read by JavaScript clients.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                    @NonNull HttpServletResponse response, 
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Force generation of the token, triggering it to be written to the cookie
            csrfToken.getToken();
        }
        
        filterChain.doFilter(request, response);
    }
}
