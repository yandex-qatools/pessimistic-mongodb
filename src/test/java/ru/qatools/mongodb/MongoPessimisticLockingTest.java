package ru.qatools.mongodb;

import org.junit.Test;
import ru.qatools.mongodb.error.InvalidLockOwnerException;
import ru.qatools.mongodb.error.LockWaitTimeoutException;

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

/**
 * @author Ilya Sadykov
 */
public class MongoPessimisticLockingTest extends MongoBasicTest {

    @Test
    public void testLockAndUnlock() throws Exception {
        final MongoPessimisticLocking lock = createLocking();
        lock.tryLock("someKey", 100L);
        assertThat(lock.isLocked("someKey"), is(true));
        assertThat(lock.isLockedByMe("someKey"), is(true));
        lock.unlock("someKey");
        assertThat(lock.isLocked("someKey"), is(false));
        assertThat(lock.isLockedByMe("someKey"), is(false));
        forceLockInSeparateThread("someKey");
        assertThat(lock.isLocked("someKey"), is(true));
        assertThat(lock.isLockedByMe("someKey"), is(false));
    }

    @Test
    public void testTryLockWithOtherThread() throws Exception {
        final AtomicLong waitedMs = new AtomicLong();
        final MongoPessimisticLocking lock = createLocking();
        lock.tryLock("key1", 100L);
        final Thread otherThread = new Thread(() -> {
            final MongoPessimisticLocking otherLock = createLocking();
            long startedWaitTime = currentTimeMillis();
            otherLock.tryLock("key1", 3000L);
            waitedMs.set(currentTimeMillis() - startedWaitTime);
        });
        otherThread.start();
        sleep(1100L);
        lock.unlock("key1");
        otherThread.join();
        assertThat(waitedMs.get(), allOf(greaterThan(1000L), lessThan(2000L)));
    }

    @Test(expected = LockWaitTimeoutException.class)
    public void testTimeoutForOtherThread() throws Exception {
        forceLockInSeparateThread("someKey");
        createLocking().tryLock("someKey", 500L);
    }

    @Test(expected = InvalidLockOwnerException.class)
    public void testInvalidLockOwner() throws Exception {
        final PessimisticLocking lock = createLocking();
        lock.tryLock("key1", 100L);
        forceLockInSeparateThread("key1");
        lock.unlock("key1");
    }

    @Test
    public void testLockingTwiceByCurrentThreadIsAllowed() throws Exception {
        final PessimisticLocking lock = createLocking();
        lock.tryLock("key", 100L);
        assertThat(lock.isLockedByMe("key"), is(true));
        lock.tryLock("key", 100L);
        assertThat(lock.isLockedByMe("key"), is(true));
    }

}