package com.assessment.management.adapter.out;

import com.assessment.entity.Session;
import com.assessment.management.port.out.SessionRepositoryPort;
import com.assessment.repository.SessionRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * JPA-адаптер выходного порта доступа к сессиям оценки в management-контексте.
 *
 * <p>Оборачивает {@link SessionRepository} и делегирует ему вызовы без
 * дополнительной логики (сущность используется как есть). Используется
 * management-контекстом для проверки существования сессии при формировании
 * админского отчёта.
 */
@Component
public class SessionJpaAdapter implements SessionRepositoryPort {

    private final SessionRepository sessionRepository;

    public SessionJpaAdapter(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public boolean existsById(UUID id) {
        return sessionRepository.existsById(id);
    }
}
