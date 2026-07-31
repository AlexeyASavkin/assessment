package com.assessment.management.adapter.out;

import com.assessment.entity.AssessmentInviteToken;
import com.assessment.management.port.out.TokenRepositoryPort;
import com.assessment.repository.AssessmentInviteTokenRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * JPA-адаптер выходного порта доступа к пригласительным токенам в management-контексте.
 *
 * <p>Оборачивает {@link AssessmentInviteTokenRepository} и делегирует ему вызовы без
 * дополнительной логики (сущность используется как есть).
 */
@Component
public class TokenJpaAdapter implements TokenRepositoryPort {

    private final AssessmentInviteTokenRepository tokenRepository;

    public TokenJpaAdapter(AssessmentInviteTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Override
    public AssessmentInviteToken save(AssessmentInviteToken token) {
        return tokenRepository.save(token);
    }

    @Override
    public List<AssessmentInviteToken> findAll() {
        return tokenRepository.findAll();
    }

    @Override
    public void deleteByEmployeeId(UUID employeeId) {
        tokenRepository.deleteByEmployeeId(employeeId);
    }
}
