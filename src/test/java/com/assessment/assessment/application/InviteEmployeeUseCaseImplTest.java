package com.assessment.assessment.application;

import com.assessment.assessment.domain.AssessmentSession;
import com.assessment.assessment.domain.InviteToken;
import com.assessment.assessment.domain.SessionStatus;
import com.assessment.assessment.port.out.InviteTokenRepositoryPort;
import com.assessment.assessment.port.out.SessionRepositoryPort;
import com.assessment.security.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InviteEmployeeUseCase: валидация пригласительной ссылки сотрудника")
class InviteEmployeeUseCaseImplTest {

    private static final String TOKEN = "invite-token";
    private static final String HASH = "token-hash";
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();

    @Mock
    private InviteTokenRepositoryPort inviteTokenRepositoryPort;

    @Mock
    private SessionRepositoryPort sessionRepositoryPort;

    @Mock
    private TokenHasher tokenHasher;

    private InviteEmployeeUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new InviteEmployeeUseCaseImpl(inviteTokenRepositoryPort, sessionRepositoryPort, tokenHasher);
    }

    @Test
    @DisplayName("Неиспользованный токен без существующей сессии создаёт новую сессию и помечает токен")
    void unusedTokenWithoutExistingSessionCreatesNewSession() {
        InviteToken inviteToken = inviteToken(false, null, Instant.now().plus(1, ChronoUnit.DAYS));
        when(tokenHasher.hash(TOKEN)).thenReturn(HASH);
        when(inviteTokenRepositoryPort.findByTokenHash(HASH)).thenReturn(Optional.of(inviteToken));
        when(sessionRepositoryPort.findFirstByEmployeeIdOrderByCreatedAtDesc(EMPLOYEE_ID))
                .thenReturn(Optional.empty());
        UUID newSessionId = UUID.randomUUID();
        when(sessionRepositoryPort.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), newSessionId));

        Optional<InviteOutcome> result = useCase.validateInvite(TOKEN);

        assertTrue(result.isPresent());
        assertFalse(result.get().reused());
        assertEquals(newSessionId, result.get().session().getId());
        verify(sessionRepositoryPort).save(argThat(s ->
                EMPLOYEE_ID.equals(s.getEmployeeId()) && s.getStatus() == SessionStatus.ACTIVE));
        verify(inviteTokenRepositoryPort).save(argThat(t ->
                t.isUsed() && newSessionId.equals(t.getSessionId()) && t.getUsedAt() != null));
    }

    @Test
    @DisplayName("Неиспользованный токен с существующей сессией переиспользует её без создания новой")
    void unusedTokenWithExistingSessionReusesIt() {
        InviteToken inviteToken = inviteToken(false, null, Instant.now().plus(1, ChronoUnit.DAYS));
        AssessmentSession existing = session(UUID.randomUUID());
        when(tokenHasher.hash(TOKEN)).thenReturn(HASH);
        when(inviteTokenRepositoryPort.findByTokenHash(HASH)).thenReturn(Optional.of(inviteToken));
        when(sessionRepositoryPort.findFirstByEmployeeIdOrderByCreatedAtDesc(EMPLOYEE_ID))
                .thenReturn(Optional.of(existing));

        Optional<InviteOutcome> result = useCase.validateInvite(TOKEN);

        assertTrue(result.isPresent());
        assertTrue(result.get().reused());
        assertEquals(existing.getId(), result.get().session().getId());
        verify(sessionRepositoryPort, never()).save(any());
        verify(inviteTokenRepositoryPort).save(argThat(t ->
                t.isUsed() && existing.getId().equals(t.getSessionId())));
    }

    @Test
    @DisplayName("Использованный токен с привязанной сессией отклоняется — возвращается пустой результат")
    void usedTokenWithSessionReturnsEmpty() {
        UUID sessionId = UUID.randomUUID();
        InviteToken inviteToken = inviteToken(true, sessionId, Instant.now().plus(1, ChronoUnit.DAYS));
        when(tokenHasher.hash(TOKEN)).thenReturn(HASH);
        when(inviteTokenRepositoryPort.findByTokenHash(HASH)).thenReturn(Optional.of(inviteToken));

        Optional<InviteOutcome> result = useCase.validateInvite(TOKEN);

        assertTrue(result.isEmpty());
        verify(sessionRepositoryPort, never()).findById(any());
        verify(sessionRepositoryPort, never()).findFirstByEmployeeIdOrderByCreatedAtDesc(any());
        verify(inviteTokenRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Использованный токен без привязки к сессии также отклоняется")
    void usedTokenWithoutSessionIdReturnsEmpty() {
        InviteToken inviteToken = inviteToken(true, null, Instant.now().plus(1, ChronoUnit.DAYS));
        when(tokenHasher.hash(TOKEN)).thenReturn(HASH);
        when(inviteTokenRepositoryPort.findByTokenHash(HASH)).thenReturn(Optional.of(inviteToken));

        Optional<InviteOutcome> result = useCase.validateInvite(TOKEN);

        assertTrue(result.isEmpty());
        verify(sessionRepositoryPort, never()).findFirstByEmployeeIdOrderByCreatedAtDesc(any());
        verify(inviteTokenRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Повторное открытие ссылки отклоняется: второй вызов с тем же токеном возвращает пустой результат")
    void secondCallWithSameTokenReturnsEmpty() {
        // Первый вызов — токен не использован, создаётся сессия и токен помечается использованным
        InviteToken unused = inviteToken(false, null, Instant.now().plus(1, ChronoUnit.DAYS));
        when(tokenHasher.hash(TOKEN)).thenReturn(HASH);
        when(inviteTokenRepositoryPort.findByTokenHash(HASH)).thenReturn(Optional.of(unused));
        when(sessionRepositoryPort.findFirstByEmployeeIdOrderByCreatedAtDesc(EMPLOYEE_ID))
                .thenReturn(Optional.empty());
        UUID newSessionId = UUID.randomUUID();
        when(sessionRepositoryPort.save(any())).thenAnswer(inv -> withId(inv.getArgument(0), newSessionId));

        Optional<InviteOutcome> first = useCase.validateInvite(TOKEN);
        assertTrue(first.isPresent());

        // Второй вызов — токен уже использован (как после markUsed), доступ отклоняется
        when(inviteTokenRepositoryPort.findByTokenHash(HASH))
                .thenReturn(Optional.of(inviteToken(true, newSessionId, Instant.now().plus(1, ChronoUnit.DAYS))));

        Optional<InviteOutcome> second = useCase.validateInvite(TOKEN);

        assertTrue(second.isEmpty());
        verify(sessionRepositoryPort, never()).findById(any());
    }

    @Test
    @DisplayName("Истёкший токен возвращает пустой результат без создания сессии")
    void expiredTokenReturnsEmpty() {
        InviteToken inviteToken = inviteToken(false, null, Instant.now().minus(1, ChronoUnit.DAYS));
        when(tokenHasher.hash(TOKEN)).thenReturn(HASH);
        when(inviteTokenRepositoryPort.findByTokenHash(HASH)).thenReturn(Optional.of(inviteToken));

        Optional<InviteOutcome> result = useCase.validateInvite(TOKEN);

        assertTrue(result.isEmpty());
        verify(sessionRepositoryPort, never()).save(any());
        verify(inviteTokenRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Неизвестный токен возвращает пустой результат без обращения к сессиям")
    void unknownTokenReturnsEmpty() {
        when(tokenHasher.hash(TOKEN)).thenReturn(HASH);
        when(inviteTokenRepositoryPort.findByTokenHash(HASH)).thenReturn(Optional.empty());

        Optional<InviteOutcome> result = useCase.validateInvite(TOKEN);

        assertTrue(result.isEmpty());
        verifyNoInteractions(sessionRepositoryPort);
    }

    private InviteToken inviteToken(boolean used, UUID sessionId, Instant expiresAt) {
        return InviteToken.of(UUID.randomUUID(), HASH, EMPLOYEE_ID, sessionId, used,
                used ? Instant.now() : null, expiresAt, Instant.now());
    }

    private AssessmentSession session(UUID id) {
        return AssessmentSession.of(id, EMPLOYEE_ID, "Иванов Иван", null,
                SessionStatus.ACTIVE, null, Instant.now());
    }

    private static AssessmentSession withId(AssessmentSession s, UUID id) {
        return AssessmentSession.of(id, s.getEmployeeId(), s.getEmployeeName(), s.getCompetencyId(),
                s.getStatus(), s.getCurrentQuestionId(), s.getCreatedAt());
    }
}