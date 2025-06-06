package com.codewithzea.projecttrackingsystem.controller;


import com.codewithzea.projecttrackingsystem.dto.AuditLogDTO;
import com.codewithzea.projecttrackingsystem.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<AuditLogDTO>> getLogs(@RequestParam(required = false) String entityType,
                                                     @RequestParam(required = false) String actorName) {
        return ResponseEntity.ok(auditLogService.getLogs(entityType, actorName));
    }
}


