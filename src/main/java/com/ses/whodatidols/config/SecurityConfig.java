package com.ses.whodatidols.config;

import com.ses.whodatidols.model.Person;
import com.ses.whodatidols.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final IpBlockFilter ipBlockFilter;

        @Autowired
        public SecurityConfig(IpBlockFilter ipBlockFilter) {
                this.ipBlockFilter = ipBlockFilter;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .addFilterBefore(ipBlockFilter,
                                                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/sitemap.xml", "/robots.txt").permitAll()
                                                .requestMatchers("/admin/users", "/admin/ban-user",
                                                                "/admin/update-role", "/admin/delete-user")
                                                .hasAnyRole("SUPER_ADMIN", "KURUCU", "GELISTIRICI")
                                                .requestMatchers(
                                                                "/admin/panel", "/admin/me", "/admin/translate",
                                                                "/admin/movies", "/admin/series", "/admin/series/check",
                                                                "/admin/check-episode-collision",
                                                                "/admin/check-movie-collision",
                                                                "/admin/add-movie", "/admin/update-movie",
                                                                "/admin/update-series",
                                                                "/admin/add-series", "/admin/delete-episode",
                                                                "/admin/update-episode",
                                                                "/admin/delete-series-by-name", "/admin/delete-movie",
                                                                "/admin/delete-movies-bulk",
                                                                "/admin/delete-series-bulk",
                                                                "/admin/add-source", "/admin/delete-source",
                                                                "/admin/delete-sources-for-content")
                                                .hasAnyRole("CEVIRMEN", "ADMIN", "SUPER_ADMIN", "KURUCU", "GELISTIRICI")
                                                .requestMatchers("/admin/**")
                                                .hasAnyRole("ADMIN", "SUPER_ADMIN", "KURUCU", "GELISTIRICI")
                                                .anyRequest().permitAll())
                                .formLogin(form -> form
                                                .loginPage("/login-admin")
                                                .loginProcessingUrl("/login-admin")
                                                .defaultSuccessUrl("/admin", true)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout-admin")
                                                .logoutSuccessUrl("/login-admin")
                                                .permitAll());

                return http.build();
        }

        @Bean
        public UserDetailsService userDetailsService(PersonRepository personRepository) {
                return username -> {
                        // Fixed Super Admin
                        if ("admin".equals(username)) {
                                return User.builder()
                                                .username("admin")
                                                .password("{noop}qlXLvn3hbxGGJKT6tXhOjIUxHBw6doyH")
                                                .roles("SUPER_ADMIN", "KURUCU")
                                                .build();
                        }

                        // Other users from DB
                        Optional<Person> personOp = personRepository.findByNicknameOrEmail(username);
                        if (personOp.isPresent()) {
                                Person person = personOp.get();
                                // Check if user has ADMIN, SUPER_ADMIN, KURUCU, GELISTIRICI or CEVIRMEN role in
                                // DB
                                String dbRole = person.getRole();
                                if (dbRole != null && (dbRole.equalsIgnoreCase("ADMIN")
                                                || dbRole.equalsIgnoreCase("SUPER_ADMIN")
                                                || dbRole.equalsIgnoreCase("KURUCU")
                                                || dbRole.equalsIgnoreCase("GELISTIRICI")
                                                || dbRole.equalsIgnoreCase("CEVIRMEN"))) {

                                        String springRole = dbRole.toUpperCase();

                                        return User.builder()
                                                        .username(person.getNickname())
                                                        .password("{noop}" + person.getPassword())
                                                        .roles(springRole)
                                                        .build();
                                }
                        }

                        throw new UsernameNotFoundException("User not found or not authorized for admin panel");
                };
        }
}