package com.assessment.config;

import com.assessment.security.AdminUserDetailsService;
import com.assessment.security.HmacTokenValidator;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Конфигурация Spring Security для приложения оценки компетенций.
 * <p>
 * Определяет правила доступа к API: форма входа для администраторов,
 * открытый доступ для сотрудников по пригласительным ссылкам,
 * CSRF-защита админских endpoint'ов по SPA-паттерну (cookie-токен),
 * запрет всех неизвестных путей.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AdminUserDetailsService adminUserDetailsService;
    private final HmacTokenValidator hmacTokenValidator;
    private final LoginAttemptRateLimiter loginAttemptRateLimiter;

    /**
     * Конструктор, внедряющий зависимости сервиса пользователей, валидатора токенов
     * и rate limiter'а неудачных попыток входа.
     *
     * @param adminUserDetailsService сервис загрузки данных администраторов
     * @param hmacTokenValidator валидатор HMAC-токенов для пригласительных ссылок
     * @param loginAttemptRateLimiter лимитер неудачных попыток входа (5 за 15 минут)
     */
    public SecurityConfig(AdminUserDetailsService adminUserDetailsService,
                          HmacTokenValidator hmacTokenValidator,
                          LoginAttemptRateLimiter loginAttemptRateLimiter) {
        this.adminUserDetailsService = adminUserDetailsService;
        this.hmacTokenValidator = hmacTokenValidator;
        this.loginAttemptRateLimiter = loginAttemptRateLimiter;
    }

    /**
     * Настраивает цепочку фильтров безопасности.
     * <p>
     * Разрешает доступ к входу администратора без аутентификации,
     * требует роль ADMIN для остальных админских endpoint'ов,
     * разрешает доступ к API сотрудников (аутентификация по cookie),
     * включает CSRF-защиту для админских endpoint'ов по SPA-паттерну
     * ({@code CookieCsrfTokenRepository.withHttpOnlyFalse()} + deferred-token
     * request handler, см. {@code CsrfConfigurer.spa()}), оставляя API
     * сотрудников без CSRF (SameSite=Lax cookie уже смягчает риск),
     * и запрещает все неизвестные пути.
     *
     * @param http объект конфигурации HTTP-безопасности
     * @return настроенная цепочка фильтров {@link SecurityFilterChain}
     * @throws Exception при ошибке конфигурации
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/employee/**")
                .spa()
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/login").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/employee/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().denyAll()
            )
            .formLogin(form -> form
                .loginProcessingUrl("/api/admin/login")
                .successHandler((request, response, authentication) -> {
                    response.setStatus(200);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":\"authenticated\"}");
                })
                .failureHandler((request, response, exception) -> {
                    String username = request.getParameter("username");
                    try {
                        loginAttemptRateLimiter.check(username);
                    } catch (RequestNotPermitted e) {
                        response.setStatus(429);
                        response.setHeader("Retry-After", "900");
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"too many failed login attempts\"}");
                        return;
                    }
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"invalid credentials\"}");
                })
            )
            .userDetailsService(adminUserDetailsService)
            .addFilterBefore(loginLockoutFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Фильтр блокировки входа: отклоняет POST на {@code /api/admin/login} с HTTP 429
     * и заголовком {@code Retry-After}, если bucket неудачных попыток пользователя
     * уже исчерпан ({@link LoginAttemptRateLimiter#isBlocked}). Работает ДО
     * аутентификации, поэтому во время окна блокировки отклоняется любая попытка
     * входа — включая с верным паролем. Счётчик попыток пополняется только в
     * failure handler'е (на неудачных попытках), фильтр лишь проверяет состояние.
     *
     * @return фильтр блокировки входа
     */
    private OncePerRequestFilter loginLockoutFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                if ("POST".equalsIgnoreCase(request.getMethod())
                        && "/api/admin/login".equals(request.getRequestURI())
                        && loginAttemptRateLimiter.isBlocked(request.getParameter("username"))) {
                    response.setStatus(429);
                    response.setHeader("Retry-After", "900");
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"too many failed login attempts\"}");
                    return;
                }
                filterChain.doFilter(request, response);
            }
        };
    }

}
