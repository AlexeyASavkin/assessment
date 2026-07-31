package com.assessment.management.application;

import com.assessment.entity.Section;
import com.assessment.entity.Topic;
import com.assessment.management.port.out.SectionRepositoryPort;
import com.assessment.management.port.out.TopicRepositoryPort;
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
@DisplayName("TopicCrudUseCase: CRUD тем разделов")
class TopicCrudUseCaseImplTest {

    @Mock
    private SectionRepositoryPort sectionRepositoryPort;

    @Mock
    private TopicRepositoryPort topicRepositoryPort;

    private TopicCrudUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new TopicCrudUseCaseImpl(sectionRepositoryPort, topicRepositoryPort);
    }

    @Test
    @DisplayName("createTopic привязывает тему к найденному разделу и сохраняет")
    void createTopicSuccess() {
        UUID sectionId = UUID.randomUUID();
        Section section = Section.builder().id(sectionId).name("Основы").build();
        Topic topic = Topic.builder().name("Stream API").build();
        when(sectionRepositoryPort.findById(sectionId)).thenReturn(Optional.of(section));
        when(topicRepositoryPort.save(topic)).thenReturn(topic);

        Optional<Topic> created = useCase.createTopic(sectionId, topic);

        assertTrue(created.isPresent());
        assertSame(section, created.get().getSection());
        verify(topicRepositoryPort).save(topic);
    }

    @Test
    @DisplayName("createTopic возвращает пусто, если раздел не найден")
    void createTopicSectionNotFound() {
        UUID sectionId = UUID.randomUUID();
        Topic topic = Topic.builder().name("Stream API").build();
        when(sectionRepositoryPort.findById(sectionId)).thenReturn(Optional.empty());

        Optional<Topic> created = useCase.createTopic(sectionId, topic);

        assertTrue(created.isEmpty());
        verify(topicRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("listTopics делегирует findBySectionId")
    void listTopicsDelegatesToPort() {
        UUID sectionId = UUID.randomUUID();
        List<Topic> topics = List.of(Topic.builder().name("Stream API").build());
        when(topicRepositoryPort.findBySectionId(sectionId)).thenReturn(topics);

        List<Topic> result = useCase.listTopics(sectionId);

        assertSame(topics, result);
        verify(topicRepositoryPort).findBySectionId(sectionId);
    }

    @Test
    @DisplayName("updateTopic применяет мутатор и сохраняет сущность")
    void updateTopicAppliesMutatorAndSaves() {
        UUID id = UUID.randomUUID();
        Topic topic = Topic.builder().id(id).name("Старое название").build();
        when(topicRepositoryPort.findById(id)).thenReturn(Optional.of(topic));
        when(topicRepositoryPort.save(topic)).thenReturn(topic);

        Optional<Topic> updated = useCase.updateTopic(id, t -> {
            t.setName("Новое название");
            return t;
        });

        assertTrue(updated.isPresent());
        assertEquals("Новое название", updated.get().getName());
        verify(topicRepositoryPort).save(topic);
    }

    @Test
    @DisplayName("updateTopic возвращает пусто, если тема не найдена")
    void updateTopicNotFound() {
        UUID id = UUID.randomUUID();
        when(topicRepositoryPort.findById(id)).thenReturn(Optional.empty());

        Optional<Topic> updated = useCase.updateTopic(id, t -> t);

        assertTrue(updated.isEmpty());
        verify(topicRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("deleteTopic делегирует deleteById")
    void deleteTopicDelegatesToPort() {
        UUID id = UUID.randomUUID();

        useCase.deleteTopic(id);

        verify(topicRepositoryPort).deleteById(id);
    }
}
