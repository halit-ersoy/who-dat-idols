package com.ses.whodatidols.interceptor;

import com.ses.whodatidols.repository.SystemSettingRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MaintenanceInterceptor implements HandlerInterceptor {

    private final SystemSettingRepository systemSettingRepository;

    public MaintenanceInterceptor(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler)
            throws Exception {
        if (systemSettingRepository.isMaintenanceMode()) {
            String uri = request.getRequestURI();

            // Allow access to maintenance page itself
            if (uri.equals("/maintenance")) {
                return true;
            }

            // Allow access to admin panel and its API endpoints
            if (uri.startsWith("/admin") || uri.startsWith("/api/admin") || uri.startsWith("/panel/")
                    || uri.startsWith("/api/stats")) {
                return true;
            }

            // Allow access to login/authentication and error routes
            if (uri.equals("/login") || uri.equals("/login-admin") || uri.startsWith("/api/auth")
                    || uri.equals("/error")) {
                return true;
            }

            // Allow access to static resources so the maintenance page looks good
            if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/")
                    || uri.startsWith("/elements/") || uri.startsWith("/favicon")) {
                return true;
            }

            // If none of the above matches, redirect to the maintenance page
            response.sendRedirect("/maintenance");
            return false;
        }

        // If not in maintenance mode, proceed normally
        return true;
    }
}
