package com.assessment.dto.mapper;

import com.assessment.dto.AssessmentInviteTokenDto;
import com.assessment.dto.AssessmentInviteTokenSessionDto;
import com.assessment.entity.AssessmentInviteToken;
import com.assessment.entity.Session;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct-маппер для {@link AssessmentInviteToken} → {@link AssessmentInviteTokenDto}.
 *
 * <p>Вложенный объект {@link AssessmentInviteTokenDto#getEmployee()} маппится как
 * полный {@link com.assessment.dto.EmployeeDto} через {@link EmployeeMapper}.
 * Вложенный объект {@link AssessmentInviteTokenDto#getSession()} — как
 * урезанный {@link AssessmentInviteTokenSessionDto} (только {@code id} и {@code status}),
 * для чего используется {@link #sessionToDto(Session)}.
 */
@Mapper(
        componentModel = "spring",
        uses = {MapperSupport.class, EmployeeMapper.class})
public interface InviteTokenMapper {

    /**
     * Преобразует сущность {@link AssessmentInviteToken} в {@link AssessmentInviteTokenDto}.
     *
     * @param entity сущность токена (может быть {@code null})
     * @return DTO токена или {@code null}
     */
    @Mapping(target = "employee", source = "employee")
    @Mapping(target = "session",  source = "session", qualifiedByName = "sessionToDto")
    @Mapping(target = "usedAt",   source = "usedAt")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "createdAt", source = "createdAt")
    AssessmentInviteTokenDto toDto(AssessmentInviteToken entity);

    /**
     * Преобразует связанную сессию в урезанный DTO (только id и status).
     * Используется как квалифицированный helper для поля {@code session}.
     *
     * @param session сущность сессии (может быть {@code null})
     * @return DTO сессии только с идентификатором и статусом, либо {@code null}
     */
    @Named("sessionToDto")
    default AssessmentInviteTokenSessionDto sessionToDto(Session session) {
        if (session == null) return null;
        return new AssessmentInviteTokenSessionDto()
                .id(session.getId())
                .status(session.getStatus());
    }
}