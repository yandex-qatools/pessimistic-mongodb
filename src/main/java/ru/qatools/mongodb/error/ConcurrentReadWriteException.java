package ru.qatools.mongodb.error;

/**
 * @author Ilya Sadykov
 */
public class ConcurrentReadWriteException extends PessimisticException {
    public ConcurrentReadWriteException(String message) {
        super(message);
    }

    public ConcurrentReadWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
