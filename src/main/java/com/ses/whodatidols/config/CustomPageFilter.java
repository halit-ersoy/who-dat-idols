package com.ses.whodatidols.config;

import com.ses.whodatidols.service.CustomPageService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class CustomPageFilter extends OncePerRequestFilter {

    private final CustomPageService customPageService;

    private static final Set<String> SYSTEM_PREFIXES = Set.of(
            "admin", "panel", "api", "elements", "homepage", "assets", "static",
            "error", "watch", "login", "login-admin", "logout", "logout-admin",
            "register", "profile", "settings", "history", "favorites", "sss",
            "about", "privacy_policy", "terms_of_use", "bl-dizileri", "coming-soon",
            "programlar", "diziler", "filmler", "search", "streamlive", "uploads",
            "favicon.ico", "robots.txt", "sitemap.xml", "manifest.json", "sw.js"
    );

    public CustomPageFilter(CustomPageService customPageService) {
        this.customPageService = customPageService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path == null || path.isEmpty() || path.equals("/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // e.g. /xyz or /xyz/style.css
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        int slashIdx = cleanPath.indexOf('/');
        String firstSegment = (slashIdx == -1) ? cleanPath : cleanPath.substring(0, slashIdx);

        if (firstSegment.isBlank() || SYSTEM_PREFIXES.contains(firstSegment.toLowerCase())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Only intercept if firstSegment is an active CustomPage!
        if (customPageService.findActiveBySlug(firstSegment).isPresent()) {
            String subPath = (slashIdx == -1) ? "" : cleanPath.substring(slashIdx);
            boolean served = customPageService.servePageDirectly(firstSegment, subPath, request, response);
            if (served) {
                return;
            }
        }

        // Otherwise pass-through completely (e.g. movies, series, watch pages)
        filterChain.doFilter(request, response);
    }
}
