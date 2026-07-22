package com.assessment.service;

import com.assessment.entity.*;
import com.assessment.repository.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class QuestionGeneratorService {

    private final ChatClient chatClient;
    private final QuestionSelector questionSelector;
    private final CompetencyRepository competencyRepository;
    private final TopicRepository topicRepository;
    private final QuestionBankRepository questionBankRepository;

    public QuestionGeneratorService(
            ObjectProvider<ChatClient> chatClientProvider,
            QuestionSelector questionSelector,
            CompetencyRepository competencyRepository,
            TopicRepository topicRepository,
            QuestionBankRepository questionBankRepository) {
        this.chatClient = chatClientProvider.getIfAvailable();
        this.questionSelector = questionSelector;
        this.competencyRepository = competencyRepository;
        this.topicRepository = topicRepository;
        this.questionBankRepository = questionBankRepository;
    }

    public List<QuestionBank> generateAndSave(UUID competencyId, int count, String difficulty) {
        if (chatClient == null) {
            throw new IllegalStateException("ChatClient не настроен. Укажите API ключ для генерации вопросов.");
        }

        Competency competency = competencyRepository.findById(competencyId)
                .orElseThrow(() -> new NoSuchElementException("Компетенция не найдена: " + competencyId));

        List<Topic> topics = topicRepository.findAll().stream()
                .filter(t -> t.getSection().getCompetency().getId().equals(competencyId))
                .toList();

        if (topics.isEmpty()) {
            throw new IllegalStateException("У компетенции нет тем для генерации вопросов.");
        }

        List<QuestionBank> saved = new ArrayList<>();

        for (Topic topic : topics) {
            for (int i = 0; i < count; i++) {
                String questionText = questionSelector.generateQuestion(null, topic.getId());

                QuestionBank question = new QuestionBank();
                question.setCompetency(competency);
                question.setTopic(topic);
                question.setQuestionText(questionText.trim());
                question.setDifficulty(difficulty);

                saved.add(questionBankRepository.save(question));
            }
        }

        return saved;
    }

    public List<QuestionBank> generateAndSaveForTopic(UUID topicId, int count, String difficulty) {
        if (chatClient == null) {
            throw new IllegalStateException("ChatClient не настроен. Укажите API ключ для генерации вопросов.");
        }

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new NoSuchElementException("Тема не найдена: " + topicId));

        Competency competency = topic.getSection().getCompetency();

        List<QuestionBank> saved = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String questionText = questionSelector.generateQuestion(null, topic.getId());

            QuestionBank question = new QuestionBank();
            question.setCompetency(competency);
            question.setTopic(topic);
            question.setQuestionText(questionText.trim());
            question.setDifficulty(difficulty);

            saved.add(questionBankRepository.save(question));
        }

        return saved;
    }
}
