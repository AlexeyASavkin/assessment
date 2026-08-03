package com.assessment.management.port.out;

import com.assessment.entity.QuestionBank;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Выходной порт доступа к сущности {@link QuestionBank} в management-контексте.
 *
 * <p>Реализуется JPA-адаптером поверх Spring Data репозитория.
 * Сущность используется как есть (pure-CRUD, без отдельной доменной модели).
 */
public interface QuestionBankRepositoryPort {

    /**
     * Сохраняет (создаёт или обновляет) вопрос банка.
     *
     * @param question вопрос
     * @return сохранённый вопрос
     */
    QuestionBank save(QuestionBank question);

    /**
     * Сохраняет список вопросов банка.
     *
     * @param questions список вопросов
     * @return сохранённые вопросы
     */
    List<QuestionBank> saveAll(List<QuestionBank> questions);

    /**
     * Возвращает вопрос банка по идентификатору.
     *
     * @param id идентификатор вопроса
     * @return вопрос или пусто, если не найден
     */
    Optional<QuestionBank> findById(UUID id);

    /**
     * Возвращает вопросы указанной темы, отсортированные по порядку сортировки.
     *
     * @param topicId идентификатор темы
     * @return список вопросов
     */
    List<QuestionBank> findByTopicIdOrderBySortOrderAsc(UUID topicId);

    /**
     * Возвращает вопросы указанной компетенции, отсортированные по дате создания (новые первыми).
     *
     * @param competencyId идентификатор компетенции
     * @return список вопросов
     */
    List<QuestionBank> findByCompetencyIdOrderByCreatedAtDesc(UUID competencyId);

    /**
     * Удаляет вопрос банка по идентификатору.
     *
     * @param id идентификатор вопроса
     */
    void deleteById(UUID id);
}
