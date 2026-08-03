package com.assessment.management.application;

import com.assessment.entity.Competency;
import com.assessment.management.port.out.CompetencyRepositoryPort;
import com.assessment.management.port.out.QuestionAttemptRepositoryPort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Реализация use case CRUD-операций над компетенциями.
 *
 * <p>Воспроизводит бизнес-логику {@code AdminController}: создание, выборка,
 * обновление через мутатор на управляемой сущности и удаление по идентификатору.
 * Зависит только от выходного порта {@link CompetencyRepositoryPort}.
 */
@Service
public class CompetencyCrudUseCaseImpl implements CompetencyCrudUseCase {

    private final CompetencyRepositoryPort competencyRepositoryPort;
    private final QuestionAttemptRepositoryPort questionAttemptRepositoryPort;

    public CompetencyCrudUseCaseImpl(CompetencyRepositoryPort competencyRepositoryPort,
                                     QuestionAttemptRepositoryPort questionAttemptRepositoryPort) {
        this.competencyRepositoryPort = competencyRepositoryPort;
        this.questionAttemptRepositoryPort = questionAttemptRepositoryPort;
    }

    @Override
    public Competency createCompetency(Competency competency) {
        return competencyRepositoryPort.save(competency);
    }

    @Override
    public List<Competency> listCompetencies() {
        return competencyRepositoryPort.findAll();
    }

    @Override
    public Optional<Competency> getCompetency(UUID id) {
        return competencyRepositoryPort.findById(id);
    }

    @Override
    @Transactional
    public Optional<Competency> updateCompetency(UUID id, UnaryOperator<Competency> mutator) {
        return competencyRepositoryPort.findById(id)
                .map(entity -> {
                    mutator.apply(entity);
                    return competencyRepositoryPort.save(entity);
                });
    }

    @Override
    @Transactional
    public void deleteCompetency(UUID id) {
        // Попытки ответов по темам компетенции ссылаются на них через FK fk_attempts_topic —
        // чистим их до удаления самой компетенции, иначе БД отклонит удаление.
        questionAttemptRepositoryPort.deleteByCompetencyId(id);
        competencyRepositoryPort.deleteById(id);
    }
}
