package com.ses.whodatidols.config;

import com.ses.whodatidols.model.Person;
import com.ses.whodatidols.repository.PersonRepository;
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

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/admin/users", "/admin/ban-user",
                                                                "/admin/update-role", "/admin/delete-user")
                                                .hasRole("SUPER_ADMIN")
                                                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
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
                                                .roles("SUPER_ADMIN")
                                                .build();
                        }

                        // Other users from DB
                        Optional<Person> personOp = personRepository.findByNicknameOrEmail(username);
                        if (personOp.isPresent()) {
                                Person person = personOp.get();
                                // Check if user has ADMIN or SUPER_ADMIN role in DB
                                if ("ADMIN".equalsIgnoreCase(person.getRole())
                                                || "SUPER_ADMIN".equalsIgnoreCase(person.getRole())) {
                                        String role = "ADMIN";
                                        if ("SUPER_ADMIN".equalsIgnoreCase(person.getRole())) {
                                                role = "SUPER_ADMIN";
                                        }

                                        return User.builder()
                                                        .username(person.getNickname())
                                                        .password("{noop}" + person.getPassword())
                                                        .roles(role)
                                                        .build();
                                }
                        }

                        throw new UsernameNotFoundException("User not found or not authorized for admin panel");
                };
        }
}