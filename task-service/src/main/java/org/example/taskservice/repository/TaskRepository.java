package org.example.taskservice.repository;

import org.example.taskservice.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    // Найти задачи для конкретного исполнителя
    @Query("SELECT t FROM Task t JOIN t.executors a WHERE a.id = :userId")
    Page<Task> findByAssigneeId(@Param("userId") Long userId, Pageable pageable);

    Page<Task> findByAuthorId(Long authorId, Pageable pageable);

   Optional<Task> findByName(String name);

   Optional<Task> findByPriority(String priority);

   Optional<Task> findByStatus(String status);

}
