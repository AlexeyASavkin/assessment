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
    private final CriteriaRepository criteriaRepository;
    private final QuestionBankRepository questionBankRepository;

    public QuestionGeneratorService(
            ObjectProvider<ChatClient> chatClientProvider,
            QuestionSelector questionSelector,
            CompetencyRepository competencyRepository,
            CriteriaRepository criteriaRepository,
            QuestionBankRepository questionBankRepository) {
        this.chatClient = chatClientProvider.getIfAvailable();
        this.questionSelector = questionSelector;
        this.competencyRepository = competencyRepository;
        this.criteriaRepository = criteriaRepository;
        this.questionBankRepository = questionBankRepository;
    }

    public List<QuestionBank> generateAndSave(UUID competencyId, int count, String difficulty) {
        if (chatClient == null) {
            throw new IllegalStateException("ChatClient не настроен. Укажите API ключ для генерации вопросов.");
        }

        Competency competency = competencyRepository.findById(competencyId)
                .orElseThrow(() -> new NoSuchElementException("Компетенция не найдена: " + competencyId));

        List<Criteria> criteriaList = criteriaRepository.findByCompetencyId(competencyId);
        if (criteriaList.isEmpty()) {
            throw new IllegalStateException("У компетенции нет критериев для генерации вопросов.");
        }

        List<QuestionBank> saved = new ArrayList<>();

        for (Criteria criteria : criteriaList) {
            for (int i = 0; i < count; i++) {
                String questionText = questionSelector.generateQuestion(null, criteria.getId());

                QuestionBank question = new QuestionBank();
                question.setCompetency(competency);
                question.setCriteria(criteria);
                question.setQuestionText(questionText.trim());
                question.setDifficulty(difficulty);

                saved.add(questionBankRepository.save(question));
            }
        }

        return saved;
    }
}
