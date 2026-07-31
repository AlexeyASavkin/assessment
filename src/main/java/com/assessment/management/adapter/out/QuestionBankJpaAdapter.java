package com.assessment.management.adapter.out;

import com.assessment.entity.QuestionBank;
import com.assessment.management.port.out.QuestionBankRepositoryPort;
import com.assessment.repository.QuestionBankRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-адаптер выходного порта доступа к банку вопросов в management-контексте.
 *
 * <p>Оборачивает {@link QuestionBankRepository} и делегирует ему вызовы без
 * дополнительной логики (сущность используется как есть).
 */
@Component
public class QuestionBankJpaAdapter implements QuestionBankRepositoryPort {

    private final QuestionBankRepository questionBankRepository;

    public QuestionBankJpaAdapter(QuestionBankRepository questionBankRepository) {
        this.questionBankRepository = questionBankRepository;
    }

    @Override
    public QuestionBank save(QuestionBank question) {
        return questionBankRepository.save(question);
    }

    @Override
    public List<QuestionBank> saveAll(List<QuestionBank> questions) {
        return questionBankRepository.saveAll(questions);
    }

    @Override
    public Optional<QuestionBank> findById(UUID id) {
        return questionBankRepository.findById(id);
    }

    @Override
    public List<QuestionBank> findByTopicIdOrderBySortOrderAsc(UUID topicId) {
        return questionBankRepository.findByTopicIdOrderBySortOrderAsc(topicId);
    }

    @Override
    public List<QuestionBank> findByCompetencyIdOrderByCreatedAtDesc(UUID competencyId) {
        return questionBankRepository.findByCompetencyIdOrderByCreatedAtDesc(competencyId);
    }

    @Override
    public void deleteById(UUID id) {
        questionBankRepository.deleteById(id);
    }
}
