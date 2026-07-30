package com.assessment.dto.mapper;

import org.mapstruct.Mapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Общий helper для конвертации временных типов между JPA-сущностями и DTO.
 *
 * <p>Spring-бин: другие мапперы подключают его через {@code uses = MapperSupport.class}.
 * MapStruct автоматически использовает метод {@link #toOffsetDateTime(Instant)}
 * для полей типа {@code Instant} в сущности → {@code OffsetDateTime} в DTO.
 */
@Mapper(componentModel = "spring")
public interface MapperSupport {

    /** Конвертирует {@link Instant} (UTC) в {@link OffsetDateTime} сохранением зоны UTC. */
    default OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}