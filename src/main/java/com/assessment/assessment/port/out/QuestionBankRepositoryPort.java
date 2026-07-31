package com.assessment.assessment.port.out;

import com.assessment.assessment.domain.QuestionBankQuestion;

import java.util.List;
import java.util.UUID;

/**
 * Выходной порт доступа к банку вопросов.
 *
 * <p>Реализуется JPA-адаптером поверх {@code QuestionBankRepository}.
 */
public interface QuestionBankRepositoryPort {

    /**
     * Возвращает вопросы банка для темы, отсортированные по порядку.
     *
     * @param topicId идентификатор темы
     * @return список вопросов банка
     */
    List<QuestionBankQuestion> findByTopicIdOrderBySortOrderAsc(UUID topicId);
}
