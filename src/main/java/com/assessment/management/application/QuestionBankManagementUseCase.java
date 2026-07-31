package com.assessment.management.application;

import com.assessment.entity.QuestionBank;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Входной порт (use case) управления банком вопросов.
 *
 * <p>Покрывает генерацию вопросов через LLM (по теме или компетенции),
 * выборку, переупорядочивание, обновление текста и удаление вопросов.
 * Работает с JPA-сущностью {@link QuestionBank} как с доменной моделью.
 */
public interface QuestionBankManagementUseCase {

    /**
     * Генерирует и сохраняет вопросы для конкретной темы через LLM.
     *
     * @param topicId    идентификатор темы
     * @param count      количество вопросов (1–10)
     * @param difficulty уровень сложности (ALL, JUNIOR, MIDDLE, SENIOR)
     * @return список сохранённых вопросов
     * @throws java.util.NoSuchElementException если тема не найдена
     * @throws IllegalStateException            при ошибке генерации
     */
    List<QuestionBank> generateForTopic(UUID topicId, int count, String difficulty);

    /**
     * Генерирует и сохраняет вопросы для всех тем компетенции через LLM.
     *
     * @param competencyId идентификатор компетенции
     * @param count        количество вопросов на каждую тему (1–10)
     * @param difficulty   уровень сложности (ALL, JUNIOR, MIDDLE, SENIOR)
     * @return список сохранённых вопросов
     * @throws java.util.NoSuchElementException если компетенция не найдена
     * @throws IllegalStateException            при ошибке генерации
     */
    List<QuestionBank> generateForCompetency(UUID competencyId, int count, String difficulty);

    /**
     * Возвращает вопросы указанной темы, отсортированные по порядку сортировки.
     *
     * @param topicId идентификатор темы
     * @return список вопросов
     */
    List<QuestionBank> listByTopic(UUID topicId);

    /**
     * Возвращает вопросы указанной компетенции, отсортированные по дате создания.
     *
     * @param competencyId идентификатор компетенции
     * @return список вопросов
     */
    List<QuestionBank> listByCompetency(UUID competencyId);

    /**
     * Переупорядочивает вопросы банка для указанной темы.
     *
     * @param topicId    идентификатор темы
     * @param orderedIds список идентификаторов вопросов в новом порядке
     */
    void reorder(UUID topicId, List<UUID> orderedIds);

    /**
     * Обновляет текст вопроса банка.
     *
     * @param id            идентификатор вопроса
     * @param questionText  новый текст вопроса (не пустой)
     * @return обновлённый вопрос или пусто, если не найден
     */
    Optional<QuestionBank> updateQuestion(UUID id, String questionText);

    /**
     * Удаляет вопрос банка по идентификатору.
     *
     * @param id идентификатор вопроса
     */
    void deleteQuestion(UUID id);
}
