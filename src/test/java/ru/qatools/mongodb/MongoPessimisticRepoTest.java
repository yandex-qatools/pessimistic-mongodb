package ru.qatools.mongodb;

import org.junit.Test;
import ru.qatools.mongodb.error.LockWaitTimeoutException;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Ilya Sadykov
 */
public class MongoPessimisticRepoTest extends MongoBasicTest {

    @Test
    public void testRepoGetAndPut() throws Exception {
        final PessimisticRepo<User> repo = createRepo();
        repo.put("user", user("Vasya"));
        final User user = repo.tryLockAndGet("user", 100);
        final AtomicBoolean exceptionRaised = new AtomicBoolean();
        final Thread thread = new Thread(() -> {
            try {
                repo.tryLockAndGet("user", 500L);
            } catch (LockWaitTimeoutException e) {
                exceptionRaised.set(true);
            }
        });
        thread.start();
        thread.join();
        assertThat(exceptionRaised.get(), is(true));
        user.lastName = "Vasilyev";
        repo.put("userNull", null);
        repo.putAndUnlock("user", user);
        assertThat(repo.get("user").lastName, is("Vasilyev"));
        assertThat(repo.keySet(), hasItem("user"));
        assertThat(repo.valueSet().stream().map(u -> (u != null) ? u.firstName : null).collect(toSet()),
                hasItem(user.firstName));
        assertThat(repo.keyValueMap().keySet(), hasItem("user"));
    }

    @Test
    public void testReturnsNullWhenNoData() throws Exception {
        assertThat(createRepo().get("somekey"), nullValue());
    }

    protected MongoPessimisticRepo<User> createRepo() {
        return new MongoPessimisticRepo<>(createLocking());
    }
}