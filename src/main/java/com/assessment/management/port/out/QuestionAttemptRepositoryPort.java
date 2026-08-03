package com.assessment.management.port.out;

import java.util.UUID;

/**
 * Выходной порт удаления попыток ответов в management-контексте.
 *
 * <p>Нужен use case'ам CRUD компетенций/разделов/тем: при удалении
 * компетенции, раздела или темы сначала удаляются попытки ответов,
 * связанные с их темами, иначе удаление упирается в ограничение
 * внешнего ключа {@code fk_attempts_topic}.
 *
 * <p>Реализуется JPA-адаптером поверх Spring Data репозитория
 * {@code QuestionAttemptRepository}.
 */
public interface QuestionAttemptRepositoryPort {

    /**
     * Удаляет все попытки ответов, связанные с указанной темой.
     *
     * @param topicId идентификатор темы
     */
    void deleteByTopicId(UUID topicId);

    /**
     * Удаляет все попытки ответов по темам указанного раздела.
     *
     * @param sectionId идентификатор раздела
     */
    void deleteBySectionId(UUID sectionId);

    /**
     * Удаляет все попытки ответов по темам всех разделов указанной компетенции.
     *
     * @param competencyId идентификатор компетенции
     */
    void deleteByCompetencyId(UUID competencyId);
}
