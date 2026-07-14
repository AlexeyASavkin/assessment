package com.assessment.repository;

import com.assessment.entity.AiSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AiSettingsRepository extends JpaRepository<AiSettings, String> {
    Optional<AiSettings> findBySettingKey(String settingKey);
}