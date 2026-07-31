package com.assessment.management.application;

import com.assessment.entity.Competency;
import com.assessment.entity.Section;
import com.assessment.management.port.out.CompetencyRepositoryPort;
import com.assessment.management.port.out.SectionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SectionCrudUseCase: CRUD разделов компетенций")
class SectionCrudUseCaseImplTest {

    @Mock
    private CompetencyRepositoryPort competencyRepositoryPort;

    @Mock
    private SectionRepositoryPort sectionRepositoryPort;

    private SectionCrudUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SectionCrudUseCaseImpl(competencyRepositoryPort, sectionRepositoryPort);
    }

    @Test
    @DisplayName("createSection привязывает раздел к найденной компетенции и сохраняет")
    void createSectionSuccess() {
        UUID competencyId = UUID.randomUUID();
        Competency competency = Competency.builder().id(competencyId).name("Java").build();
        Section section = Section.builder().name("Основы").build();
        when(competencyRepositoryPort.findById(competencyId)).thenReturn(Optional.of(competency));
        when(sectionRepositoryPort.save(section)).thenReturn(section);

        Optional<Section> created = useCase.createSection(competencyId, section);

        assertTrue(created.isPresent());
        assertSame(competency, created.get().getCompetency());
        verify(sectionRepositoryPort).save(section);
    }

    @Test
    @DisplayName("createSection возвращает пусто, если компетенция не найдена")
    void createSectionCompetencyNotFound() {
        UUID competencyId = UUID.randomUUID();
        Section section = Section.builder().name("Основы").build();
        when(competencyRepositoryPort.findById(competencyId)).thenReturn(Optional.empty());

        Optional<Section> created = useCase.createSection(competencyId, section);

        assertTrue(created.isEmpty());
        verify(sectionRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("listSections делегирует findByCompetencyId")
    void listSectionsDelegatesToPort() {
        UUID competencyId = UUID.randomUUID();
        List<Section> sections = List.of(Section.builder().name("Основы").build());
        when(sectionRepositoryPort.findByCompetencyId(competencyId)).thenReturn(sections);

        List<Section> result = useCase.listSections(competencyId);

        assertSame(sections, result);
        verify(sectionRepositoryPort).findByCompetencyId(competencyId);
    }

    @Test
    @DisplayName("updateSection применяет мутатор и сохраняет сущность")
    void updateSectionAppliesMutatorAndSaves() {
        UUID id = UUID.randomUUID();
        Section section = Section.builder().id(id).name("Старое название").build();
        when(sectionRepositoryPort.findById(id)).thenReturn(Optional.of(section));
        when(sectionRepositoryPort.save(section)).thenReturn(section);

        Optional<Section> updated = useCase.updateSection(id, s -> {
            s.setName("Новое название");
            return s;
        });

        assertTrue(updated.isPresent());
        assertEquals("Новое название", updated.get().getName());
        verify(sectionRepositoryPort).save(section);
    }

    @Test
    @DisplayName("updateSection возвращает пусто, если раздел не найден")
    void updateSectionNotFound() {
        UUID id = UUID.randomUUID();
        when(sectionRepositoryPort.findById(id)).thenReturn(Optional.empty());

        Optional<Section> updated = useCase.updateSection(id, s -> s);

        assertTrue(updated.isEmpty());
        verify(sectionRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("deleteSection делегирует deleteById")
    void deleteSectionDelegatesToPort() {
        UUID id = UUID.randomUUID();

        useCase.deleteSection(id);

        verify(sectionRepositoryPort).deleteById(id);
    }
}
