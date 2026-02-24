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

    @Autowired
    public IpBlockFilter(BannedIpRepository bannedIpRepository) {
        this.bannedIpRepository = bannedIpRepository;
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
            clientIp = request.getRemoteAddr();
        } else {
            clientIp = clientIp.split(",")[0].trim();
        }

        if (bannedIpRepository.isBanned(clientIp)) {
            response.sendRedirect("/banned");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
