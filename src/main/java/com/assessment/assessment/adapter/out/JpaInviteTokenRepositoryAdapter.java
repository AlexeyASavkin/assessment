package com.assessment.assessment.adapter.out;

import com.assessment.assessment.domain.InviteToken;
import com.assessment.assessment.port.out.InviteTokenRepositoryPort;
import com.assessment.entity.AssessmentInviteToken;
import com.assessment.repository.AssessmentInviteTokenRepository;
import com.assessment.repository.EmployeeRepository;
import com.assessment.repository.SessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * JPA-адаптер выходного порта доступа к пригласительным токенам.
 *
 * <p>Оборачивает {@link AssessmentInviteTokenRepository},
 * {@link SessionRepository} и {@link EmployeeRepository}, преобразуя сущность
 * {@link AssessmentInviteToken} в доменную модель {@link InviteToken}.
 * Чтение ленивых ассоциаций (employee, session) выполняется в транзакции.
 */
@Component
public class JpaInviteTokenRepositoryAdapter implements InviteTokenRepositoryPort {

    private final AssessmentInviteTokenRepository assessmentInviteTokenRepository;
    private final SessionRepository sessionRepository;
    private final EmployeeRepository employeeRepository;

    public JpaInviteTokenRepositoryAdapter(AssessmentInviteTokenRepository assessmentInviteTokenRepository,
                                           SessionRepository sessionRepository,
                                           EmployeeRepository employeeRepository) {
        this.assessmentInviteTokenRepository = assessmentInviteTokenRepository;
        this.sessionRepository = sessionRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InviteToken> findByTokenHash(String tokenHash) {
        return assessmentInviteTokenRepository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    @Transactional
    public InviteToken save(InviteToken token) {
        AssessmentInviteToken entity;
        if (token.getId() != null) {
            entity = assessmentInviteTokenRepository.findById(token.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "AssessmentInviteToken not found: " + token.getId()));
            entity.setUsed(token.isUsed());
            entity.setUsedAt(token.getUsedAt());
            entity.setSession(token.getSessionId() != null
                    ? sessionRepository.getReferenceById(token.getSessionId())
                    : null);
        } else {
            entity = AssessmentInviteToken.builder()
                    .tokenHash(token.getTokenHash())
                    .employee(employeeRepository.getReferenceById(token.getEmployeeId()))
                    .session(token.getSessionId() != null
                            ? sessionRepository.getReferenceById(token.getSessionId())
                            : null)
                    .used(token.isUsed())
                    .usedAt(token.getUsedAt())
                    .expiresAt(token.getExpiresAt())
                    .build();
        }
        AssessmentInviteToken saved = assessmentInviteTokenRepository.save(entity);
        return toDomain(saved);
    }

    private InviteToken toDomain(AssessmentInviteToken e) {
        return InviteToken.of(
                e.getId(),
                e.getTokenHash(),
                e.getEmployee().getId(),
                e.getSession() != null ? e.getSession().getId() : null,
                Boolean.TRUE.equals(e.getUsed()),
                e.getUsedAt(),
                e.getExpiresAt(),
                e.getCreatedAt());
    }
}