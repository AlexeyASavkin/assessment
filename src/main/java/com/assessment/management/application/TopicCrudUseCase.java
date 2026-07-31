package com.assessment.management.application;

import com.assessment.entity.Topic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Входной порт (use case) CRUD-операций над темами.
 *
 * <p>Работает с JPA-сущностью {@link Topic} как с доменной моделью
 * (pure-CRUD — сущность не имеет бизнес-инвариантов). Маппинг DTO ↔ сущность
 * выполняется на уровне HTTP-адаптера.
 */
public interface TopicCrudUseCase {

    /**
     * Создаёт тему внутри указанного раздела.
     *
     * @param sectionId идентификатор родительского раздела
     * @param topic     новая тема (поля уже заполнены маппером)
     * @return созданная тема или пусто, если раздел не найден
     */
    Optional<Topic> createTopic(UUID sectionId, Topic topic);

    /**
     * Возвращает темы указанного раздела.
     *
     * @param sectionId идентификатор раздела
     * @return список тем
     */
    List<Topic> listTopics(UUID sectionId);

    /**
     * Обновляет тему: применяет к найденной сущности переданный мутатор
     * и сохраняет её.
     *
     * @param id      идентификатор обновляемой темы
     * @param mutator функция, изменяющая поля сущности (задаётся адаптером через маппер DTO)
     * @return обновлённая тема или пусто, если не найдена
     */
    Optional<Topic> updateTopic(UUID id, UnaryOperator<Topic> mutator);

    /**
     * Удаляет тему по идентификатору.
     *
     * @param id идентификатор темы
     */
    void deleteTopic(UUID id);
}
