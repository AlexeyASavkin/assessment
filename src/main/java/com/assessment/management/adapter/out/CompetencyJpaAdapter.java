package com.assessment.management.adapter.out;

import com.assessment.entity.Competency;
import com.assessment.management.port.out.CompetencyRepositoryPort;
import com.assessment.repository.CompetencyRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-адаптер выходного порта доступа к компетенциям в management-контексте.
 *
 * <p>Оборачивает {@link CompetencyRepository} и делегирует ему вызовы без
 * дополнительной логики (сущность используется как есть).
 */
@Component
public class CompetencyJpaAdapter implements CompetencyRepositoryPort {

    private final CompetencyRepository competencyRepository;

    public CompetencyJpaAdapter(CompetencyRepository competencyRepository) {
        this.competencyRepository = competencyRepository;
    }

    @Override
    public Competency save(Competency competency) {
        return competencyRepository.save(competency);
    }

    @Override
    public List<Competency> findAll() {
        return competencyRepository.findAll();
    }

    @Override
    public Optional<Competency> findById(UUID id) {
        return competencyRepository.findById(id);
    }

    @Override
    public void deleteById(UUID id) {
        competencyRepository.deleteById(id);
    }
}
