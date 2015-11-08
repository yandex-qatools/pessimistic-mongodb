package ru.qatools.mongodb.error;

/**
 * @author Ilya Sadykov
 */
public class LockNotAvailableException extends PessimisticException {
    public LockNotAvailableException(String message) {
        super(message);
    }

    public LockNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
