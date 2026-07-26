package com.assessment.repository;

import com.assessment.entity.QuestionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для управления попытками ответов на вопросы в рамках сессий оценки.
 */
public interface QuestionAttemptRepository extends JpaRepository<QuestionAttempt, UUID> {

    /**
     * Возвращает список попыток ответов для указанной сессии, отсортированных по дате создания.
     *
     * @param sessionId идентификатор сессии
     * @return список попыток ответов
     */
    List<QuestionAttempt> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    /**
     * Находит все попытки ответов, связанные с указанной темой.
     *
     * @param topicId идентификатор темы
     * @return список попыток ответов
     */
    List<QuestionAttempt> findByTopicId(UUID topicId);
}
