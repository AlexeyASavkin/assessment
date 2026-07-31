package com.assessment.ai.port;

import com.assessment.ai.domain.FollowUpResult;
import com.assessment.ai.domain.ScoreResult;

import java.util.Optional;

/**
 * Порт уточняющих вопросов через LLM (выходной порт).
 *
 * <p>Используется use case'ами уточняющих вопросов для генерации уточнения
 * после слабого ответа (оценка ≤ 2) и переоценки основной попытки с учётом
 * ответа на уточнение.
 *
 * <p>Оба метода возвращают {@link Optional#empty()} при сбое LLM или
 * невозможности распарсить ответ — сессия продолжается без зависания.
 */
public interface LlmFollowUpPort {

    /**
     * Генерирует уточняющий вопрос на основе исходного вопроса и слабого ответа.
     *
     * @param questionText исходный вопрос
     * @param answerText   слабый ответ сотрудника
     * @return уточняющий вопрос или {@code empty} при сбое LLM
     */
    Optional<FollowUpResult> generateFollowUpQuestion(String questionText, String answerText);

    /**
     * Переоценивает основной ответ с учётом ответа на уточняющий вопрос.
     *
     * @param questionText          исходный вопрос
     * @param answerText            исходный ответ сотрудника
     * @param followUpQuestionText  уточняющий вопрос
     * @param followUpAnswerText    ответ на уточняющий вопрос
     * @return пересчитанная оценка или {@code empty} при сбое LLM/парсинга
     */
    Optional<ScoreResult> rescoreMainAttempt(String questionText, String answerText,
                                             String followUpQuestionText, String followUpAnswerText);
}
