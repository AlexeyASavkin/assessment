package com.assessment.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AdminUserDetailsService: загрузка администратора из окружения")
class AdminUserDetailsServiceTest {

    @Test
    @DisplayName("Совпадающий логин возвращает корректные данные из env")
    void loadUserByUsernameFoundReturnsUserDetails() {
        AdminUserDetailsService service = new AdminUserDetailsService("admin", "{bcrypt}hash");

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
    @DisplayName("Неизвестный логин вызывает UsernameNotFoundException")
    void loadUserByUsernameNotFoundThrows() {
        AdminUserDetailsService service = new AdminUserDetailsService("admin", "{bcrypt}hash");

        assertThrows(UsernameNotFoundException.class,
            () -> service.loadUserByUsername("unknown"));
    }

    @Test
    @DisplayName("Пустой ADMIN_PASSWORD_HASH означает ненастроенного администратора")
    void loadUserByUsernameEmptyPasswordHashThrows() {
        AdminUserDetailsService service = new AdminUserDetailsService("admin", "");

        assertThrows(UsernameNotFoundException.class,
            () -> service.loadUserByUsername("admin"));
    }

    @Test
    @DisplayName("Поиск чувствителен к регистру логина")
    void loadUserByUsernameCaseSensitive() {
        AdminUserDetailsService service = new AdminUserDetailsService("admin", "{bcrypt}hash");

        assertThrows(UsernameNotFoundException.class,
            () -> service.loadUserByUsername("Admin"));
    }

    @Test
    @DisplayName("null-логин вызывает UsernameNotFoundException")
    void loadUserByUsernameNullUsername() {
        AdminUserDetailsService service = new AdminUserDetailsService("admin", "{bcrypt}hash");

        assertThrows(UsernameNotFoundException.class,
            () -> service.loadUserByUsername(null));
    }
}