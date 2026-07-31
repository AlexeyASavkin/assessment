package com.assessment.management.application;

import com.assessment.entity.Section;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Входной порт (use case) CRUD-операций над разделами.
 *
 * <p>Работает с JPA-сущностью {@link Section} как с доменной моделью
 * (pure-CRUD — сущность не имеет бизнес-инвариантов). Маппинг DTO ↔ сущность
 * выполняется на уровне HTTP-адаптера.
 */
public interface SectionCrudUseCase {

    /**
     * Создаёт раздел внутри указанной компетенции.
     *
     * @param competencyId идентификатор родительской компетенции
     * @param section      новый раздел (поля уже заполнены маппером)
     * @return созданный раздел или пусто, если компетенция не найдена
     */
    Optional<Section> createSection(UUID competencyId, Section section);

    /**
     * Возвращает разделы указанной компетенции.
     *
     * @param competencyId идентификатор компетенции
     * @return список разделов
     */
    List<Section> listSections(UUID competencyId);

    /**
     * Обновляет раздел: применяет к найденной сущности переданный мутатор
     * и сохраняет её.
     *
     * @param id      идентификатор обновляемого раздела
     * @param mutator функция, изменяющая поля сущности (задаётся адаптером через маппер DTO)
     * @return обновлённый раздел или пусто, если не найден
     */
    Optional<Section> updateSection(UUID id, UnaryOperator<Section> mutator);

    /**
     * Удаляет раздел по идентификатору.
     *
     * @param id идентификатор раздела
     */
    void deleteSection(UUID id);
}
