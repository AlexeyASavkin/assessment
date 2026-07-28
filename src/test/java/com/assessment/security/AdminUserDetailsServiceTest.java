package com.assessment.security;

import com.assessment.entity.AdminUser;
import com.assessment.repository.AdminUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserDetailsService: загрузка пользователей из БД")
class AdminUserDetailsServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    private AdminUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserDetailsService(adminUserRepository);
    }

    @Test
    @DisplayName("Существующий пользователь возвращает корректные данные")
    void loadUserByUsernameFoundReturnsUserDetails() {
        AdminUser admin = AdminUser.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .passwordHash("{bcrypt}hash")
                .role("ADMIN")
                .enabled(true)
                .build();
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        UserDetails details = service.loadUserByUsername("admin");

        assertEquals("admin", details.getUsername());
        assertEquals("{bcrypt}hash", details.getPassword());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(details.isEnabled());
        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isCredentialsNonExpired());
    }

    @Test
    @DisplayName("Неизвестный пользователь вызывает UsernameNotFoundException")
    void loadUserByUsernameNotFoundThrows() {
        when(adminUserRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
            () -> service.loadUserByUsername("unknown"));
    }

    @Test
    @DisplayName("Отключённый пользователь возвращается с enabled=false")
    void loadUserByUsernameDisabledUser() {
        AdminUser admin = AdminUser.builder()
                .id(UUID.randomUUID())
                .username("disabled")
                .passwordHash("{bcrypt}hash")
                .role("ADMIN")
                .enabled(false)
                .build();
        when(adminUserRepository.findByUsername("disabled")).thenReturn(Optional.of(admin));

        UserDetails details = service.loadUserByUsername("disabled");

        assertFalse(details.isEnabled());
    }

    @Test
    @DisplayName("Поиск чувствителен к регистру username")
    void loadUserByUsernameCaseSensitive() {
        when(adminUserRepository.findByUsername("Admin")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
            () -> service.loadUserByUsername("Admin"));
    }

    @Test
    @DisplayName("null-username вызывает UsernameNotFoundException")
    void loadUserByUsernameNullUsername() {
        assertThrows(UsernameNotFoundException.class,
            () -> service.loadUserByUsername(null));
    }
}
