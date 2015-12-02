package ru.qatools.mongodb;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.currentTimeMillis;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Ilya Sadykov
 */
public class MongoPessimisticLockTest extends MongoBasicTest {

    @Test
    public void testLock() throws Exception {
        MongoPessimisticLock lock = new MongoPessimisticLock(createLocking());
        lock.lock();
        final AtomicBoolean lockAvb = new AtomicBoolean();
        final Thread t = new Thread(() -> lockAvb.set(lock.tryLock()));
        t.start();
        t.join();
        assertThat(lockAvb.get(), is(false));
    }

    @Test
    public void testLockAndWait() throws Exception {
        MongoPessimisticLock lock = new MongoPessimisticLock(createLocking());
        lock.lock();
        final Thread t = new Thread(lock::lock);
        t.start();
        final long startedTime = currentTimeMillis();
        t.join(1000);
        assertThat(currentTimeMillis() - startedTime, greaterThanOrEqualTo(1000L));
    }

    @Test
    public void testLockAndWaitWithSameLockName() throws Exception {
        final MongoPessimisticLocking locking = createLocking();
        MongoPessimisticLock lock1 = new MongoPessimisticLock(locking, "lockname");
        MongoPessimisticLock lock2 = new MongoPessimisticLock(locking, "lockname");
        lock1.lock();
        final Thread t = new Thread(lock2::lock);
        t.start();
        final long startedTime = currentTimeMillis();
        t.join(1000);
        assertThat(currentTimeMillis() - startedTime, greaterThanOrEqualTo(1000L));
    }

    @Test
    public void testForceUnlock() throws Exception {
        MongoPessimisticLock lock = new MongoPessimisticLock(createLocking());
        lock.lock();
        final Thread t = new Thread(() -> {
            lock.forceUnlock();
            lock.lock();
        });
        t.start();
        t.join();
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isLockedByMe(), is(false));
    }
}