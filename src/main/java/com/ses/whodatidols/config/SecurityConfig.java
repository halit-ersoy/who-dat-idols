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
                .authorizeHttpRequests((requests) -> requests
                        // 1. Sadece Admin paneli şifreli olsun
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 2. Statik dosyalar ve diğer sayfalar herkese açık olsun
                        .requestMatchers("/", "/home/**", "/watch/**", "/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()

                        // Geri kalan her şey açık olsun (veya ihtiyaca göre kısıtlanabilir)
                        .anyRequest().permitAll()
                )
                .formLogin((form) -> form
                        // Admin paneline girmeye çalışanı Spring'in hazır login sayfasına at
                        .defaultSuccessUrl("/admin/panel", true)
                        .permitAll()
                )
                .logout((logout) -> logout.permitAll())
                // CSRF korumasını admin testleri için şimdilik kapatabiliriz (isteğe bağlı)
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        // İŞTE BURASI: Veritabanından bağımsız, RAM'de yaşayan kullanıcı
        UserDetails admin = User.withDefaultPasswordEncoder() // "{noop}" şifreleme kullanır (test için ideal)
                .username("admin")       // Kullanıcı Adı
                .password("Sh218106")   // Şifre (Bunu değiştirebilirsiniz)
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }
}