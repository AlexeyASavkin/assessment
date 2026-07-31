package com.assessment.management.application;

import com.assessment.entity.QuestionBank;
import com.assessment.management.port.out.QuestionBankRepositoryPort;
import com.assessment.service.QuestionGeneratorService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Реализация use case управления банком вопросов.
 *
 * <p>Воспроизводит бизнес-логику {@code AdminController}: генерация вопросов
 * делегируется {@link QuestionGeneratorService} (LLM-вызовы выполняются без
 * открытой транзакции БД), переупорядочивание проставляет порядок сортировки
 * согласно переданному списку идентификаторов, обновление текста вопроса
 * сохраняет обрезанный ({@code trim}) текст. Зависит от выходного порта
 * {@link QuestionBankRepositoryPort} и {@link QuestionGeneratorService}.
 */
@Service
public class QuestionBankManagementUseCaseImpl implements QuestionBankManagementUseCase {

    private final QuestionBankRepositoryPort questionBankRepositoryPort;
    private final QuestionGeneratorService questionGeneratorService;

    public QuestionBankManagementUseCaseImpl(QuestionBankRepositoryPort questionBankRepositoryPort,
                                             QuestionGeneratorService questionGeneratorService) {
        this.questionBankRepositoryPort = questionBankRepositoryPort;
        this.questionGeneratorService = questionGeneratorService;
    }

    @Override
    public List<QuestionBank> generateForTopic(UUID topicId, int count, String difficulty) {
        return questionGeneratorService.generateAndSaveForTopic(topicId, count, difficulty);
    }

    @Override
    public List<QuestionBank> generateForCompetency(UUID competencyId, int count, String difficulty) {
        return questionGeneratorService.generateAndSave(competencyId, count, difficulty);
    }

    @Override
    public List<QuestionBank> listByTopic(UUID topicId) {
        return questionBankRepositoryPort.findByTopicIdOrderBySortOrderAsc(topicId);
    }

    @Override
    public List<QuestionBank> listByCompetency(UUID competencyId) {
        return questionBankRepositoryPort.findByCompetencyIdOrderByCreatedAtDesc(competencyId);
    }

    @Override
    @Transactional
    public void reorder(UUID topicId, List<UUID> orderedIds) {
        List<QuestionBank> questions = questionBankRepositoryPort.findByTopicIdOrderBySortOrderAsc(topicId);
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            int sortOrder = i;
            questions.stream()
                    .filter(q -> q.getId().equals(id))
                    .findFirst()
                    .ifPresent(q -> q.setSortOrder(sortOrder));
        }
        questionBankRepositoryPort.saveAll(questions);
    }

    @Override
    public Optional<QuestionBank> updateQuestion(UUID id, String questionText) {
        return questionBankRepositoryPort.findById(id)
                .map(question -> {
                    question.setQuestionText(questionText.trim());
                    return questionBankRepositoryPort.save(question);
                });
    }

    @Override
    public void deleteQuestion(UUID id) {
        questionBankRepositoryPort.deleteById(id);
    }
}
