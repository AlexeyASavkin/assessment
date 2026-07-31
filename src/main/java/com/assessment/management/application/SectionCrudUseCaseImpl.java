package com.assessment.management.application;

import com.assessment.entity.Section;
import com.assessment.management.port.out.CompetencyRepositoryPort;
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

    public SectionCrudUseCaseImpl(CompetencyRepositoryPort competencyRepositoryPort,
                                  SectionRepositoryPort sectionRepositoryPort) {
        this.competencyRepositoryPort = competencyRepositoryPort;
        this.sectionRepositoryPort = sectionRepositoryPort;
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
    public void deleteSection(UUID id) {
        sectionRepositoryPort.deleteById(id);
    }
}
