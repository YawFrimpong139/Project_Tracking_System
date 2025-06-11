package com.codewithzea.projecttrackingsystem.util;

import com.codewithzea.projecttrackingsystem.dto.*;
import com.codewithzea.projecttrackingsystem.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class MapperUtil {

    // Task mapping methods
    public static TaskDTO toTaskDTO(Task task) {
        if (task == null) return null;

        return TaskDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .dueDate(task.getDueDate())
                .projectId(getProjectId(task))
                .assignedDeveloperIds(getDeveloperIds(task))
                .build();
    }

    private static Long getProjectId(Task task) {
        return Optional.ofNullable(task.getProject())
                .map(Project::getId)
                .orElse(null);
    }

    private static Set<Long> getDeveloperIds(Task task) {
        return Optional.ofNullable(task.getAssignedDevelopers())
                .map(developers -> developers.stream()
                        .map(Developer::getId)
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
    }

    // Project mapping methods
    public static ProjectDTO toProjectDTO(Project project) {
        if (project == null) return null;

        return ProjectDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .deadline(project.getDeadline())
                .status(project.getStatus())
                .build();
    }

    public static Project toProject(ProjectDTO dto) {
        if (dto == null) return null;

        return Project.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .deadline(dto.getDeadline())
                .status(dto.getStatus())
                .build();
    }

    // Developer mapping methods
    public static DeveloperDTO toDeveloperDTO(Developer dev) {
        if (dev == null) return null;

        return DeveloperDTO.builder()
                .id(dev.getId())
                .name(dev.getName())
                .email(dev.getEmail())
                .skills(dev.getSkills())
                .build();
    }

    public static Developer toDeveloper(DeveloperDTO dto) {
        if (dto == null) return null;

        return Developer.builder()
                .id(dto.getId())
                .name(dto.getName())
                .email(dto.getEmail())
                .skills(dto.getSkills())
                .build();
    }
}