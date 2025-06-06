package com.codewithzea.projecttrackingsystem.dto;


import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDTO {
    private String id;
    private String actionType;
    private String entityType;
    private String entityId;
    private Instant timestamp;
    private String actorName;
    private String payload;
}


