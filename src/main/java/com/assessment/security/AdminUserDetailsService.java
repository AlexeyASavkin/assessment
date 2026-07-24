package com.assessment.security;

import com.assessment.entity.AdminUser;
import com.assessment.repository.AdminUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис загрузки данных администраторов для Spring Security.
 * <p>
 * Извлекает пользователей из базы данных, формирует объекты {@link UserDetails}
 * с ролями и статусом активности.
 */
@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    /**
     * Конструктор, внедряющий репозиторий администраторов.
     *
     * @param adminUserRepository репозиторий пользователей-администраторов
     */
    public AdminUserDetailsService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    /**
     * Загружает данные администратора по имени пользователя.
     * <p>
     * Если пользователь не найден, выбрасывает {@link UsernameNotFoundException}.
     * Роль пользователя префиксируется {@code ROLE_} для совместимости с Spring Security.
     *
     * @param username имя пользователя (логин)
     * @return объект {@link UserDetails} с паролем, ролями и статусом
     * @throws UsernameNotFoundException если пользователь не существует
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser adminUser = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return User.builder()
                .username(adminUser.getUsername())
                .password(adminUser.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + adminUser.getRole())))
                .disabled(!adminUser.getEnabled())
                .build();
    }
}
