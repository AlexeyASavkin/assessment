package com.assessment.assessment.adapter.out;

import com.assessment.assessment.domain.QuestionBankQuestion;
import com.assessment.assessment.port.out.QuestionBankRepositoryPort;
import com.assessment.entity.QuestionBank;
import com.assessment.repository.QuestionBankRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * JPA-адаптер выходного порта доступа к банку вопросов.
 *
 * <p>Оборачивает {@link QuestionBankRepository} и преобразует сущность
 * {@link QuestionBank} в доменную модель {@link QuestionBankQuestion}.
 * Чтение ленивой ассоциации topic выполняется в транзакции.
 */
@Component
public class JpaQuestionBankRepositoryAdapter implements QuestionBankRepositoryPort {

    private final QuestionBankRepository questionBankRepository;

    public JpaQuestionBankRepositoryAdapter(QuestionBankRepository questionBankRepository) {
        this.questionBankRepository = questionBankRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionBankQuestion> findByTopicIdOrderBySortOrderAsc(UUID topicId) {
        return questionBankRepository.findByTopicIdOrderBySortOrderAsc(topicId).stream()
                .map(this::toDomain)
                .toList();
    }

    private QuestionBankQuestion toDomain(QuestionBank q) {
        return QuestionBankQuestion.of(
                q.getId(),
                q.getTopic() != null ? q.getTopic().getId() : null,
                q.getQuestionText());
    }
}