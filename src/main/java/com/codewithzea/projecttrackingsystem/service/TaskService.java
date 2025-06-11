package com.codewithzea.projecttrackingsystem.service;

import com.codewithzea.projecttrackingsystem.dto.TaskDTO;
import com.codewithzea.projecttrackingsystem.model.Developer;
import com.codewithzea.projecttrackingsystem.model.Project;
import com.codewithzea.projecttrackingsystem.model.Task;
import com.codewithzea.projecttrackingsystem.exception.ResourceNotFoundException;
import com.codewithzea.projecttrackingsystem.exception.BusinessValidationException;
import com.codewithzea.projecttrackingsystem.repository.DeveloperRepository;
import com.codewithzea.projecttrackingsystem.repository.ProjectRepository;
import com.codewithzea.projecttrackingsystem.repository.TaskRepository;
import com.codewithzea.projecttrackingsystem.util.MapperUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private static final Set<String> VALID_STATUSES = Set.of(
            "NOT_STARTED", "IN_PROGRESS", "COMPLETED", "BLOCKED"
    );

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final DeveloperRepository developerRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public Page<TaskDTO> getAllTasks(Pageable pageable) {
        log.debug("Fetching all tasks with pagination: {}", pageable);
        return taskRepository.findAll(pageable)
                .map(MapperUtil::toTaskDTO);
    }

    public TaskDTO getTaskById(Long id) {
        log.debug("Fetching task with id: {}", id);
        Task task = findTaskByIdOrThrow(id);
        return MapperUtil.toTaskDTO(task);
    }

    public List<TaskDTO> getTasksByProjectId(Long projectId) {
        log.debug("Fetching tasks for project id: {}", projectId);
        validateProjectExists(projectId);
        return taskRepository.findByProjectId(projectId).stream()
                .map(MapperUtil::toTaskDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getTasksByDeveloperId(Long developerId) {
        log.debug("Fetching tasks for developer id: {}", developerId);
        validateDeveloperExists(developerId);
        return taskRepository.findByDeveloperId(developerId).stream()
                .map(MapperUtil::toTaskDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getOverdueTasks() {
        log.debug("Fetching overdue tasks");
        LocalDate today = LocalDate.now();
        return taskRepository.findOverdueTasks(today).stream()
                .map(MapperUtil::toTaskDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskDTO createTask(TaskDTO dto) {
        log.debug("Creating new task with data: {}", dto);
        validateTaskDTO(dto);

        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + dto.getProjectId()));

        Task task = buildTaskFromDTO(dto, project);
        assignDevelopersToTask(dto, task);

        Task savedTask = taskRepository.save(task);
        log.info("Created new task with id: {}", savedTask.getId());

        auditTaskCreation(savedTask);
        return MapperUtil.toTaskDTO(savedTask);
    }

    @Transactional
    public TaskDTO updateTask(Long id, TaskDTO dto) {
        log.debug("Updating task with id: {} and data: {}", id, dto);
        validateTaskDTO(dto);

        Task task = findTaskByIdOrThrow(id);
        updateTaskFromDTO(dto, task);

        Task updatedTask = taskRepository.save(task);
        log.info("Updated task with id: {}", id);

        auditTaskUpdate(updatedTask);
        return MapperUtil.toTaskDTO(updatedTask);
    }

    @Transactional
    public void deleteTask(Long id, String actorName) {
        log.debug("Deleting task with id: {}", id);
        Task task = findTaskByIdOrThrow(id);
        taskRepository.delete(task);
        log.info("Deleted task with id: {}", id);
        auditLogService.log("DELETE", "Task", id.toString(), actorName, "");
    }

    public Map<String, Long> getTaskCountsByStatus() {
        log.debug("Fetching task counts by status");
        return taskRepository.countTasksGroupedByStatus().stream()
                .collect(Collectors.toMap(
                        obj -> (String) obj[0],
                        obj -> (Long) obj[1]
                ));
    }

    // Helper methods
    private Task findTaskByIdOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
    }

    private void validateProjectExists(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id " + projectId);
        }
    }

    private void validateDeveloperExists(Long developerId) {
        if (!developerRepository.existsById(developerId)) {
            throw new ResourceNotFoundException("Developer not found with id " + developerId);
        }
    }

    private void validateTaskDTO(TaskDTO dto) {
        if (dto.getDueDate() != null && dto.getDueDate().isBefore(LocalDate.now())) {
            throw new BusinessValidationException("Due date cannot be in the past");
        }

        if (dto.getStatus() != null && !VALID_STATUSES.contains(dto.getStatus())) {
            throw new BusinessValidationException("Invalid task status: " + dto.getStatus());
        }
    }

    private Task buildTaskFromDTO(TaskDTO dto, Project project) {
        return Task.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .dueDate(dto.getDueDate())
                .project(project)
                .build();
    }

    private void assignDevelopersToTask(TaskDTO dto, Task task) {
        if (dto.getAssignedDeveloperIds() != null && !dto.getAssignedDeveloperIds().isEmpty()) {
            Set<Developer> developers = dto.getAssignedDeveloperIds().stream()
                    .map(devId -> developerRepository.findById(devId)
                            .orElseThrow(() -> new ResourceNotFoundException("Developer not found with id " + devId)))
                    .collect(Collectors.toSet());
            task.setAssignedDevelopers(developers);
        }
    }

    private void updateTaskFromDTO(TaskDTO dto, Task task) {
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        task.setDueDate(dto.getDueDate());

        if (!task.getProject().getId().equals(dto.getProjectId())) {
            Project project = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + dto.getProjectId()));
            task.setProject(project);
        }

        assignDevelopersToTask(dto, task);
    }

    private void auditTaskCreation(Task task) {
        try {
            String payload = objectMapper.writeValueAsString(task);
            auditLogService.log("CREATE", "Task", task.getId().toString(), payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize task for audit logging", e);
        }
    }

    private void auditTaskUpdate(Task task) {
        try {
            String payload = objectMapper.writeValueAsString(task);
            auditLogService.log("UPDATE", "Task", task.getId().toString(), payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize task for audit logging", e);
        }
    }
}