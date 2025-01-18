package org.example.taskservice.service;

import org.example.taskservice.entity.Task;
import org.example.taskservice.entity.TaskPriority;
import org.example.taskservice.entity.TaskStatus;
import org.springframework.data.jpa.domain.Specification;

public class TaskSpecification {
    public static Specification<Task> hasAuthorId(Long authorId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("author").get("id"), authorId);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Task> hasPriority(TaskPriority priority) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("priority"), priority);
    }
}
