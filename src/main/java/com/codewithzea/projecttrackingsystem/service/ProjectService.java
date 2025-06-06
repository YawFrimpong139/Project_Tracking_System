package com.codewithzea.projecttrackingsystem.service;



import com.codewithzea.projecttrackingsystem.dto.ProjectDTO;
import com.codewithzea.projecttrackingsystem.model.Project;
import com.codewithzea.projecttrackingsystem.exception.ResourceNotFoundException;
import com.codewithzea.projecttrackingsystem.repository.ProjectRepository;
import com.codewithzea.projecttrackingsystem.util.MapperUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;




@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Cacheable(value = "projects", key = "#id")
    public ProjectDTO getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + id));
        return MapperUtil.toProjectDTO(project);
    }

    public Page<ProjectDTO> getAllProjects(Pageable pageable) {
        return projectRepository.findAll(pageable).map(MapperUtil::toProjectDTO);
    }

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public ProjectDTO createProject(ProjectDTO dto) throws Exception {
        Project project = MapperUtil.toProject(dto);
        Project saved = projectRepository.save(project);
        String payload = objectMapper.writeValueAsString(saved);
        auditLogService.log("CREATE", "Project", saved.getId().toString(), payload);
        return MapperUtil.toProjectDTO(saved);
    }

    @Transactional
    @CacheEvict(value = "projects", key = "#id")
    public ProjectDTO updateProject(Long id, ProjectDTO dto) throws Exception {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + id));

        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setDeadline(dto.getDeadline());
        project.setStatus(dto.getStatus());

        Project updated = projectRepository.save(project);
        String payload = objectMapper.writeValueAsString(updated);
        auditLogService.log("UPDATE", "Project", updated.getId().toString(), payload);
        return MapperUtil.toProjectDTO(updated);
    }

    @Transactional
    @CacheEvict(value = "projects", key = "#id")
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + id));
        projectRepository.delete(project);
        auditLogService.log("DELETE", "Project", id.toString(), "");
    }

    public List<ProjectDTO> findProjectsWithoutTasks() {
        return projectRepository.findProjectsWithoutTasks().stream()
                .map(MapperUtil::toProjectDTO)
                .collect(Collectors.toList());
    }
}



