package com.assessment.management.application;

import com.assessment.entity.AssessmentInviteToken;
import com.assessment.entity.Employee;
import com.assessment.management.port.out.EmployeeRepositoryPort;
import com.assessment.management.port.out.TokenRepositoryPort;
import com.assessment.security.HmacTokenValidator;
import com.assessment.security.RandomTokenGenerator;
import com.assessment.security.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenManagementUseCase: генерация пригласительных ссылок и список токенов")
class TokenManagementUseCaseImplTest {

    @Mock
    private EmployeeRepositoryPort employeeRepositoryPort;

    @Mock
    private TokenRepositoryPort tokenRepositoryPort;

    @Mock
    private RandomTokenGenerator randomTokenGenerator;

    @Mock
    private TokenHasher tokenHasher;

    private TokenManagementUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new TokenManagementUseCaseImpl(employeeRepositoryPort, tokenRepositoryPort,
                randomTokenGenerator, tokenHasher);
    }

    @Test
    @DisplayName("generateInviteLink возвращает ссылку со случайным токеном и сохраняет его хеш")
    void generateInviteLinkSuccess() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = Employee.builder().id(employeeId).fullName("Иванов Иван").build();
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(employee));
        when(randomTokenGenerator.generate()).thenReturn("raw-token");
        when(tokenHasher.hash("raw-token")).thenReturn("token-hash");

        Optional<String> link = useCase.generateInviteLink(employeeId);

        assertTrue(link.isPresent());
        assertEquals("/api/employee/invite/raw-token", link.get());

        ArgumentCaptor<AssessmentInviteToken> captor = ArgumentCaptor.forClass(AssessmentInviteToken.class);
        verify(tokenRepositoryPort).save(captor.capture());
        AssessmentInviteToken saved = captor.getValue();
        assertEquals("token-hash", saved.getTokenHash());
        assertSame(employee, saved.getEmployee());
        assertNotNull(saved.getExpiresAt());
        assertTrue(saved.getExpiresAt().isAfter(Instant.now().plusSeconds(71 * 3600)));
        assertTrue(saved.getExpiresAt().isBefore(Instant.now().plusSeconds(73 * 3600)));
    }

    @Test
    @DisplayName("Возвращённый токен не равен HMAC-подписи employeeId (токен теперь случайный)")
    void returnedTokenIsNotHmacOfEmployeeId() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = Employee.builder().id(employeeId).build();
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(employee));
        when(randomTokenGenerator.generate()).thenReturn("random-256-bit-token");
        when(tokenHasher.hash("random-256-bit-token")).thenReturn("token-hash");

        Optional<String> link = useCase.generateInviteLink(employeeId);

        assertTrue(link.isPresent());
        String token = link.get().substring(link.get().lastIndexOf('/') + 1);
        String hmacOfEmployeeId = new HmacTokenValidator("test-secret").generateToken(employeeId.toString());
        assertNotEquals(hmacOfEmployeeId, token);
    }

    @Test
    @DisplayName("generateInviteLink удаляет старые токены сотрудника перед сохранением нового")
    void generateInviteLinkDeletesOldTokensFirst() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = Employee.builder().id(employeeId).build();
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.of(employee));
        when(randomTokenGenerator.generate()).thenReturn("raw-token");
        when(tokenHasher.hash(anyString())).thenReturn("token-hash");

        useCase.generateInviteLink(employeeId);

        InOrder inOrder = inOrder(tokenRepositoryPort);
        inOrder.verify(tokenRepositoryPort).deleteByEmployeeId(employeeId);
        inOrder.verify(tokenRepositoryPort).save(any(AssessmentInviteToken.class));
    }

    @Test
    @DisplayName("generateInviteLink возвращает пусто, если сотрудник не найден")
    void generateInviteLinkEmployeeNotFound() {
        UUID employeeId = UUID.randomUUID();
        when(employeeRepositoryPort.findById(employeeId)).thenReturn(Optional.empty());

        Optional<String> link = useCase.generateInviteLink(employeeId);

        assertTrue(link.isEmpty());
        verifyNoInteractions(tokenRepositoryPort, randomTokenGenerator, tokenHasher);
    }
}