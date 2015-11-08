package ru.qatools.mongodb.error;

/**
 * @author Ilya Sadykov
 */
public class InvalidLockOwnerException extends PessimisticException {
    public InvalidLockOwnerException(String message) {
        super(message);
    }

    public InvalidLockOwnerException(String message, Throwable cause) {
        super(message, cause);
    }
}
