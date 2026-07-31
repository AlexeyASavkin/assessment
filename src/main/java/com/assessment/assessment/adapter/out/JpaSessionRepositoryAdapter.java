package com.assessment.assessment.adapter.out;

import com.assessment.assessment.domain.AssessmentSession;
import com.assessment.assessment.domain.SessionStatus;
import com.assessment.assessment.port.out.SessionRepositoryPort;
import com.assessment.entity.Employee;
import com.assessment.entity.Session;
import com.assessment.repository.EmployeeRepository;
import com.assessment.repository.SessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA-адаптер выходного порта доступа к сессиям оценки.
 *
 * <p>Оборачивает {@link SessionRepository} и {@link EmployeeRepository} и
 * преобразует сущность {@link Session} в доменную модель
 * {@link AssessmentSession}. Методы, читающие ленивые ассоциации
 * (employee, employee.competency), выполняются в транзакции.
 */
@Component
public class JpaSessionRepositoryAdapter implements SessionRepositoryPort {

    private final SessionRepository sessionRepository;
    private final EmployeeRepository employeeRepository;

    public JpaSessionRepositoryAdapter(SessionRepository sessionRepository,
                                        EmployeeRepository employeeRepository) {
        this.sessionRepository = sessionRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssessmentSession> findById(UUID sessionId) {
        return sessionRepository.findById(sessionId).map(this::toDomain);
    }

    @Override
    @Transactional
    public AssessmentSession save(AssessmentSession session) {
        Session entity;
        if (session.getId() != null) {
            entity = sessionRepository.findById(session.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Session not found: " + session.getId()));
            entity.setStatus(session.getStatus().value());
            entity.setCurrentQuestionId(session.getCurrentQuestionId());
        } else {
            entity = Session.builder()
                    .employee(employeeRepository.getReferenceById(session.getEmployeeId()))
                    .build();
        }
        Session saved = sessionRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssessmentSession> findFirstByEmployeeIdOrderByCreatedAtDesc(UUID employeeId) {
        return sessionRepository.findFirstByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .map(this::toDomain);
    }

    private AssessmentSession toDomain(Session e) {
        Employee employee = e.getEmployee();
        return AssessmentSession.of(
                e.getId(),
                employee.getId(),
                employee.getFullName(),
                employee.getCompetency() != null ? employee.getCompetency().getId() : null,
                SessionStatus.fromValue(e.getStatus()),
                e.getCurrentQuestionId(),
                e.getCreatedAt());
    }
}