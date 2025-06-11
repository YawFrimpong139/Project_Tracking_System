package com.codewithzea.projecttrackingsystem.service;

import com.codewithzea.projecttrackingsystem.dto.DeveloperDTO;
import com.codewithzea.projecttrackingsystem.exception.AuditLogException;
import com.codewithzea.projecttrackingsystem.exception.ResourceNotFoundException;
import com.codewithzea.projecttrackingsystem.model.Developer;
import com.codewithzea.projecttrackingsystem.repository.DeveloperRepository;
import com.codewithzea.projecttrackingsystem.util.MapperUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeveloperService {

    private static final String ENTITY_TYPE = "Developer";
    private final DeveloperRepository developerRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public DeveloperDTO getDeveloperById(Long id) {
        log.debug("Fetching developer with id: {}", id);
        Developer dev = developerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_TYPE + " not found with id " + id));
        return MapperUtil.toDeveloperDTO(dev);
    }

    public Page<DeveloperDTO> getAllDevelopers(Pageable pageable) {
        log.debug("Fetching all developers with pagination: {}", pageable);
        return developerRepository.findAll(pageable)
                .map(MapperUtil::toDeveloperDTO);
    }

    @Transactional
    public DeveloperDTO createDeveloper(DeveloperDTO dto) {
        log.debug("Creating new developer with data: {}", dto);
        validateDeveloperDTO(dto, true);

        Developer dev = MapperUtil.toDeveloper(dto);
        Developer saved = developerRepository.save(dev);

        auditOperation("CREATE", saved);
        log.info("Created new developer with id: {}", saved.getId());
        return MapperUtil.toDeveloperDTO(saved);
    }

    @Transactional
    public DeveloperDTO updateDeveloper(Long id, DeveloperDTO dto) {
        log.debug("Updating developer with id: {} and data: {}", id, dto);
        validateDeveloperDTO(dto, false);

        Developer dev = developerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_TYPE + " not found with id " + id));

        dev.setName(dto.getName());
        dev.setEmail(dto.getEmail());
        dev.setSkills(dto.getSkills());

        Developer updated = developerRepository.save(dev);

        auditOperation("UPDATE", updated);
        log.info("Updated developer with id: {}", updated.getId());
        return MapperUtil.toDeveloperDTO(updated);
    }

    @Transactional
    public void deleteDeveloper(Long id) {
        log.debug("Deleting developer with id: {}", id);
        Developer dev = developerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_TYPE + " not found with id " + id));
        developerRepository.delete(dev);
        auditLogService.log("DELETE", ENTITY_TYPE, id.toString(), "");
        log.info("Deleted developer with id: {}", id);
    }

    public List<DeveloperDTO> getTop5DevelopersByTaskCount() {
        log.debug("Fetching top 5 developers by task count");
        List<Developer> topDevs = developerRepository.findTop5ByTaskCount(PageRequest.of(0, 5));
        return topDevs.stream()
                .map(MapperUtil::toDeveloperDTO)
                .collect(Collectors.toList());
    }

    // --- Private Helper Methods ---

    private void validateDeveloperDTO(DeveloperDTO dto, boolean isNew) {
        if (dto == null) {
            throw new IllegalArgumentException("Developer data cannot be null");
        }

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Developer name cannot be empty");
        }

        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new IllegalArgumentException("Developer email cannot be empty");
        }

        if (!isValidEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (isNew && developerRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already in use.");
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }

    private void auditOperation(String operation, Developer developer) {
        try {
            String payload = objectMapper.writeValueAsString(developer);
            auditLogService.log(operation, ENTITY_TYPE, developer.getId().toString(), payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize developer for audit logging", e);
            throw new AuditLogException("Failed to audit developer operation", e);
        }
    }
}
