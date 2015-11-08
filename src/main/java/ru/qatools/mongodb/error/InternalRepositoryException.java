package ru.qatools.mongodb.error;

/**
 * @author Ilya Sadykov
 */
public class InternalRepositoryException extends PessimisticException {
    public InternalRepositoryException(String message) {
        super(message);
    }

    public InternalRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
