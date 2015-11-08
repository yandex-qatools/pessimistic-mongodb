package ru.qatools.mongodb.error;

/**
 * @author Ilya Sadykov
 */
public class PessimisticException extends RuntimeException {
    public PessimisticException(String message) {
        super(message);
    }

    public PessimisticException(String message, Throwable cause) {
        super(message, cause);
    }
}
