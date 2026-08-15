package com.assessment.management.adapter.in;

import com.assessment.dto.CreateTopicRequestDto;
import com.assessment.dto.TopicDto;
import com.assessment.dto.UpdateTopicRequestDto;
import com.assessment.dto.mapper.TopicMapper;
import com.assessment.management.application.TopicCrudUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Тонкий HTTP-адаптер CRUD-операций над темами
 * (пути {@code /api/admin/sections/{sectionId}/topics} и {@code /api/admin/topics/{id}}).
 * Требует роль ADMIN, делегирует бизнес-логику {@link TopicCrudUseCase}, маппит DTO через {@link TopicMapper}.
 */
@RestController
@RequestMapping("/api/admin")
public class TopicController {

    private final TopicCrudUseCase topicCrud;
    private final TopicMapper topicMapper;

    public TopicController(TopicCrudUseCase topicCrud, TopicMapper topicMapper) {
        this.topicCrud = topicCrud;
        this.topicMapper = topicMapper;
    }

    /**
     * Создает тему внутри указанного раздела.
     *
     * @param sectionId идентификатор раздела
     * @param dto       данные темы из тела запроса
     * @return созданная тема с HTTP 201 или HTTP 404, если раздел не найден
     */
    @PostMapping("/sections/{sectionId}/topics")
    public ResponseEntity<TopicDto> createTopic(@PathVariable UUID sectionId,
                                                 @Valid @RequestBody CreateTopicRequestDto dto) {
        var topic = topicMapper.toEntity(sectionId, dto);
        return topicCrud.createTopic(sectionId, topic)
                .map(topicMapper::toDto)
                .map(e -> ResponseEntity.status(HttpStatus.CREATED).body(e))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Возвращает список тем для указанного раздела.
     *
     * @param sectionId идентификатор раздела
     * @return список тем с HTTP 200
     */
    @GetMapping("/sections/{sectionId}/topics")
    public ResponseEntity<List<TopicDto>> listTopics(@PathVariable UUID sectionId) {
        return ResponseEntity.ok(topicCrud.listTopics(sectionId).stream()
                .map(topicMapper::toDto)
                .collect(Collectors.toList()));
    }

    /**
     * Обновляет название, описание, вес и порядок сортировки темы.
     *
     * @param id  идентификатор обновляемой темы
     * @param dto новые данные темы из тела запроса
     * @return обновленная тема с HTTP 200 или HTTP 404
     */
    @PutMapping("/topics/{id}")
    public ResponseEntity<TopicDto> updateTopic(@PathVariable UUID id,
                                                 @Valid @RequestBody UpdateTopicRequestDto dto) {
        return topicCrud.updateTopic(id, e -> {
                    topicMapper.updateEntity(e, dto);
                    return e;
                })
                .map(topicMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Удаляет тему по идентификатору.
     *
     * @param id идентификатор удаляемой темы
     * @return HTTP 204 при успешном удалении
     */
    @DeleteMapping("/topics/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable UUID id) {
        topicCrud.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }
}