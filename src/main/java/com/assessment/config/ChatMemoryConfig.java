package com.assessment.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.PostgresChatMemoryRepositoryDialect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Конфигурация памяти чата для сохранения контекста LLM-диалогов в PostgreSQL.
 * <p>
 * Использует JDBC-репозиторий с диалектом PostgreSQL и ограничивает
 * размер окна сообщений настройкой {@code max-questions-per-session}.
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * Создает бин памяти чата с JDBC-хранилищем в PostgreSQL.
     * <p>
     * Сообщения хранятся в базе данных, что позволяет восстанавливать
     * контекст сессии при перезапуске приложения.
     *
     * @param url JDBC-URL подключения к PostgreSQL
     * @param username имя пользователя базы данных
     * @param password пароль пользователя базы данных
     * @param maxMessages максимальное количество сообщений в окне памяти
     * @return настроенный {@link ChatMemory}
     */
    @Bean
    public ChatMemory chatMemory(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${assessment.question.max-questions-per-session}") int maxMessages) {

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        JdbcChatMemoryRepository chatMemoryRepository = JdbcChatMemoryRepository.builder()
                .dialect(new PostgresChatMemoryRepositoryDialect())
                .jdbcTemplate(new JdbcTemplate(dataSource))
                .build();

        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(maxMessages)
                .build();
    }
}
