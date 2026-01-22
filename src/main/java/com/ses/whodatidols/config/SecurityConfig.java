package com.ses.whodatidols.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF'i kapatıyoruz ki POST isteklerinde "Token" hatası almayalım
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests((requests) -> requests
                        // 1. Sadece Admin paneli "/admin/**" şifreli olsun (ROLE_ADMIN gerekir)
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 2. Kendi yazdığınız login endpoint'i dahil her şeye izin ver
                        // Özellikle "/login", "/register", "/api/**" gibi yolları açık bırakın
                        .requestMatchers("/login", "/register", "/", "/home/**", "/watch/**", "/css/**", "/js/**", "/images/**").permitAll()

                        // Geri kalan her şey de açık olsun
                        .anyRequest().permitAll()
                )

                // Spring Security'nin kendi login formu SADECE Admin paneline girmeye çalışınca çıksın
                // Sizin ana sayfanızdaki login butonu buraya düşmemeli.
                .formLogin((form) -> form
                        .loginPage("/login-admin") // Change default login page to avoid conflict
                        .permitAll()
                        // Admin giriş yaparsa nereye gitsin?
                        .defaultSuccessUrl("/admin/panel", true)
                )

                .logout((logout) -> logout.permitAll());

        return http.build();
    }

    // Bu kısım SADECE Admin paneli girişi için kullanılır.
    // Sizin SQL tabanlı kullanıcılarınız buraya takılmaz.
    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails admin = User.withDefaultPasswordEncoder()
                .username("admin")
                .password("Sh218106")
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }
}