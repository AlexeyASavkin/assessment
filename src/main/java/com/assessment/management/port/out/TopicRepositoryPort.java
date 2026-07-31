package com.assessment.management.port.out;

import com.assessment.entity.Topic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Выходной порт доступа к сущности {@link Topic} в management-контексте.
 *
 * <p>Реализуется JPA-адаптером поверх Spring Data репозитория.
 * Сущность используется как есть (pure-CRUD, без отдельной доменной модели).
 */
public interface TopicRepositoryPort {

    /**
     * Сохраняет (создаёт или обновляет) тему.
     *
     * @param topic тема
     * @return сохранённая тема
     */
    Topic save(Topic topic);

    /**
     * Возвращает тему по идентификатору.
     *
     * @param id идентификатор темы
     * @return тема или пусто, если не найдена
     */
    Optional<Topic> findById(UUID id);

    /**
     * Возвращает темы указанного раздела.
     *
     * @param sectionId идентификатор раздела
     * @return список тем
     */
    List<Topic> findBySectionId(UUID sectionId);

    /**
     * Удаляет тему по идентификатору.
     *
     * @param id идентификатор темы
     */
    void deleteById(UUID id);
}
