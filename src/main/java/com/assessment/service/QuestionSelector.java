package com.assessment.service;

import com.assessment.entity.*;
import com.assessment.repository.TopicRepository;
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
    private final TopicRepository topicRepository;
    private final QuestionAttemptRepository questionAttemptRepository;
    private final int maxFollowupsPerMain;

    public QuestionSelector(
            ObjectProvider<ChatClient> chatClientProvider,
            TopicRepository topicRepository,
            QuestionAttemptRepository questionAttemptRepository,
            @Value("${assessment.question.max-followups-per-main}") int maxFollowupsPerMain) {
        this.chatClient = chatClientProvider.getIfAvailable();
        this.topicRepository = topicRepository;
        this.questionAttemptRepository = questionAttemptRepository;
        this.maxFollowupsPerMain = maxFollowupsPerMain;
    }

    public String generateQuestion(Session session, UUID topicId) {
        Topic topic = topicRepository.findById(topicId).orElseThrow();

        String prompt = String.format("""
                Ты — эксперт по оценке компетенций. Сгенерируй вопрос для сотрудника.

                Компетенция: %s
                Тема: %s

                Сгенерируй один вопрос на русском языке для оценки этой темы.
                Вопрос должен быть конкретным и позволять оценить уровень сотрудника.
                Верни ТОЛЬКО текст вопроса без лишних объяснений.
                """,
                topic.getSection().getCompetency().getName(),
                topic.getName());

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

    public boolean shouldAskFollowUp(Session session, UUID topicId) {
        List<QuestionAttempt> attempts = questionAttemptRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());

        long followUpCount = attempts.stream()
                .filter(a -> a.getFollowupDepth() > 0)
                .filter(a -> a.getTopic() != null && a.getTopic().getId().equals(topicId))
                .count();

        QuestionAttempt lastAttempt = attempts.isEmpty() ? null : attempts.get(attempts.size() - 1);
        if (lastAttempt != null && lastAttempt.getFollowupDepth() == 0) {
            return followUpCount < maxFollowupsPerMain;
        }

        return false;
    }
}
