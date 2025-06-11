package com.codewithzea.projecttrackingsystem.repository;





import com.codewithzea.projecttrackingsystem.model.Task;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Find all tasks by project id
    List<Task> findByProjectId(Long projectId);

    // Find all tasks assigned to a developer
    @Query("SELECT t FROM Task t JOIN t.assignedDevelopers d WHERE d.id = :developerId")
    List<Task> findByDeveloperId(@Param("developerId") Long developerId);

    // Find overdue tasks
    @Query("SELECT t FROM Task t WHERE t.dueDate < :currentDate AND t.status <> 'COMPLETED'")
    List<Task> findOverdueTasks(@Param("currentDate") LocalDate currentDate);

    // Task counts grouped by status
    @Query("SELECT t.status as status, COUNT(t) as count FROM Task t GROUP BY t.status")
    List<Object[]> countTasksGroupedByStatus();
}
