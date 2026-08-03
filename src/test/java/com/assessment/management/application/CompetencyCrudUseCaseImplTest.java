package com.assessment.management.application;

import com.assessment.entity.Competency;
import com.assessment.management.port.out.CompetencyRepositoryPort;
import com.assessment.management.port.out.QuestionAttemptRepositoryPort;
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

    @Mock
    private QuestionAttemptRepositoryPort questionAttemptRepositoryPort;

    private CompetencyCrudUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new CompetencyCrudUseCaseImpl(competencyRepositoryPort,
                questionAttemptRepositoryPort);
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
    @DisplayName("deleteCompetency чистит попытки ответов компетенции перед удалением")
    void deleteCompetencyCleansAttemptsThenDeletes() {
        UUID id = UUID.randomUUID();

        useCase.deleteCompetency(id);

        verify(questionAttemptRepositoryPort).deleteByCompetencyId(id);
        verify(competencyRepositoryPort).deleteById(id);
    }
}
