package com.assessment.assessment.application;

import com.assessment.assessment.domain.AssessmentSession;
import com.assessment.assessment.domain.Attempt;
import com.assessment.assessment.domain.SessionStatus;
import com.assessment.assessment.domain.TopicInfo;
import com.assessment.assessment.port.out.AttemptRepositoryPort;
import com.assessment.assessment.port.out.SessionRepositoryPort;
import com.assessment.assessment.port.out.TopicQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetQuestionUseCase: получение текущего вопроса сессии")
class GetQuestionUseCaseImplTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID TOPIC_ID = UUID.randomUUID();

    @Mock
    private SessionRepositoryPort sessionRepositoryPort;

    @Mock
    private AttemptRepositoryPort attemptRepositoryPort;

    @Mock
    private TopicQueryPort topicQueryPort;

    @Mock
    private SessionQuestionPicker questionPicker;

    @Captor
    private ArgumentCaptor<List<TopicInfo>> topicsCaptor;

    private GetQuestionUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetQuestionUseCaseImpl(sessionRepositoryPort, attemptRepositoryPort,
                topicQueryPort, questionPicker);
    }

    @Test
    @DisplayName("Завершённая сессия сразу возвращает Completed без обращения к попыткам")
    void completedSessionReturnsCompleted() {
        when(sessionRepositoryPort.findById(SESSION_ID))
                .thenReturn(Optional.of(session(SessionStatus.COMPLETED, null, null)));

        QuestionOutcome outcome = useCase.getCurrentQuestion(SESSION_ID);

        assertInstanceOf(QuestionOutcome.Completed.class, outcome);
        verifyNoInteractions(attemptRepositoryPort, topicQueryPort, questionPicker);
    }

    @Test
    @DisplayName("Отсутствующая сессия приводит к NoSuchElementException")
    void missingSessionThrows() {
        when(sessionRepositoryPort.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> useCase.getCurrentQuestion(SESSION_ID));
    }

    @Test
    @DisplayName("Назначенный текущий вопрос возвращается из репозитория без создания нового")
    void assignedCurrentQuestionIsReturned() {
        UUID attemptId = UUID.randomUUID();
        when(sessionRepositoryPort.findById(SESSION_ID))
                .thenReturn(Optional.of(session(SessionStatus.ACTIVE, attemptId, null)));
        Attempt attempt = attempt(attemptId, TOPIC_ID, "Текущий вопрос");
        when(attemptRepositoryPort.findById(attemptId)).thenReturn(Optional.of(attempt));

        QuestionOutcome outcome = useCase.getCurrentQuestion(SESSION_ID);

        QuestionOutcome.Question question = assertInstanceOf(QuestionOutcome.Question.class, outcome);
        assertEquals(attemptId, question.attempt().getId());
        assertEquals("Текущий вопрос", question.attempt().getQuestionText());
        verify(attemptRepositoryPort, never()).save(any());
        verifyNoInteractions(topicQueryPort, questionPicker);
    }

    @Test
    @DisplayName("Назначенный, но отсутствующий в репозитории вопрос приводит к выбору нового")
    void missingAssignedQuestionTriggersPickOfNewQuestion() {
        UUID staleAttemptId = UUID.randomUUID();
        UUID newAttemptId = UUID.randomUUID();
        when(sessionRepositoryPort.findById(SESSION_ID))
                .thenReturn(Optional.of(session(SessionStatus.ACTIVE, staleAttemptId, null)));
        when(attemptRepositoryPort.findById(staleAttemptId)).thenReturn(Optional.empty());
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID)).thenReturn(List.of());
        when(topicQueryPort.findAll()).thenReturn(List.of(topicInfo(TOPIC_ID, null)));
        when(questionPicker.findNextTopicId(anyList(), anyList())).thenReturn(TOPIC_ID);
        when(questionPicker.pickQuestion(eq(TOPIC_ID), anyList())).thenReturn("Новый вопрос");
        when(attemptRepositoryPort.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), newAttemptId));

        QuestionOutcome outcome = useCase.getCurrentQuestion(SESSION_ID);

        QuestionOutcome.Question question = assertInstanceOf(QuestionOutcome.Question.class, outcome);
        assertEquals(newAttemptId, question.attempt().getId());
        assertEquals("Новый вопрос", question.attempt().getQuestionText());
    }

    @Test
    @DisplayName("Отсутствие следующей темы возвращает Completed без создания попытки")
    void noNextTopicReturnsCompleted() {
        when(sessionRepositoryPort.findById(SESSION_ID))
                .thenReturn(Optional.of(session(SessionStatus.ACTIVE, null, null)));
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID)).thenReturn(List.of());
        when(topicQueryPort.findAll()).thenReturn(List.of(topicInfo(TOPIC_ID, null)));
        when(questionPicker.findNextTopicId(anyList(), anyList())).thenReturn(null);

        QuestionOutcome outcome = useCase.getCurrentQuestion(SESSION_ID);

        assertInstanceOf(QuestionOutcome.Completed.class, outcome);
        verify(attemptRepositoryPort, never()).save(any());
        verify(sessionRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Новый вопрос создаётся для следующей темы и назначается текущим в сессии")
    void nextTopicQuestionIsCreatedAndAssignedToSession() {
        UUID newAttemptId = UUID.randomUUID();
        when(sessionRepositoryPort.findById(SESSION_ID))
                .thenReturn(Optional.of(session(SessionStatus.ACTIVE, null, null)));
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID)).thenReturn(List.of());
        when(topicQueryPort.findAll()).thenReturn(List.of(topicInfo(TOPIC_ID, null)));
        when(questionPicker.findNextTopicId(anyList(), anyList())).thenReturn(TOPIC_ID);
        when(questionPicker.pickQuestion(eq(TOPIC_ID), anyList())).thenReturn("Вопрос из банка");
        when(attemptRepositoryPort.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), newAttemptId));

        QuestionOutcome outcome = useCase.getCurrentQuestion(SESSION_ID);

        QuestionOutcome.Question question = assertInstanceOf(QuestionOutcome.Question.class, outcome);
        assertEquals(newAttemptId, question.attempt().getId());
        assertEquals("Вопрос из банка", question.attempt().getQuestionText());
        assertEquals(TOPIC_ID, question.attempt().getTopicId());
        assertEquals(Integer.valueOf(0), question.attempt().getFollowupDepth());
        assertEquals(SESSION_ID, question.attempt().getSessionId());
        verify(sessionRepositoryPort).save(argThat(s -> newIdEquals(s, newAttemptId)));
    }

    @Test
    @DisplayName("Темы фильтруются по компетенции сессии перед выбором следующей")
    void topicsAreFilteredBySessionCompetency() {
        UUID competencyId = UUID.randomUUID();
        UUID otherCompetencyId = UUID.randomUUID();
        TopicInfo matching = topicInfo(TOPIC_ID, competencyId);
        TopicInfo other = topicInfo(UUID.randomUUID(), otherCompetencyId);
        when(sessionRepositoryPort.findById(SESSION_ID))
                .thenReturn(Optional.of(session(SessionStatus.ACTIVE, null, competencyId)));
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID)).thenReturn(List.of());
        when(topicQueryPort.findAll()).thenReturn(List.of(matching, other));
        when(questionPicker.findNextTopicId(anyList(), anyList())).thenReturn(null);

        QuestionOutcome outcome = useCase.getCurrentQuestion(SESSION_ID);

        assertInstanceOf(QuestionOutcome.Completed.class, outcome);
        verify(questionPicker).findNextTopicId(anyList(), topicsCaptor.capture());
        assertEquals(List.of(matching), topicsCaptor.getValue());
    }

    private AssessmentSession session(SessionStatus status, UUID currentQuestionId, UUID competencyId) {
        return AssessmentSession.of(SESSION_ID, UUID.randomUUID(), "Иванов Иван", competencyId,
                status, currentQuestionId, Instant.now());
    }

    private static TopicInfo topicInfo(UUID topicId, UUID competencyId) {
        return TopicInfo.of(topicId, "Тема", "Раздел", competencyId, "Компетенция");
    }

    private static Attempt attempt(UUID id, UUID topicId, String questionText) {
        return Attempt.of(id, SESSION_ID, questionText, null, null, null, null, null, null,
                0, null, topicId, null, null, null, Instant.now());
    }

    private static Attempt withId(Attempt a, UUID id) {
        return Attempt.of(id, a.getSessionId(), a.getQuestionText(), a.getFinalTranscript(), a.getScore(),
                a.getBaseScore(), a.getConfidence(), a.getValidJudge(), a.getFeedback(), a.getFollowupDepth(),
                a.getFollowupParentId(), a.getTopicId(), a.getTopicName(), a.getSectionName(),
                a.getCompetencyName(), a.getCreatedAt());
    }

    private static boolean newIdEquals(AssessmentSession s, UUID expectedId) {
        return expectedId.equals(s.getCurrentQuestionId());
    }
}
