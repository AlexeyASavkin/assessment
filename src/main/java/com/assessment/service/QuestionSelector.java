package com.assessment.service;

import com.assessment.entity.*;
import com.assessment.repository.CriteriaLevelRepository;
import com.assessment.repository.CriteriaRepository;
import com.assessment.repository.QuestionAttemptRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class QuestionSelector {

    private final ChatClient chatClient;
    private final CriteriaRepository criteriaRepository;
    private final CriteriaLevelRepository criteriaLevelRepository;
    private final QuestionAttemptRepository questionAttemptRepository;
    private final int maxFollowupsPerMain;

    public QuestionSelector(
            ObjectProvider<ChatClient> chatClientProvider,
            CriteriaRepository criteriaRepository,
            CriteriaLevelRepository criteriaLevelRepository,
            QuestionAttemptRepository questionAttemptRepository,
            @Value("${assessment.question.max-followups-per-main}") int maxFollowupsPerMain) {
        this.chatClient = chatClientProvider.getIfAvailable();
        this.criteriaRepository = criteriaRepository;
        this.criteriaLevelRepository = criteriaLevelRepository;
        this.questionAttemptRepository = questionAttemptRepository;
        this.maxFollowupsPerMain = maxFollowupsPerMain;
    }

    public String generateQuestion(Session session, UUID criteriaId) {
        Criteria criteria = criteriaRepository.findById(criteriaId).orElseThrow();
        List<CriteriaLevel> levels = criteriaLevelRepository.findByCriteriaId(criteriaId);

        String levelRequirements = levels.stream()
                .map(l -> l.getLevel() + ": " + l.getRequirements())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("Нет требований");

        String prompt = String.format("""
                Ты — эксперт по оценке компетенций. Сгенерируй вопрос для сотрудника.

                Компетенция: %s
                Критерий: %s
                Уровни требований:
                %s

                Сгенерируй один вопрос на русском языке для оценки этого критерия.
                Вопрос должен быть конкретным и позволять оценить уровень сотрудника.
                Верни ТОЛЬКО текст вопроса без лишних объяснений.
                """,
                criteria.getCompetency().getName(),
                criteria.getName(),
                levelRequirements);

        if (chatClient == null) {
            throw new IllegalStateException("ChatClient не настроен. Укажите GEMINI_API_KEY для генерации вопросов.");
        }
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    public String generateFollowUp(Session session, QuestionAttempt previousAttempt) {
        String prompt = String.format("""
                Сотрудник ответил на вопрос, но ответ требует уточнения.
                Предыдущий вопрос: %s
                Ответ сотрудника: %s

                Задай один уточняющий вопрос на русском языке.
                Верни ТОЛЬКО текст вопроса.
                """,
                previousAttempt.getQuestionText(),
                previousAttempt.getFinalTranscript());

        if (chatClient == null) {
            throw new IllegalStateException("ChatClient не настроен. Укажите GEMINI_API_KEY для генерации вопросов.");
        }
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    public boolean shouldAskFollowUp(Session session, UUID criteriaId) {
        List<QuestionAttempt> attempts = questionAttemptRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());

        long followUpCount = attempts.stream()
                .filter(a -> a.getFollowupDepth() > 0)
                .filter(a -> a.getCriteria() != null && a.getCriteria().getId().equals(criteriaId))
                .count();

        QuestionAttempt lastAttempt = attempts.isEmpty() ? null : attempts.get(attempts.size() - 1);
        if (lastAttempt != null && lastAttempt.getFollowupDepth() == 0) {
            return followUpCount < maxFollowupsPerMain;
        }

        return false;
    }
}
