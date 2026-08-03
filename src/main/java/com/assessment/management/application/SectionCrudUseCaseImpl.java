package com.assessment.management.application;

import com.assessment.entity.Section;
import com.assessment.management.port.out.CompetencyRepositoryPort;
import com.assessment.management.port.out.QuestionAttemptRepositoryPort;
import com.assessment.management.port.out.SectionRepositoryPort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Реализация use case CRUD-операций над разделами.
 *
 * <p>Воспроизводит бизнес-логику {@code AdminController}: при создании раздел
 * привязывается к найденной компетенции (если её нет — возвращается пусто),
 * обновление применяет мутатор к управляемой сущности, удаление — по идентификатору.
 * Зависит от выходных портов {@link CompetencyRepositoryPort} и
 * {@link SectionRepositoryPort}.
 */
@Service
public class SectionCrudUseCaseImpl implements SectionCrudUseCase {

    private final CompetencyRepositoryPort competencyRepositoryPort;
    private final SectionRepositoryPort sectionRepositoryPort;
    private final QuestionAttemptRepositoryPort questionAttemptRepositoryPort;

    public SectionCrudUseCaseImpl(CompetencyRepositoryPort competencyRepositoryPort,
                                  SectionRepositoryPort sectionRepositoryPort,
                                  QuestionAttemptRepositoryPort questionAttemptRepositoryPort) {
        this.competencyRepositoryPort = competencyRepositoryPort;
        this.sectionRepositoryPort = sectionRepositoryPort;
        this.questionAttemptRepositoryPort = questionAttemptRepositoryPort;
    }

    @Override
    @Transactional
    public Optional<Section> createSection(UUID competencyId, Section section) {
        return competencyRepositoryPort.findById(competencyId)
                .map(competency -> {
                    section.setCompetency(competency);
                    return sectionRepositoryPort.save(section);
                });
    }

    @Override
    public List<Section> listSections(UUID competencyId) {
        return sectionRepositoryPort.findByCompetencyId(competencyId);
    }

    @Override
    @Transactional
    public Optional<Section> updateSection(UUID id, UnaryOperator<Section> mutator) {
        return sectionRepositoryPort.findById(id)
                .map(entity -> {
                    mutator.apply(entity);
                    return sectionRepositoryPort.save(entity);
                });
    }

    @Override
    @Transactional
    public void deleteSection(UUID id) {
        // Попытки ответов по темам раздела ссылаются на них через FK fk_attempts_topic —
        // чистим их до удаления самого раздела, иначе БД отклонит удаление.
        questionAttemptRepositoryPort.deleteBySectionId(id);
        sectionRepositoryPort.deleteById(id);
    }
}
