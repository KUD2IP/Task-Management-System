package org.example.taskservice.exeception;

public class InvalidCommentDataException extends RuntimeException {

    public InvalidCommentDataException(String message) {
        super(message);
    }
}
