package com.assessment.config;

import com.assessment.security.AdminUserDetailsService;
import com.assessment.security.HmacTokenValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AdminUserDetailsService adminUserDetailsService;
    private final HmacTokenValidator hmacTokenValidator;

    public SecurityConfig(AdminUserDetailsService adminUserDetailsService,
                          HmacTokenValidator hmacTokenValidator) {
        this.adminUserDetailsService = adminUserDetailsService;
        this.hmacTokenValidator = hmacTokenValidator;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/login").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/employee/**").permitAll()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginProcessingUrl("/api/admin/login")
                .successHandler((request, response, authentication) -> {
                    response.setStatus(200);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":\"authenticated\"}");
                })
                .failureHandler((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"invalid credentials\"}");
                })
            )
            .userDetailsService(adminUserDetailsService);

        return http.build();
    }

}
