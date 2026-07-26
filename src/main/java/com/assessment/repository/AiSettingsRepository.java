package com.assessment.repository;

import com.assessment.entity.AiSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Репозиторий для управления настройками AI, используемыми при генерации вопросов и оценке ответов.
 */
public interface AiSettingsRepository extends JpaRepository<AiSettings, String> {

    /**
     * Находит настройку AI по её ключу.
     *
     * @param settingKey ключ настройки
     * @return настройка или пустой результат, если настройка не найдена
     */
    Optional<AiSettings> findBySettingKey(String settingKey);
}