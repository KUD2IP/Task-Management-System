package org.example.authenticationservice.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.authenticationservice.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обработка исключения UserAlreadyExistsException
     *
     * @param ex исключение
     * @return HTTP-ответ с сообщением об ошибке
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<String> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.error("UserAlreadyExistsException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    /**
     * Обработка исключения RoleNotFoundException
     *
     * @param ex исключение
     * @return HTTP-ответ с сообщением об ошибке
     */
    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<String> handleRoleNotFoundException(RoleNotFoundException ex) {
        log.error("RoleNotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * Обработка исключения UserNotFoundException
     *
     * @param ex исключение
     * @return HTTP-ответ с сообщением об ошибке
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFoundException(UserNotFoundException ex) {
        log.error("UserNotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * Обработка исключения InvalidTokenException
     *
     * @param ex исключение
     * @return HTTP-ответ с сообщением об ошибке
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<String> handleInvalidTokenException(InvalidTokenException ex) {
        log.error("InvalidTokenException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    /**
     * Обработка исключения TokenSaveException
     *
     * @param ex исключение
     * @return HTTP-ответ с сообщением об ошибке
     */
    @ExceptionHandler(TokenSaveException.class)
    public ResponseEntity<String> handleTokenSaveException(TokenSaveException ex) {
        log.error("TokenSaveException: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }

    /**
     * Обработка исключения AuthenticationException
     *
     * @param ex исключение
     * @return HTTP-ответ с сообщением об ошибке
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> handleAuthenticationException(AuthenticationException ex) {
        log.error("AuthenticationException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    /**
     * Обработка остальных исключений
     *
     * @param ex исключение
     * @return HTTP-ответ с сообщением об ошибке
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + ex.getMessage());
    }
}