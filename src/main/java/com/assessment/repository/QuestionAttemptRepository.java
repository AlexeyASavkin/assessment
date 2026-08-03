package com.assessment.repository;

import com.assessment.entity.QuestionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Удаляет все попытки ответов, связанные с указанной темой.
     *
     * <p>Выполняется одним bulk-запросом: попытки имеют ссылку сами на себя
     * ({@code followup_parent_id}), поэтому поштучное удаление через JPA
     * нарушило бы ограничение внешнего ключа {@code fk_attempts_parent}.
     *
     * @param topicId идентификатор темы
     */
    @Modifying
    @Query("delete from QuestionAttempt a where a.topic.id = :topicId")
    void deleteByTopicId(@Param("topicId") UUID topicId);

    /**
     * Удаляет все попытки ответов по темам указанного раздела.
     *
     * <p>Вызывается при удалении раздела, чтобы не упереться в ограничение
     * внешнего ключа {@code fk_attempts_topic}.
     *
     * @param sectionId идентификатор раздела
     */
    @Modifying
    @Query("delete from QuestionAttempt a where a.topic.section.id = :sectionId")
    void deleteBySectionId(@Param("sectionId") UUID sectionId);

    /**
     * Удаляет все попытки ответов по темам всех разделов указанной компетенции.
     *
     * <p>Вызывается при удалении компетенции, чтобы не упереться в ограничение
     * внешнего ключа {@code fk_attempts_topic}.
     *
     * @param competencyId идентификатор компетенции
     */
    @Modifying
    @Query("delete from QuestionAttempt a where a.topic.section.competency.id = :competencyId")
    void deleteByCompetencyId(@Param("competencyId") UUID competencyId);
}
