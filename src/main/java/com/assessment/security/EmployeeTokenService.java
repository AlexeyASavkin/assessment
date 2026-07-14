package com.assessment.security;

import com.assessment.entity.AssessmentInviteToken;
import com.assessment.entity.Employee;
import com.assessment.entity.Session;
import com.assessment.repository.AssessmentInviteTokenRepository;
import com.assessment.repository.EmployeeRepository;
import com.assessment.repository.SessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class EmployeeTokenService {

    private final AssessmentInviteTokenRepository tokenRepository;
    private final EmployeeRepository employeeRepository;
    private final SessionRepository sessionRepository;
    private final HmacTokenValidator hmacValidator;
    private final String cookieName;
    private final int tokenExpiryHours;

    public EmployeeTokenService(
            AssessmentInviteTokenRepository tokenRepository,
            EmployeeRepository employeeRepository,
            SessionRepository sessionRepository,
            HmacTokenValidator hmacValidator,
            @Value("${assessment.security.session-cookie-name}") String cookieName,
            @Value("${assessment.security.token-expiry-hours}") int tokenExpiryHours) {
        this.tokenRepository = tokenRepository;
        this.employeeRepository = employeeRepository;
        this.sessionRepository = sessionRepository;
        this.hmacValidator = hmacValidator;
        this.cookieName = cookieName;
        this.tokenExpiryHours = tokenExpiryHours;
    }

    public Optional<Session> validateInviteToken(String token) {
        return tokenRepository.findByTokenHash(token)
                .filter(t -> !t.getUsed())
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(tokenEntity -> {
                    Employee employee = tokenEntity.getEmployee();
                    Session session = Session.builder()
                            .employee(employee)
                            .build();
                    session = sessionRepository.save(session);

                    tokenEntity.setUsed(true);
                    tokenEntity.setUsedAt(LocalDateTime.now());
                    tokenEntity.setSession(session);
                    tokenRepository.save(tokenEntity);

                    return session;
                });
    }

    public Optional<Session> validateSessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                String value = cookie.getValue();
                String[] parts = value.split("\\|");
                if (parts.length == 2) {
                    UUID sessionId = UUID.fromString(parts[0]);
                    String signature = parts[1];

                    return sessionRepository.findById(sessionId)
                            .filter(session -> hmacValidator.validateToken(session.getId().toString(), signature));
                }
            }
        }
        return Optional.empty();
    }

    public void addSessionCookie(HttpServletResponse response, Session session) {
        String signature = hmacValidator.generateToken(session.getId().toString());
        String value = session.getId().toString() + "|" + signature;

        Cookie cookie = new Cookie(cookieName, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(tokenExpiryHours * 3600);
        response.addCookie(cookie);
    }
}
