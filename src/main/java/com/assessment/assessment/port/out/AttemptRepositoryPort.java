package com.assessment.assessment.port.out;

import com.assessment.assessment.domain.Attempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Выходной порт доступа к попыткам ответов.
 *
 * <p>Реализуется JPA-адаптером поверх {@code QuestionAttemptRepository}.
 */
public interface AttemptRepositoryPort {

    Optional<Attempt> findById(UUID attemptId);

    Attempt save(Attempt attempt);

    List<Attempt> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
