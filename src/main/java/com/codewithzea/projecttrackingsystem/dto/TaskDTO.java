package com.codewithzea.projecttrackingsystem.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Task Data Transfer Object")
public class TaskDTO {

    @Schema(description = "Unique identifier of the task", example = "1")
    private Long id;

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    @Schema(description = "Title of the task", example = "Implement authentication", required = true)
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Schema(description = "Detailed description of the task", example = "Implement JWT based authentication")
    private String description;

    @NotBlank(message = "Status cannot be blank")
    @Schema(description = "Current status of the task",
            example = "IN_PROGRESS",
            allowableValues = {"NOT_STARTED", "IN_PROGRESS", "COMPLETED", "BLOCKED"})
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "Due date cannot be null")
    @Schema(description = "Due date of the task", example = "2023-12-31", required = true)
    private LocalDate dueDate;

    @NotNull(message = "Project ID cannot be null")
    @Schema(description = "ID of the project this task belongs to", example = "1", required = true)
    private Long projectId;

    @Builder.Default
    @Schema(description = "Set of developer IDs assigned to this task")
    private Set<Long> assignedDeveloperIds = Collections.emptySet();

    // Custom validation method could be added here if needed
    public boolean isValidStatus() {
        return Set.of("NOT_STARTED", "IN_PROGRESS", "COMPLETED", "BLOCKED").contains(status);
    }
}