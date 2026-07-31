package com.assessment.management.adapter.out;

import com.assessment.entity.Section;
import com.assessment.management.port.out.SectionRepositoryPort;
import com.assessment.repository.SectionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-адаптер выходного порта доступа к разделам компетенций в management-контексте.
 *
 * <p>Оборачивает {@link SectionRepository} и делегирует ему вызовы без
 * дополнительной логики (сущность используется как есть).
 */
@Component
public class SectionJpaAdapter implements SectionRepositoryPort {

    private final SectionRepository sectionRepository;

    public SectionJpaAdapter(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    @Override
    public Section save(Section section) {
        return sectionRepository.save(section);
    }

    @Override
    public Optional<Section> findById(UUID id) {
        return sectionRepository.findById(id);
    }

    @Override
    public List<Section> findByCompetencyId(UUID competencyId) {
        return sectionRepository.findByCompetencyId(competencyId);
    }

    @Override
    public void deleteById(UUID id) {
        sectionRepository.deleteById(id);
    }
}
