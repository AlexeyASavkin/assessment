package com.assessment.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Сервис загрузки данных администратора для Spring Security.
 * <p>
 * Учётная запись администратора не хранится в БД: логин и BCrypt-хэш пароля
 * задаются переменными окружения {@code ADMIN_USERNAME} и
 * {@code ADMIN_PASSWORD_HASH} и читаются при каждом обращении.
 */
@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final String adminUsername;
    private final String adminPasswordHash;

    /**
     * Конструктор, принимающий учётные данные администратора из окружения.
     *
     * @param adminUsername     логин администратора (env {@code ADMIN_USERNAME})
     * @param adminPasswordHash BCrypt-хэш пароля (env {@code ADMIN_PASSWORD_HASH})
     */
    public AdminUserDetailsService(
            @Value("${ADMIN_USERNAME:}") String adminUsername,
            @Value("${ADMIN_PASSWORD_HASH:}") String adminPasswordHash) {
        this.adminUsername = adminUsername;
        // docker compose env_file НЕ интерполирует значения: BCrypt-хэш из .env приходит
        // в виде {bcrypt}$$2a$$12$$... (двойные $ — escaping для compose-файла). Разэкранируем
        // здесь, чтобы приложение работало при любом способе запуска (compose / start-dev.cmd /
        // bootRun / jar). Для валидного BCrypt-хэша замена безопасна: формат $2a$12$... не
        // содержит подряд идущих '$'.
        this.adminPasswordHash = adminPasswordHash.replace("$$", "$");
    }

    /**
     * Загружает данные администратора по имени пользователя.
     * <p>
     * Если имя не совпадает с {@code ADMIN_USERNAME} или пароль не настроен
     * (пустой {@code ADMIN_PASSWORD_HASH}), выбрасывает
     * {@link UsernameNotFoundException}. Роль всегда {@code ROLE_ADMIN}.
     *
     * @param username имя пользователя (логин)
     * @return объект {@link UserDetails} с паролем и ролью ADMIN
     * @throws UsernameNotFoundException если пользователь не существует или не настроен
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (adminPasswordHash.isBlank()
                || !Objects.equals(adminUsername, username)) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        return User.builder()
                .username(adminUsername)
                .password(adminPasswordHash)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();
    }
}
