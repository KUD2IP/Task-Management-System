package org.example.taskservice.service;

import org.example.taskservice.entity.Comment;
import org.springframework.data.jpa.domain.Specification;

public class CommentSpecification {

    /**
     * Фильтр по идентификатору задачи
     *
     * @param taskId - идентификатор задачи
     * @return - спецификация
     */
    public static Specification<Comment> hasTaskId(Long taskId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("task").get("id"), taskId);
    }
}
