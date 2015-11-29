package ru.qatools.mongodb;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.IntStream.rangeClosed;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.qatools.matchers.decorators.MatcherDecorators.should;
import static ru.yandex.qatools.matchers.decorators.MatcherDecorators.timeoutHasExpired;

/**
 * @author Ilya Sadykov
 */
public class MongoTailingQueueTest extends MongoPessimisticLockingTest {

    private TailingQueue<User> queue;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        queue = new MongoTailingQueue<>(User.class, mongoClient, DB, "queue", 3);
        queue.init();
    }

    @Test
    public void testInitMultipleTimes() throws Exception {
        rangeClosed(1, 3).forEach(n -> {
            queue.init();
            queue.add(user("vasya"));
        });
        assertThat(queue.size(), is(3L));
    }

    @Test
    public void testQueueSizeMax() throws Exception {
        rangeClosed(1, 5).forEach(n -> queue.add(user("Vasya" + n)));
        assertThat(queue.size(), is(3L));
        final List<String> users = new ArrayList<>();
        newSingleThreadExecutor().submit((Runnable) () -> queue.poll(u -> users.add(u.firstName)));
        assertThat(users, should(contains("Vasya3", "Vasya4", "Vasya5"))
                .whileWaitingUntil(timeoutHasExpired(500L)));
    }

    @Test
    public void testCumulative() throws Exception {
        final List<String> users = new ArrayList<>();
        newSingleThreadExecutor().submit((Runnable) () -> queue.poll(u -> users.add(u.firstName)));
        rangeClosed(1, 2).forEach(n -> queue.add(user("Vasya" + n)));
        assertThat(users, should(contains("Vasya1", "Vasya2"))
                .whileWaitingUntil(timeoutHasExpired(500L)));
        rangeClosed(4, 5).forEach(n -> queue.add(user("Vasya" + n)));
        assertThat(users, should(contains("Vasya1", "Vasya2", "Vasya4", "Vasya5"))
                .whileWaitingUntil(timeoutHasExpired(500L)));
    }
}