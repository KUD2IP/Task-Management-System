package org.example.taskservice.exeception;

public class InvalidTaskDataException extends RuntimeException {

    public InvalidTaskDataException(String message) {
        super(message);
    }
}
