package com.assessment.security;

import com.assessment.assessment.domain.AssessmentSession;
import com.assessment.assessment.port.out.SessionRepositoryPort;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Сервис управления cookie аутентификации сотрудника.
 * <p>
 * Отвечает только за HTTP-cookie сессии: проверку подписи cookie и установку
 * cookie в ответ. Валидация пригласительных токенов перенесена в use case
 * {@code InviteEmployeeUseCase}.
 */
@Component
public class EmployeeTokenService {

    private final SessionRepositoryPort sessionRepositoryPort;
    private final HmacTokenValidator hmacValidator;
    private final String cookieName;
    private final int tokenExpiryHours;

    /**
     * Конструктор сервиса cookie сессий сотрудников.
     *
     * @param sessionRepositoryPort выходной порт доступа к сессиям оценки
     * @param hmacValidator         валидатор HMAC-подписей
     * @param cookieName            имя cookie сессии
     * @param tokenExpiryHours      время жизни cookie в часах
     */
    public EmployeeTokenService(
            SessionRepositoryPort sessionRepositoryPort,
            HmacTokenValidator hmacValidator,
            @Value("${assessment.security.session-cookie-name}") String cookieName,
            @Value("${assessment.security.token-expiry-hours}") int tokenExpiryHours) {
        this.sessionRepositoryPort = sessionRepositoryPort;
        this.hmacValidator = hmacValidator;
        this.cookieName = cookieName;
        this.tokenExpiryHours = tokenExpiryHours;
    }

    /**
     * Проверяет cookie сессии сотрудника в HTTP-запросе.
     * <p>
     * Cookie содержит идентификатор сессии и HMAC-подпись, разделенные символом '|'.
     *
     * @param request HTTP-запрос с cookie
     * @return доменная сессия, если cookie валиден, иначе {@link Optional#empty()}
     */
    public Optional<AssessmentSession> validateSessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                String value = cookie.getValue();
                String[] parts = value.split("\\|");
                if (parts.length == 2) {
                    UUID sessionId = UUID.fromString(parts[0]);
                    String signature = parts[1];

                    return sessionRepositoryPort.findById(sessionId)
                            .filter(session -> hmacValidator.validateToken(session.getId().toString(), signature));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Устанавливает cookie сессии сотрудника в HTTP-ответе.
     * <p>
     * Cookie содержит идентификатор сессии и HMAC-подпись, защищен флагами HttpOnly,
     * Secure (при HTTPS/за прокси) и SameSite=Lax (защита от CSRF-подобных сценариев).
     *
     * @param request  HTTP-запрос для определения флага Secure
     * @param response HTTP-ответ, в который добавляется cookie
     * @param session  доменная сессия, для которой создается cookie
     */
    public void addSessionCookie(HttpServletRequest request, HttpServletResponse response, AssessmentSession session) {
        String signature = hmacValidator.generateToken(session.getId().toString());
        String value = session.getId().toString() + "|" + signature;

        ResponseCookie cookie = ResponseCookie.from(cookieName, value)
                .path("/")
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .maxAge((long) tokenExpiryHours * 3600)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}