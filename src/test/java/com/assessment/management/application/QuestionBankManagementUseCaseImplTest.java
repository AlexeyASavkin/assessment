package com.assessment.management.application;

import com.assessment.entity.QuestionBank;
import com.assessment.management.port.out.QuestionBankRepositoryPort;
import com.assessment.service.QuestionGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionBankManagementUseCase: генерация, переупорядочивание и редактирование вопросов")
class QuestionBankManagementUseCaseImplTest {

    @Mock
    private QuestionBankRepositoryPort questionBankRepositoryPort;

    @Mock
    private QuestionGeneratorService questionGeneratorService;

    private QuestionBankManagementUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new QuestionBankManagementUseCaseImpl(questionBankRepositoryPort, questionGeneratorService);
    }

    private static QuestionBank question(UUID id, int sortOrder) {
        QuestionBank question = new QuestionBank();
        question.setId(id);
        question.setSortOrder(sortOrder);
        question.setQuestionText("Текст вопроса");
        question.setDifficulty("MIDDLE");
        return question;
    }

    @Test
    @DisplayName("generateForTopic делегирует questionGeneratorService.generateAndSaveForTopic")
    void generateForTopicDelegatesToGenerator() {
        UUID topicId = UUID.randomUUID();
        List<QuestionBank> generated = List.of(question(UUID.randomUUID(), 0));
        when(questionGeneratorService.generateAndSaveForTopic(topicId, 5, "MIDDLE")).thenReturn(generated);

        List<QuestionBank> result = useCase.generateForTopic(topicId, 5, "MIDDLE");

        assertSame(generated, result);
        verify(questionGeneratorService).generateAndSaveForTopic(topicId, 5, "MIDDLE");
    }

    @Test
    @DisplayName("generateForCompetency делегирует questionGeneratorService.generateAndSave")
    void generateForCompetencyDelegatesToGenerator() {
        UUID competencyId = UUID.randomUUID();
        List<QuestionBank> generated = List.of(question(UUID.randomUUID(), 0));
        when(questionGeneratorService.generateAndSave(competencyId, 10, "SENIOR")).thenReturn(generated);

        List<QuestionBank> result = useCase.generateForCompetency(competencyId, 10, "SENIOR");

        assertSame(generated, result);
        verify(questionGeneratorService).generateAndSave(competencyId, 10, "SENIOR");
    }

    @Test
    @DisplayName("listByTopic делегирует findByTopicIdOrderBySortOrderAsc")
    void listByTopicDelegatesToPort() {
        UUID topicId = UUID.randomUUID();
        List<QuestionBank> questions = List.of(question(UUID.randomUUID(), 0));
        when(questionBankRepositoryPort.findByTopicIdOrderBySortOrderAsc(topicId)).thenReturn(questions);

        List<QuestionBank> result = useCase.listByTopic(topicId);

        assertSame(questions, result);
        verify(questionBankRepositoryPort).findByTopicIdOrderBySortOrderAsc(topicId);
    }

    @Test
    @DisplayName("listByCompetency делегирует findByCompetencyIdOrderByCreatedAtDesc")
    void listByCompetencyDelegatesToPort() {
        UUID competencyId = UUID.randomUUID();
        List<QuestionBank> questions = List.of(question(UUID.randomUUID(), 0));
        when(questionBankRepositoryPort.findByCompetencyIdOrderByCreatedAtDesc(competencyId)).thenReturn(questions);

        List<QuestionBank> result = useCase.listByCompetency(competencyId);

        assertSame(questions, result);
        verify(questionBankRepositoryPort).findByCompetencyIdOrderByCreatedAtDesc(competencyId);
    }

    @Test
    @DisplayName("reorder проставляет sortOrder согласно порядку идентификаторов")
    void reorderAssignsSortOrder() {
        UUID topicId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        QuestionBank first = question(id1, 0);
        QuestionBank second = question(id2, 1);
        QuestionBank third = question(id3, 2);
        List<QuestionBank> questions = new ArrayList<>(List.of(first, second, third));
        when(questionBankRepositoryPort.findByTopicIdOrderBySortOrderAsc(topicId)).thenReturn(questions);

        useCase.reorder(topicId, List.of(id3, id1, id2));

        assertEquals(0, third.getSortOrder());
        assertEquals(1, first.getSortOrder());
        assertEquals(2, second.getSortOrder());
        verify(questionBankRepositoryPort).saveAll(questions);
    }

    @Test
    @DisplayName("reorder игнорирует идентификаторы, отсутствующие в теме")
    void reorderIgnoresUnknownIds() {
        UUID topicId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        QuestionBank existing = question(id1, 7);
        List<QuestionBank> questions = new ArrayList<>(List.of(existing));
        when(questionBankRepositoryPort.findByTopicIdOrderBySortOrderAsc(topicId)).thenReturn(questions);

        useCase.reorder(topicId, List.of(UUID.randomUUID(), id1));

        assertEquals(1, existing.getSortOrder());
        verify(questionBankRepositoryPort).saveAll(questions);
    }

    @Test
    @DisplayName("updateQuestion обрезает пробелы в тексте и сохраняет вопрос")
    void updateQuestionTrimsTextAndSaves() {
        UUID id = UUID.randomUUID();
        QuestionBank question = question(id, 0);
        when(questionBankRepositoryPort.findById(id)).thenReturn(Optional.of(question));
        when(questionBankRepositoryPort.save(question)).thenReturn(question);

        Optional<QuestionBank> updated = useCase.updateQuestion(id, "  Новый текст вопроса  ");

        assertTrue(updated.isPresent());
        assertEquals("Новый текст вопроса", updated.get().getQuestionText());
        verify(questionBankRepositoryPort).save(question);
    }

    @Test
    @DisplayName("updateQuestion возвращает пусто, если вопрос не найден")
    void updateQuestionNotFound() {
        UUID id = UUID.randomUUID();
        when(questionBankRepositoryPort.findById(id)).thenReturn(Optional.empty());

        Optional<QuestionBank> updated = useCase.updateQuestion(id, "Текст");

        assertTrue(updated.isEmpty());
        verify(questionBankRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("deleteQuestion делегирует deleteById")
    void deleteQuestionDelegatesToPort() {
        UUID id = UUID.randomUUID();

        useCase.deleteQuestion(id);

        verify(questionBankRepositoryPort).deleteById(id);
    }
}
