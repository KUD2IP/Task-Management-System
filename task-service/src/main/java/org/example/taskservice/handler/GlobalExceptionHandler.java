package org.example.taskservice.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.taskservice.exeception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обработка исключения AccessDeniedException
     * @param ex исключение
     * @return 403 Forbidden, если доступ к ресурсу запрещен.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    /**
     * Обработка исключения CommentNotFoundException
     * @param ex исключение
     * @return 404 Not Found, если комментарий не найден.
     */
    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<String> handleCommentNotFound(CommentNotFoundException ex) {
        log.error("Comment not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * Обработка исключения ExecutorAssignmentException
     * @param ex исключение
     * @return 400 Bad Request, если возникла ошибка назначения исполнителя.
     */
    @ExceptionHandler(ExecutorAssignmentException.class)
    public ResponseEntity<String> handleExecutorAssignment(ExecutorAssignmentException ex) {
        log.error("Error assigning executor: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    /**
     * Обработка исключения InvalidTaskDataException
     * @param ex исключение
     * @return 400 Bad Request, если данные задачи некорректны.
     */
    @ExceptionHandler(InvalidTaskDataException.class)
    public ResponseEntity<String> handleInvalidTaskData(InvalidTaskDataException ex) {
        log.error("Invalid task data: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    /**
     * Обработка исключения InvalidCommentDataException
     * @param ex исключение
     * @return 400 Bad Request, если данные задачи некорректны.
     */
    @ExceptionHandler(InvalidCommentDataException.class)
    public ResponseEntity<String> handleInvalidCommentData(InvalidCommentDataException ex) {
        log.error("Invalid comment data: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    /**
     * Обработка исключения TaskCreationException
     * @param ex исключение
     * @return 500 Internal Server Error, если задача не может быть создана.
     */
    @ExceptionHandler(TaskCreationException.class)
    public ResponseEntity<String> handleTaskCreation(TaskCreationException ex) {
        log.error("Error creating task: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }

    /**
     * Обработка исключения TaskNotFoundException
     * @param ex исключение
     * @return 404 Not Found, если задача не найдена.
     */
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<String> handleTaskNotFound(TaskNotFoundException ex) {
        log.error("Task not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * Обработка исключения TaskUpdateException
     * @param ex исключение
     * @return 400 Bad Request, если возникла ошибка при обновлении задачи.
     */
    @ExceptionHandler(TaskUpdateException.class)
    public ResponseEntity<String> handleTaskUpdate(TaskUpdateException ex) {
        log.error("Error updating task: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    /**
     * Обработка исключения UserAlreadyExistsException
     * @param ex исключение
     * @return 409 Conflict, если пользователь уже существует.
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<String> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.error("User already exists: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    /**
     * Обработка исключения UserNotFoundException
     * @param ex исключение
     * @return 404 Not Found, если пользователь не найден.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(UserNotFoundException ex) {
        log.error("User not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * Обработка всех остальных исключений
     * @param ex исключение
     * @return 500 Internal Server Error для неожиданных ошибок.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred. Please try again later.");
    }
}
