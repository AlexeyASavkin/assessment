package com.assessment.assessment.port.out;

import com.assessment.assessment.domain.InviteToken;

import java.util.Optional;

/**
 * Выходной порт доступа к пригласительным токенам.
 *
 * <p>Реализуется JPA-адаптером поверх {@code AssessmentInviteTokenRepository}.
 */
public interface InviteTokenRepositoryPort {

    /**
     * Находит токен по HMAC-хешу.
     *
     * @param tokenHash хеш токена
     * @return токен или пустой результат, если не найден
     */
    Optional<InviteToken> findByTokenHash(String tokenHash);

    InviteToken save(InviteToken token);
}
