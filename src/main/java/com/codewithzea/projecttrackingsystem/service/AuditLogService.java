package com.codewithzea.projecttrackingsystem.service;

import com.codewithzea.projecttrackingsystem.dto.AuditLogDTO;
import com.codewithzea.projecttrackingsystem.model.AuditLog;
import com.codewithzea.projecttrackingsystem.repository.audit.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final MongoTemplate mongoTemplate;

    public void log(String actionType, String entityType, String entityId, String payloadJson) {
        log(actionType, entityType, entityId, "SYSTEM", payloadJson);
    }

    public void log(String actionType, String entityType, String entityId, String actorName, String payloadJson) {
        try {
            AuditLog logEntry = AuditLog.builder()
                    .actionType(actionType)
                    .entityType(entityType)
                    .entityId(entityId)
                    .actorName(Optional.ofNullable(actorName).orElse("SYSTEM"))
                    .payload(payloadJson)
                    .timestamp(Instant.now())
                    .build();

            auditLogRepository.save(logEntry);
            log.debug("Audit log saved for {}-{} by {}", entityType, entityId, actorName);
        } catch (Exception e) {
            log.error("Failed to save audit log for entityId: {}", entityId, e);
        }
    }

    public Page<AuditLogDTO> getLogs(Optional<String> entityType,
                                     Optional<String> actorName,
                                     Optional<Instant> from,
                                     Optional<Instant> to,
                                     Pageable pageable) {

        Query query = new Query().with(pageable);

        entityType.ifPresent(type -> query.addCriteria(Criteria.where("entityType").is(type)));
        actorName.ifPresent(actor -> query.addCriteria(Criteria.where("actorName").is(actor)));
        from.ifPresent(start -> query.addCriteria(Criteria.where("timestamp").gte(start)));
        to.ifPresent(end -> query.addCriteria(Criteria.where("timestamp").lte(end)));

        long total = mongoTemplate.count(query, AuditLog.class);
        List<AuditLog> logs = mongoTemplate.find(query, AuditLog.class);

        List<AuditLogDTO> dtoList = logs.stream().map(this::toDTO).collect(Collectors.toList());
        return new PageImpl<>(dtoList, pageable, total);
    }

    private AuditLogDTO toDTO(AuditLog log) {
        return AuditLogDTO.builder()
                .actionType(log.getActionType())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .timestamp(log.getTimestamp())
                .actorName(log.getActorName())
                .payload(log.getPayload())
                .build();
    }
}
