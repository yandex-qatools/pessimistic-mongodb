package ru.qatools.mongodb;

import ru.qatools.mongodb.error.InvalidLockOwnerException;
import ru.qatools.mongodb.error.LockWaitTimeoutException;

/**
 * @author Ilya Sadykov
 */
public interface PessimisticLocking {
    void tryLock(String key, long timeoutMs) throws LockWaitTimeoutException;

    void unlock(String key) throws InvalidLockOwnerException;

    void forceUnlock(String key);

    boolean isLocked(String key);

    boolean isLockedByMe(String key);
}
