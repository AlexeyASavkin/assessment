package com.assessment.repository;

import com.assessment.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для управления учётными записями администраторов системы.
 */
public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {

    /**
     * Находит администратора по имени пользователя.
     *
     * @param username имя пользователя
     * @return учётная запись администратора или пустой результат, если не найдена
     */
    Optional<AdminUser> findByUsername(String username);
}
