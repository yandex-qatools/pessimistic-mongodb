package ru.qatools.mongodb.error;

/**
 * @author Ilya Sadykov
 */
public class LockWaitTimeoutException extends PessimisticException {
    public LockWaitTimeoutException(String message) {
        super(message);
    }

    public LockWaitTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
