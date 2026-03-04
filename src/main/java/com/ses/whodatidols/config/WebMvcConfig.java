package com.ses.whodatidols.config;

import com.ses.whodatidols.interceptor.MaintenanceInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final MaintenanceInterceptor maintenanceInterceptor;

    public WebMvcConfig(MaintenanceInterceptor maintenanceInterceptor) {
        this.maintenanceInterceptor = maintenanceInterceptor;
    }

    @Override
    @SuppressWarnings("null")
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // Intercept all routes
        registry.addInterceptor(maintenanceInterceptor)
                .addPathPatterns("/**");
    }
}
