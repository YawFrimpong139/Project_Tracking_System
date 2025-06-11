package com.codewithzea.projecttrackingsystem.servicetest;

import com.codewithzea.projecttrackingsystem.dto.TaskDTO;
import com.codewithzea.projecttrackingsystem.model.*;
import com.codewithzea.projecttrackingsystem.service.*;
import com.codewithzea.projecttrackingsystem.exception.*;
import com.codewithzea.projecttrackingsystem.repository.*;
import com.codewithzea.projecttrackingsystem.util.MapperUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private DeveloperRepository developerRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private TaskService taskService;

    private Task task;
    private TaskDTO taskDTO;
    private Project project;
    private Developer developer;

    @BeforeEach
    void setUp() {
        project = Project.builder()
                .id(1L)
                .name("Test Project")
                .build();

        developer = Developer.builder()
                .id(1L)
                .name("John Doe")
                .build();

        task = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status("IN_PROGRESS")
                .dueDate(LocalDate.now().plusDays(7))
                .project(project)
                .assignedDevelopers(new HashSet<>(Set.of(developer)))
                .build();

        taskDTO = TaskDTO.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status("IN_PROGRESS")
                .dueDate(LocalDate.now().plusDays(7))
                .projectId(1L)
                .assignedDeveloperIds(new HashSet<>(Set.of(1L)))
                .build();
    }

    @Nested
    @DisplayName("Get Tasks Tests")
    class GetTasksTests {
        @Test
        @DisplayName("Should return page of tasks")
        void getAllTasks_ShouldReturnPageOfTasks() {
            Pageable pageable = PageRequest.of(0, 10);
            when(taskRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(task)));

            Page<TaskDTO> result = taskService.getAllTasks(pageable);

            assertEquals(1, result.getTotalElements());
            assertEquals("Test Task", result.getContent().get(0).getTitle());
        }

        @Test
        @DisplayName("Should return task when exists")
        void getTaskById_WhenTaskExists_ShouldReturnTask() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

            TaskDTO result = taskService.getTaskById(1L);

            assertNotNull(result);
            assertEquals("Test Task", result.getTitle());
        }

        @Test
        @DisplayName("Should throw exception when task not found")
        void getTaskById_WhenTaskNotExists_ShouldThrowException() {
            when(taskRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> taskService.getTaskById(1L));
        }

        @Test
        @DisplayName("Should return tasks by project ID")
        void getTasksByProjectId_ShouldReturnListOfTasks() {
            when(projectRepository.existsById(1L)).thenReturn(true);
            when(taskRepository.findByProjectId(1L)).thenReturn(List.of(task));

            List<TaskDTO> result = taskService.getTasksByProjectId(1L);

            assertEquals(1, result.size());
            assertEquals("Test Task", result.get(0).getTitle());
            verify(projectRepository).existsById(1L);
        }

        @Test
        @DisplayName("Should throw exception when project not found")
        void getTasksByProjectId_WhenProjectNotExists_ShouldThrowException() {
            when(projectRepository.existsById(1L)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class,
                    () -> taskService.getTasksByProjectId(1L));
        }

        @Test
        @DisplayName("Should return tasks by developer ID")
        void getTasksByDeveloperId_ShouldReturnListOfTasks() {

            when(developerRepository.existsById(1L)).thenReturn(true);

            when(taskRepository.findByDeveloperId(1L)).thenReturn(List.of(task));

            List<TaskDTO> result = taskService.getTasksByDeveloperId(1L);

            assertEquals(1, result.size());
            assertEquals("Test Task", result.get(0).getTitle());
        }

        @Test
        @DisplayName("Should return overdue tasks")
        void getOverdueTasks_ShouldReturnListOfOverdueTasks() {
            LocalDate today = LocalDate.now();
            Task overdueTask = Task.builder()
                    .id(2L)
                    .title("Overdue Task")
                    .dueDate(today.minusDays(1))
                    .status("IN_PROGRESS")
                    .build();

            when(taskRepository.findOverdueTasks(any(LocalDate.class)))
                    .thenReturn(List.of(overdueTask));

            List<TaskDTO> result = taskService.getOverdueTasks();

            assertEquals(1, result.size());
            assertEquals("Overdue Task", result.get(0).getTitle());
            verify(taskRepository).findOverdueTasks(today);
        }
    }

    @Nested
    @DisplayName("Create Task Tests")
    class CreateTaskTests {
        @Test
        @DisplayName("Should create task with valid data")
        void createTask_WithValidData_ShouldReturnCreatedTask() throws Exception {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(developerRepository.findById(1L)).thenReturn(Optional.of(developer));
            when(taskRepository.save(any(Task.class))).thenReturn(task);
            when(objectMapper.writeValueAsString(any())).thenReturn("serialized-task");

            TaskDTO result = taskService.createTask(taskDTO);

            assertNotNull(result);
            assertEquals("Test Task", result.getTitle());
            verify(auditLogService).log("CREATE", "Task", "1", "serialized-task");
        }

        @Test
        @DisplayName("Should throw exception when project not found")
        void createTask_WithInvalidProjectId_ShouldThrowException() {
            when(projectRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> taskService.createTask(taskDTO));
        }

        @Test
        @DisplayName("Should throw exception when developer not found")
        void createTask_WithInvalidDeveloperId_ShouldThrowException() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(developerRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> taskService.createTask(taskDTO));
        }


        @Test
        @DisplayName("Should skip audit logging when JSON processing fails")
        void createTask_WhenJsonProcessingFails_ShouldSkipAuditLogging() throws Exception {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(developerRepository.findById(1L)).thenReturn(Optional.of(developer));
            when(taskRepository.save(any(Task.class))).thenReturn(task);
            when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);

            TaskDTO result = taskService.createTask(taskDTO);

            assertNotNull(result);
            verify(auditLogService, never()).log(any(), any(), any(), any());
        }

//        @Test
//        @DisplayName("Should throw exception when JSON processing fails")
//        void createTask_WhenJsonProcessingFails_ShouldThrowException() throws Exception {
//            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
//            when(developerRepository.findById(1L)).thenReturn(Optional.of(developer));
//            when(taskRepository.save(any(Task.class))).thenReturn(task);
//            when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
//
//            assertThrows(Exception.class, () -> taskService.createTask(taskDTO));
//        }
    }

    @Nested
    @DisplayName("Update Task Tests")
    class UpdateTaskTests {
        @Test
        @DisplayName("Should update task with valid data")
        void updateTask_WithValidData_ShouldReturnUpdatedTask() throws Exception {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
            when(developerRepository.findById(1L)).thenReturn(Optional.of(developer)); // ✅ This is missing
            when(taskRepository.save(any(Task.class))).thenReturn(task);
            when(objectMapper.writeValueAsString(any())).thenReturn("serialized-task");

            // Act
            TaskDTO result = taskService.updateTask(1L, taskDTO);

            // Assert
            assertNotNull(result);
            assertEquals("Test Task", result.getTitle());
            verify(auditLogService).log("UPDATE", "Task", "1", "serialized-task");

            verify(taskRepository).findById(1L);
            verify(developerRepository).findById(1L); // ✅ This will now be hit
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("Should throw exception when task not found")
        void updateTask_WithInvalidTaskId_ShouldThrowException() {
            when(taskRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> taskService.updateTask(1L, taskDTO));
        }
    }

    @Nested
    @DisplayName("Delete Task Tests")
    class DeleteTaskTests {
        @Test
        @DisplayName("Should delete task with valid ID")
        void deleteTask_WithValidId_ShouldDeleteTask() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

            taskService.deleteTask(1L, "admin");

            verify(taskRepository).delete(task);
            verify(auditLogService).log("DELETE", "Task", "1", "admin", "");
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {
        @Test
        @DisplayName("Should return task counts by status")
        void getTaskCountsByStatus_ShouldReturnStatusCounts() {
            List<Object[]> statusCounts = List.of(
                    new Object[]{"IN_PROGRESS", 5L},
                    new Object[]{"COMPLETED", 3L}
            );

            when(taskRepository.countTasksGroupedByStatus()).thenReturn(statusCounts);

            Map<String, Long> result = taskService.getTaskCountsByStatus();

            assertEquals(2, result.size());
            assertEquals(5L, result.get("IN_PROGRESS"));
            assertEquals(3L, result.get("COMPLETED"));
        }
    }
}