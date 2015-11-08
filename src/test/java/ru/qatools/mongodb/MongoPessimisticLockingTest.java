package ru.qatools.mongodb;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.qatools.mongodb.error.InvalidLockOwnerException;
import ru.qatools.mongodb.error.LockWaitTimeoutException;
import ru.yandex.qatools.embed.service.MongoEmbeddedService;

import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicLong;

import static de.flapdoodle.embed.mongo.distribution.Version.Main.PRODUCTION;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static ru.qatools.mongodb.util.SocketUtil.findFreePort;

/**
 * @author Ilya Sadykov
 */
public class MongoPessimisticLockingTest {
    public static final String RS_NAME = "local";
    public static final String DB = "dbname";
    public static final String USER = "user";
    public static final String PASS = "password";
    public static final int INIT_TIMEOUT = 10000;
    protected MongoEmbeddedService mongo;

    @Before
    public void setUp() throws Exception {
        mongo = new MongoEmbeddedService("localhost:" + findFreePort(),
                DB, USER, PASS, RS_NAME, null, true, INIT_TIMEOUT)
                .useVersion(PRODUCTION).useWiredTiger();
        mongo.start();
    }

    @After
    public void tearDown() throws Exception {
        mongo.stop();
    }

    @Test
    public void testLockAndUnlock() throws Exception {
        final MongoPessimisticLocking lock = createLocking();
        lock.tryLock("someKey", 1000L);
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
        sleep(2000L);
        lock.unlock("key1");
        otherThread.join();
        assertThat(waitedMs.get(), anyOf(greaterThan(2000L), lessThan(3000L)));
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

    protected MongoPessimisticLocking createLocking() {
        try {
            return new MongoPessimisticLocking(
                    mongo.getHost() + " :" + mongo.getPort(), DB, USER, PASS, "some", "ACKNOWLEDGED", 100
            );
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    protected void forceLockInSeparateThread(String key) throws InterruptedException {
        final Thread thread = new Thread(() -> {
            final MongoPessimisticLocking lock = createLocking();
            lock.forceUnlock(key);
            lock.tryLock(key, 100L);
        });
        thread.start();
        thread.join();
    }
}