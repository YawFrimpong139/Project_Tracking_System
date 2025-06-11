package com.codewithzea.projecttrackingsystem.controller;


import com.codewithzea.projecttrackingsystem.dto.AuditLogDTO;
import com.codewithzea.projecttrackingsystem.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<AuditLogDTO>> getLogs(
            @RequestParam Optional<String> entityType,
            @RequestParam Optional<String> actorName,
            @RequestParam Optional<String> startDate, // Pass as ISO-8601 String, parse to Instant
            @RequestParam Optional<String> endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Optional<Instant> start = startDate.map(Instant::parse);
        Optional<Instant> end = endDate.map(Instant::parse);

        Page<AuditLogDTO> logs = auditLogService.getLogs(entityType, actorName, start, end, pageable);
        return ResponseEntity.ok(logs);
    }

}


