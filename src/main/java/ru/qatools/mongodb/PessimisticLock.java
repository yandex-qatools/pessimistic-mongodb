package ru.qatools.mongodb;

import java.util.concurrent.TimeUnit;

/**
 * @author Ilya Sadykov
 */
public interface PessimisticLock {
    /**
     * Lock current lock
     */
    void lock();

    /**
     * Forcing unlock
     */
    void forceUnlock();

    /**
     * Returns true if lock is locked by me
     */
    boolean isLockedByMe();

    /**
     * Returns true if lock is not available
     */
    boolean isLocked();

    /**
     * Acquires the lock unless the current thread is
     * {@linkplain Thread#interrupt interrupted}.
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * Acquires the lock only if it is free at the time of invocation.
     */
    boolean tryLock();

    /**
     * Acquires the lock if it is free within the given waiting time and the
     * current thread has not been {@linkplain Thread#interrupt interrupted}.
     */
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
}
