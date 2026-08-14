package com.assessment.management.adapter.in;

import com.assessment.dto.CompetencyDto;
import com.assessment.dto.CreateCompetencyRequestDto;
import com.assessment.dto.UpdateCompetencyRequestDto;
import com.assessment.dto.mapper.CompetencyMapper;
import com.assessment.management.application.CompetencyCrudUseCase;
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
 * Тонкий HTTP-адаптер CRUD-операций над компетенциями (путь {@code /api/admin/competencies}).
 * Требует роль ADMIN, мапит DTO ↔ сущности через {@link CompetencyMapper} и делегирует
 * бизнес-логику {@link CompetencyCrudUseCase}. Не работает с JPA напрямую.
 */
@RestController
@RequestMapping("/api/admin")
public class CompetencyController {

    private final CompetencyCrudUseCase competencyCrud;
    private final CompetencyMapper competencyMapper;

    public CompetencyController(CompetencyCrudUseCase competencyCrud, CompetencyMapper competencyMapper) {
        this.competencyCrud = competencyCrud;
        this.competencyMapper = competencyMapper;
    }

    /**
     * Создает новую компетенцию.
     *
     * @param dto данные компетенции из тела запроса
     * @return созданная компетенция с HTTP 201
     */
    @PostMapping("/competencies")
    public ResponseEntity<CompetencyDto> createCompetency(@Valid @RequestBody CreateCompetencyRequestDto dto) {
        var entity = competencyMapper.toEntity(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(competencyMapper.toDto(competencyCrud.createCompetency(entity)));
    }

    /**
     * Возвращает список всех компетенций.
     *
     * @return список компетенций с HTTP 200
     */
    @GetMapping("/competencies")
    public ResponseEntity<List<CompetencyDto>> listCompetencies() {
        return ResponseEntity.ok(competencyCrud.listCompetencies().stream()
                .map(competencyMapper::toDto)
                .collect(Collectors.toList()));
    }

    /**
     * Возвращает компетенцию по идентификатору.
     *
     * @param id идентификатор компетенции
     * @return компетенция с HTTP 200 или HTTP 404, если не найдена
     */
    @GetMapping("/competencies/{id}")
    public ResponseEntity<CompetencyDto> getCompetency(@PathVariable UUID id) {
        return competencyCrud.getCompetency(id)
                .map(competencyMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Обновляет название и описание компетенции.
     *
     * @param id  идентификатор обновляемой компетенции
     * @param dto новые данные компетенции из тела запроса
     * @return обновленная компетенция с HTTP 200 или HTTP 404
     */
    @PutMapping("/competencies/{id}")
    public ResponseEntity<CompetencyDto> updateCompetency(@PathVariable UUID id,
                                                           @Valid @RequestBody UpdateCompetencyRequestDto dto) {
        return competencyCrud.updateCompetency(id, e -> {
                    competencyMapper.updateEntity(e, dto);
                    return e;
                })
                .map(competencyMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Удаляет компетенцию по идентификатору.
     *
     * @param id идентификатор удаляемой компетенции
     * @return HTTP 204 при успешном удалении
     */
    @DeleteMapping("/competencies/{id}")
    public ResponseEntity<Void> deleteCompetency(@PathVariable UUID id) {
        competencyCrud.deleteCompetency(id);
        return ResponseEntity.noContent().build();
    }
}