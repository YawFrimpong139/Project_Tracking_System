package com.codewithzea.projecttrackingsystem.repository;





import com.codewithzea.projecttrackingsystem.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Fetch projects without any tasks
    @Query("SELECT p FROM Project p WHERE p.tasks IS EMPTY")
    Page<Project> findProjectsWithoutTasks(Pageable pageable);
}

