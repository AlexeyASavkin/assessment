package com.assessment.management.application;

import com.assessment.entity.Competency;
import com.assessment.management.port.out.CompetencyRepositoryPort;
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
@DisplayName("CompetencyCrudUseCase: CRUD компетенций")
class CompetencyCrudUseCaseImplTest {

    @Mock
    private CompetencyRepositoryPort competencyRepositoryPort;

    private CompetencyCrudUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new CompetencyCrudUseCaseImpl(competencyRepositoryPort);
    }

    @Test
    @DisplayName("createCompetency сохраняет компетенцию через порт")
    void createCompetencySaves() {
        Competency competency = Competency.builder().name("Java").build();
        when(competencyRepositoryPort.save(competency)).thenReturn(competency);

        Competency saved = useCase.createCompetency(competency);

        assertSame(competency, saved);
        verify(competencyRepositoryPort).save(competency);
    }

    @Test
    @DisplayName("listCompetencies делегирует findAll")
    void listCompetenciesDelegatesToPort() {
        List<Competency> competencies = List.of(Competency.builder().name("Java").build());
        when(competencyRepositoryPort.findAll()).thenReturn(competencies);

        List<Competency> result = useCase.listCompetencies();

        assertSame(competencies, result);
        verify(competencyRepositoryPort).findAll();
    }

    @Test
    @DisplayName("getCompetency делегирует findById")
    void getCompetencyDelegatesToPort() {
        UUID id = UUID.randomUUID();
        Competency competency = Competency.builder().id(id).name("Java").build();
        when(competencyRepositoryPort.findById(id)).thenReturn(Optional.of(competency));

        Optional<Competency> result = useCase.getCompetency(id);

        assertTrue(result.isPresent());
        assertSame(competency, result.get());
        verify(competencyRepositoryPort).findById(id);
    }

    @Test
    @DisplayName("updateCompetency применяет мутатор и сохраняет сущность")
    void updateCompetencyAppliesMutatorAndSaves() {
        UUID id = UUID.randomUUID();
        Competency competency = Competency.builder().id(id).name("Старое название").build();
        when(competencyRepositoryPort.findById(id)).thenReturn(Optional.of(competency));
        when(competencyRepositoryPort.save(competency)).thenReturn(competency);

        Optional<Competency> updated = useCase.updateCompetency(id, c -> {
            c.setName("Новое название");
            return c;
        });

        assertTrue(updated.isPresent());
        assertEquals("Новое название", updated.get().getName());
        verify(competencyRepositoryPort).save(competency);
    }

    @Test
    @DisplayName("updateCompetency возвращает пусто, если компетенция не найдена")
    void updateCompetencyNotFound() {
        UUID id = UUID.randomUUID();
        when(competencyRepositoryPort.findById(id)).thenReturn(Optional.empty());

        Optional<Competency> updated = useCase.updateCompetency(id, c -> c);

        assertTrue(updated.isEmpty());
        verify(competencyRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("deleteCompetency делегирует deleteById")
    void deleteCompetencyDelegatesToPort() {
        UUID id = UUID.randomUUID();

        useCase.deleteCompetency(id);

        verify(competencyRepositoryPort).deleteById(id);
    }
}
