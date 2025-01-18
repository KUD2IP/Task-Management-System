package org.example.taskservice.repository;

import org.example.taskservice.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    // Найти задачи для конкретного исполнителя
    @Query("SELECT t FROM Task t JOIN t.executors a WHERE a.id = :userId")
    Page<Task> findByAssigneeId(@Param("userId") Long userId, Pageable pageable);

    Page<Task> findByAuthorId(Long userId, Pageable pageable);

    Optional<Task> findByName(String name);

    Page<Task> findByPriority(String priority, Pageable pageable);

    Page<Task> findByStatus(String status, Pageable pageable);

    Page<Task> findByStatusAndPriority(String s, String s1, Pageable pageable);

    Page<Task> findByExecutorsId(Long userId, Pageable pageable);

}
