package com.assessment.service;

import com.assessment.entity.*;
import com.assessment.repository.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Сервис генерации вопросов для банка вопросов с помощью LLM.
 * Создает вопросы по компетенциям и темам для последующего использования в оценке.
 */
@Service
public class QuestionGeneratorService {

    private final QuestionSelector questionSelector;
    private final CompetencyRepository competencyRepository;
    private final TopicRepository topicRepository;
    private final QuestionBankRepository questionBankRepository;

    /**
     * Конструктор сервиса генерации вопросов.
     *
     * @param questionSelector         сервис выбора вопросов
     * @param competencyRepository     репозиторий компетенций
     * @param topicRepository          репозиторий тем
     * @param questionBankRepository   репозиторий банка вопросов
     */
    public QuestionGeneratorService(
            QuestionSelector questionSelector,
            CompetencyRepository competencyRepository,
            TopicRepository topicRepository,
            QuestionBankRepository questionBankRepository) {
        this.questionSelector = questionSelector;
        this.competencyRepository = competencyRepository;
        this.topicRepository = topicRepository;
        this.questionBankRepository = questionBankRepository;
    }

    /**
     * Генерирует и сохраняет вопросы для всех тем указанной компетенции.
     *
     * @param competencyId  идентификатор компетенции
     * @param count         количество вопросов на каждую тему
     * @param difficulty    уровень сложности вопросов
     * @return список сохраненных вопросов
     * @throws NoSuchElementException   если компетенция не найдена
     */
    public List<QuestionBank> generateAndSave(UUID competencyId, int count, String difficulty) {
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

    /**
     * Генерирует и сохраняет вопросы для конкретной темы.
     *
     * @param topicId     идентификатор темы
     * @param count       количество вопросов для генерации
     * @param difficulty  уровень сложности вопросов
     * @return список сохраненных вопросов
     * @throws NoSuchElementException   если тема не найдена
     */
    public List<QuestionBank> generateAndSaveForTopic(UUID topicId, int count, String difficulty) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new NoSuchElementException("Тема не найдена: " + topicId));

        Competency competency = topic.getSection().getCompetency();

        int startOrder = questionBankRepository.findByTopicIdOrderBySortOrderAsc(topicId).size();

        List<QuestionBank> saved = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String questionText = questionSelector.generateQuestion(null, topic.getId());

            QuestionBank question = new QuestionBank();
            question.setCompetency(competency);
            question.setTopic(topic);
            question.setQuestionText(questionText.trim());
            question.setDifficulty(difficulty);
            question.setSortOrder(startOrder + i);

            saved.add(questionBankRepository.save(question));
        }

        return saved;
    }
}
