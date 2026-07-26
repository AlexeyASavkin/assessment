package com.assessment.config;

import com.assessment.security.AdminUserDetailsService;
import com.assessment.security.HmacTokenValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Конфигурация Spring Security для приложения оценки компетенций.
 * <p>
 * Определяет правила доступа к API: форма входа для администраторов,
 * открытый доступ для сотрудников по пригласительным ссылкам,
 * отключение CSRF (требуется для REST API).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AdminUserDetailsService adminUserDetailsService;
    private final HmacTokenValidator hmacTokenValidator;

    /**
     * Конструктор, внедряющий зависимости сервиса пользователей и валидатора токенов.
     *
     * @param adminUserDetailsService сервис загрузки данных администраторов
     * @param hmacTokenValidator валидатор HMAC-токенов для пригласительных ссылок
     */
    public SecurityConfig(AdminUserDetailsService adminUserDetailsService,
                          HmacTokenValidator hmacTokenValidator) {
        this.adminUserDetailsService = adminUserDetailsService;
        this.hmacTokenValidator = hmacTokenValidator;
    }

    /**
     * Настраивает цепочку фильтров безопасности.
     * <p>
     * Разрешает доступ к входу администратора без аутентификации,
     * требует роль ADMIN для остальных админских endpoint'ов,
     * разрешает доступ к API сотрудников (аутентификация по cookie),
     * отключает CSRF и настраивает обработку form-login с JSON-ответами.
     *
     * @param http объект конфигурации HTTP-безопасности
     * @return настроенная цепочка фильтров {@link SecurityFilterChain}
     * @throws Exception при ошибке конфигурации
     */
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
