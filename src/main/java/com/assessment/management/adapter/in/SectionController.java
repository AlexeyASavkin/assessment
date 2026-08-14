package com.assessment.management.adapter.in;

import com.assessment.dto.CreateSectionRequestDto;
import com.assessment.dto.SectionDto;
import com.assessment.dto.UpdateSectionRequestDto;
import com.assessment.dto.mapper.SectionMapper;
import com.assessment.management.application.SectionCrudUseCase;
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
 * Тонкий HTTP-адаптер CRUD-операций над разделами компетенций
 * (пути {@code /api/admin/competencies/{competencyId}/sections} и {@code /api/admin/sections/{id}}).
 * Требует роль ADMIN, делегирует бизнес-логику {@link SectionCrudUseCase}, маппит DTO через {@link SectionMapper}.
 */
@RestController
@RequestMapping("/api/admin")
public class SectionController {

    private final SectionCrudUseCase sectionCrud;
    private final SectionMapper sectionMapper;

    public SectionController(SectionCrudUseCase sectionCrud, SectionMapper sectionMapper) {
        this.sectionCrud = sectionCrud;
        this.sectionMapper = sectionMapper;
    }

    /**
     * Создает раздел внутри указанной компетенции.
     *
     * @param competencyId идентификатор компетенции
     * @param dto          данные раздела из тела запроса
     * @return созданный раздел с HTTP 201 или HTTP 404, если компетенция не найдена
     */
    @PostMapping("/competencies/{competencyId}/sections")
    public ResponseEntity<SectionDto> createSection(@PathVariable UUID competencyId,
                                                     @Valid @RequestBody CreateSectionRequestDto dto) {
        var section = sectionMapper.toEntity(competencyId, dto);
        return sectionCrud.createSection(competencyId, section)
                .map(sectionMapper::toDto)
                .map(e -> ResponseEntity.status(HttpStatus.CREATED).body(e))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Возвращает список разделов для указанной компетенции.
     *
     * @param competencyId идентификатор компетенции
     * @return список разделов с HTTP 200
     */
    @GetMapping("/competencies/{competencyId}/sections")
    public ResponseEntity<List<SectionDto>> listSections(@PathVariable UUID competencyId) {
        return ResponseEntity.ok(sectionCrud.listSections(competencyId).stream()
                .map(sectionMapper::toDto)
                .collect(Collectors.toList()));
    }

    /**
     * Обновляет название, описание и порядок сортировки раздела.
     *
     * @param id  идентификатор обновляемого раздела
     * @param dto новые данные раздела из тела запроса
     * @return обновленный раздел с HTTP 200 или HTTP 404
     */
    @PutMapping("/sections/{id}")
    public ResponseEntity<SectionDto> updateSection(@PathVariable UUID id,
                                                    @Valid @RequestBody UpdateSectionRequestDto dto) {
        return sectionCrud.updateSection(id, e -> {
                    sectionMapper.updateEntity(e, dto);
                    return e;
                })
                .map(sectionMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Удаляет раздел по идентификатору.
     *
     * @param id идентификатор удаляемого раздела
     * @return HTTP 204 при успешном удалении
     */
    @DeleteMapping("/sections/{id}")
    public ResponseEntity<Void> deleteSection(@PathVariable UUID id) {
        sectionCrud.deleteSection(id);
        return ResponseEntity.noContent().build();
    }
}