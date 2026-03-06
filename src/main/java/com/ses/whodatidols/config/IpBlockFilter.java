package com.ses.whodatidols.config;

import com.ses.whodatidols.repository.BannedIpRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class IpBlockFilter extends OncePerRequestFilter {

    private final BannedIpRepository bannedIpRepository;
    private final com.ses.whodatidols.service.LoginAttemptService loginAttemptService;

    @Autowired
    public IpBlockFilter(BannedIpRepository bannedIpRepository,
            com.ses.whodatidols.service.LoginAttemptService loginAttemptService) {
        this.bannedIpRepository = bannedIpRepository;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Allow access to banned page and appeal APIs
        if (path.equals("/banned") ||
                path.startsWith("/api/security/ban-info") ||
                path.startsWith("/api/security/appeal") ||
                path.startsWith("/error") ||
                path.startsWith("/favicon.ico")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getHeader("X-Real-IP");
        }
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        } else {
            clientIp = clientIp.split(",")[0].trim();
        }

        if (bannedIpRepository.isBanned(clientIp) || loginAttemptService.isBlocked(clientIp)) {
            // For AJAX/API requests, return 429 so the frontend can handle the reload
            if (path.startsWith("/api/") || path.equals("/login")) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Blocked\"}");
                return;
            }
            response.sendRedirect("/banned");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
