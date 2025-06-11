package com.codewithzea.projecttrackingsystem.service;

import com.codewithzea.projecttrackingsystem.dto.ProjectDTO;
import com.codewithzea.projecttrackingsystem.model.Project;
import com.codewithzea.projecttrackingsystem.exception.*;
import com.codewithzea.projecttrackingsystem.repository.ProjectRepository;
import com.codewithzea.projecttrackingsystem.util.MapperUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final String CACHE_NAME = "projects";
    private static final String ENTITY_TYPE = "Project";

    private final ProjectRepository projectRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Cacheable(value = CACHE_NAME, key = "#id")
    public ProjectDTO getProjectById(Long id) {
        log.debug("Fetching project with id: {}", id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_TYPE + " not found with id " + id));
        return MapperUtil.toProjectDTO(project);
    }

    public Page<ProjectDTO> getAllProjects(Pageable pageable) {
        log.debug("Fetching all projects with pagination: {}", pageable);
        return projectRepository.findAll(pageable)
                .map(MapperUtil::toProjectDTO);
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public ProjectDTO createProject(ProjectDTO dto) {
        log.debug("Creating new project with data: {}", dto);
        validateProjectDTO(dto);

        Project project = MapperUtil.toProject(dto);
        Project saved = projectRepository.save(project);

        log.info("Created new project with id: {}", saved.getId());
        auditProjectOperation("CREATE", saved);

        return MapperUtil.toProjectDTO(saved);
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#id")
    public ProjectDTO updateProject(Long id, ProjectDTO dto) {
        log.debug("Updating project with id: {} and data: {}", id, dto);
        validateProjectDTO(dto);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_TYPE + " not found with id " + id));

        updateProjectFromDTO(project, dto);
        Project updated = projectRepository.save(project);

        log.info("Updated project with id: {}", id);
        auditProjectOperation("UPDATE", updated);

        return MapperUtil.toProjectDTO(updated);
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#id")
    public void deleteProject(Long id) {
        log.debug("Deleting project with id: {}", id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_TYPE + " not found with id " + id));

        projectRepository.delete(project);
        log.info("Deleted project with id: {}", id);
        auditLogService.log("DELETE", ENTITY_TYPE, id.toString(), "");
    }

    public Page<ProjectDTO> findProjectsWithoutTasks(Pageable pageable) {
        log.debug("Finding projects without tasks (paginated)");
        return projectRepository.findProjectsWithoutTasks(pageable)
                .map(MapperUtil::toProjectDTO);
    }

    // Helper methods
    private void validateProjectDTO(ProjectDTO dto) {
        if (dto == null) {
            throw new BusinessValidationException("Project data cannot be null");
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessValidationException("Project name cannot be empty");
        }
        if (dto.getName().length() < 3 || dto.getName().length() > 100) {
            throw new BusinessValidationException("Project name must be between 3 and 100 characters");
        }
        if (dto.getDeadline() != null && dto.getDeadline().isBefore(java.time.LocalDate.now())) {
            throw new BusinessValidationException("Project deadline cannot be in the past");
        }
    }

    private void updateProjectFromDTO(Project project, ProjectDTO dto) {
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setDeadline(dto.getDeadline());
        project.setStatus(dto.getStatus());
    }

    private void auditProjectOperation(String operation, Project project) {
        try {
            String payload = objectMapper.writeValueAsString(project);
            auditLogService.log(operation, ENTITY_TYPE, project.getId().toString(), payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize project for audit logging", e);
            throw new AuditLogException("Failed to audit project operation");
        }
    }
}
