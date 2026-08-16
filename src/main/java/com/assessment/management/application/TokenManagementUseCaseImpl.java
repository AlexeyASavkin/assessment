package com.assessment.management.application;

import com.assessment.entity.AssessmentInviteToken;
import com.assessment.management.port.out.EmployeeRepositoryPort;
import com.assessment.management.port.out.TokenRepositoryPort;
import com.assessment.security.RandomTokenGenerator;
import com.assessment.security.TokenHasher;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Реализация use case управления одноразовыми пригласительными токенами.
 *
 * <p>Воспроизводит бизнес-логику {@code AdminController}: перед генерацией удаляет
 * предыдущие токены сотрудника (во избежание нарушения уникального ограничения),
 * создаёт случайный 256-битный токен со сроком действия 72 часа, в БД сохраняется
 * только его SHA-256 хеш. Зависит от выходных портов {@link EmployeeRepositoryPort},
 * {@link TokenRepositoryPort}, {@link RandomTokenGenerator} и {@link TokenHasher}.
 */
@Service
public class TokenManagementUseCaseImpl implements TokenManagementUseCase {

    private final EmployeeRepositoryPort employeeRepositoryPort;
    private final TokenRepositoryPort tokenRepositoryPort;
    private final RandomTokenGenerator randomTokenGenerator;
    private final TokenHasher tokenHasher;

    public TokenManagementUseCaseImpl(EmployeeRepositoryPort employeeRepositoryPort,
                                      TokenRepositoryPort tokenRepositoryPort,
                                      RandomTokenGenerator randomTokenGenerator,
                                      TokenHasher tokenHasher) {
        this.employeeRepositoryPort = employeeRepositoryPort;
        this.tokenRepositoryPort = tokenRepositoryPort;
        this.randomTokenGenerator = randomTokenGenerator;
        this.tokenHasher = tokenHasher;
    }

    @Override
    @Transactional
    public Optional<String> generateInviteLink(UUID employeeId) {
        return employeeRepositoryPort.findById(employeeId)
                .map(employee -> {
                    tokenRepositoryPort.deleteByEmployeeId(employeeId);
                    String token = randomTokenGenerator.generate();
                    String hash = tokenHasher.hash(token);
                    AssessmentInviteToken inviteToken = AssessmentInviteToken.builder()
                            .tokenHash(hash)
                            .employee(employee)
                            .expiresAt(Instant.now().plusSeconds(72 * 3600))
                            .build();
                    tokenRepositoryPort.save(inviteToken);
                    return "/api/employee/invite/" + token;
                });
    }

    @Override
    public List<AssessmentInviteToken> listTokens() {
        return tokenRepositoryPort.findAll();
    }
}
