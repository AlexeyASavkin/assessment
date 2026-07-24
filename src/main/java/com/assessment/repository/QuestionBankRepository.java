package com.assessment.repository;

import com.assessment.entity.QuestionBank;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для управления банком вопросов, используемых при оценке компетенций.
 */
public interface QuestionBankRepository extends JpaRepository<QuestionBank, UUID> {

    /**
     * Возвращает вопросы для указанной компетенции, отсортированные по дате создания (новые сначала).
     *
     * @param competencyId идентификатор компетенции
     * @return список вопросов
     */
    List<QuestionBank> findByCompetencyIdOrderByCreatedAtDesc(UUID competencyId);

    /**
     * Возвращает вопросы для указанной темы, отсортированные по дате создания (новые сначала).
     *
     * @param topicId идентификатор темы
     * @return список вопросов
     */
    List<QuestionBank> findByTopicIdOrderByCreatedAtDesc(UUID topicId);

    /**
     * Возвращает вопросы для указанной темы, отсортированные по порядку сортировки.
     *
     * @param topicId идентификатор темы
     * @return список вопросов
     */
    List<QuestionBank> findByTopicIdOrderBySortOrderAsc(UUID topicId);
}
