package com.assessment.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Настройки AI-провайдера.
 * Хранит пары ключ-значение для конфигурации подключения к LLM
 * (API-ключи, модель, параметры запросов и т.д.).
 */
@Entity
@Table(name = "ai_settings")
public class AiSettings {

    /** Ключ настройки (например, api_key, model). */
    @Id
    @Column(name = "setting_key")
    private String settingKey;

    /** Значение настройки. */
    @Column(name = "setting_value", nullable = false)
    private String settingValue;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Автоматически обновляет поле updatedAt перед сохранением или изменением записи.
     */
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }
    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}