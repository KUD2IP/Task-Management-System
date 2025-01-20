package org.example.taskservice.service;

import org.example.taskservice.entity.Task;
import org.example.taskservice.entity.TaskPriority;
import org.example.taskservice.entity.TaskStatus;
import org.springframework.data.jpa.domain.Specification;

public class TaskSpecification {

    /**
     * Фильтр по идентификатору автора
     *
     * @param authorId - идентификатор автора
     * @return - спецификация
     */
    public static Specification<Task> hasAuthorId(Long authorId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("author").get("id"), authorId);
    }

    /**
     * Фильтр по статусу
     *
     * @param status - статус
     * @return - спецификация
     */
    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }

    /**
     * Фильтр по приоритету
     *
     * @param priority - приоритет
     * @return - спецификация
     */
    public static Specification<Task> hasPriority(TaskPriority priority) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("priority"), priority);
    }

    /**
     * Фильтр по идентификатору исполнителя
     *
     * @param executorId - идентификатор исполнителя
     * @return - спецификация
     */
    public static Specification<Task> hasExecutorId(Long executorId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("executors").get("id"), executorId);
    }
}
