package com.codewithzea.projecttrackingsystem.service;




import com.codewithzea.projecttrackingsystem.dto.TaskDTO;
import com.codewithzea.projecttrackingsystem.model.Developer;
import com.codewithzea.projecttrackingsystem.model.Project;
import com.codewithzea.projecttrackingsystem.model.Task;
import com.codewithzea.projecttrackingsystem.exception.ResourceNotFoundException;
import com.codewithzea.projecttrackingsystem.repository.DeveloperRepository;
import com.codewithzea.projecttrackingsystem.repository.ProjectRepository;
import com.codewithzea.projecttrackingsystem.repository.TaskRepository;
import com.codewithzea.projecttrackingsystem.util.MapperUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final DeveloperRepository developerRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;


    public Page<TaskDTO> getALlTasks(Pageable pageable){
        return taskRepository.findAll(pageable).map(MapperUtil::toTaskDTO);
    }
    public TaskDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
        return MapperUtil.toTaskDTO(task);
    }

    public List<TaskDTO> getTasksByProjectId(Long projectId) {
        return taskRepository.findByProjectId(projectId).stream()
                .map(MapperUtil::toTaskDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getTasksByDeveloperId(Long developerId) {
        return taskRepository.findByDeveloperId(developerId).stream()
                .map(MapperUtil::toTaskDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getOverdueTasks() {
        return taskRepository.findOverdueTasks().stream()
                .map(MapperUtil::toTaskDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskDTO createTask(TaskDTO dto) throws Exception {
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + dto.getProjectId()));

        Task task = Task.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .dueDate(dto.getDueDate())
                .project(project)
                .build();

        if (dto.getAssignedDeveloperIds() != null && !dto.getAssignedDeveloperIds().isEmpty()) {
            Set<Developer> developers = new HashSet<>();
            for (Long devId : dto.getAssignedDeveloperIds()) {
                Developer dev = developerRepository.findById(devId)
                        .orElseThrow(() -> new ResourceNotFoundException("Developer not found with id " + devId));
                developers.add(dev);
            }
            task.setAssignedDevelopers(developers);
        }

        Task saved = taskRepository.save(task);
        String payload = objectMapper.writeValueAsString(saved);
        auditLogService.log("CREATE", "Task", saved.getId().toString(), payload);
        return MapperUtil.toTaskDTO(saved);
    }

    @Transactional
    public TaskDTO updateTask(Long id, TaskDTO dto) throws Exception {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        task.setDueDate(dto.getDueDate());

        if (!task.getProject().getId().equals(dto.getProjectId())) {
            Project project = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + dto.getProjectId()));
            task.setProject(project);
        }

        if (dto.getAssignedDeveloperIds() != null) {
            Set<Developer> developers = new HashSet<>();
            for (Long devId : dto.getAssignedDeveloperIds()) {
                Developer dev = developerRepository.findById(devId)
                        .orElseThrow(() -> new ResourceNotFoundException("Developer not found with id " + devId));
                developers.add(dev);
            }
            task.setAssignedDevelopers(developers);
        }

        Task updated = taskRepository.save(task);
        String payload = objectMapper.writeValueAsString(updated);
        auditLogService.log("UPDATE", "Task", updated.getId().toString(), payload);
        return MapperUtil.toTaskDTO(updated);
    }

    @Transactional
    public void deleteTask(Long id, String actorName) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
        taskRepository.delete(task);
        auditLogService.log("DELETE", "Task", id.toString(), actorName, "");
    }

    public Map<String, Long> getTaskCountsByStatus() {
        List<Object[]> counts = taskRepository.countTasksGroupedByStatus();
        Map<String, Long> result = new HashMap<>();
        for (Object[] obj : counts) {
            String status = (String) obj[0];
            Long count = (Long) obj[1];
            result.put(status, count);
        }
        return result;
    }
}

