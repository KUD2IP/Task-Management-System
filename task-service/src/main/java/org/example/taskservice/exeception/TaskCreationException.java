package org.example.taskservice.exeception;

public class TaskCreationException extends RuntimeException {

    public TaskCreationException(String message) {
        super(message);
    }
}
