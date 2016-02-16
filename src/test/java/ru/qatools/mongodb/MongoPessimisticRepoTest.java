package ru.qatools.mongodb;

import org.junit.Test;
import ru.qatools.mongodb.error.LockWaitTimeoutException;
import ru.qatools.mongodb.util.JsonSerializer;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.*;
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

    @Test
    public void testRepoWithCustomSerializer() throws Exception {
        final MongoPessimisticRepo<User> repo = createRepo();
        repo.setSerializer(new JsonSerializer());
        repo.setDeserializer(new JsonSerializer());
        repo.put("vasya", user("Vasya"));
        repo.put("petya", user("Petya"));
        final User user = repo.tryLockAndGet("vasya", 100);
        final AtomicBoolean exceptionRaised = new AtomicBoolean();
        final Thread thread = new Thread(() -> {
            try {
                repo.tryLockAndGet("vasya", 500L);
            } catch (LockWaitTimeoutException e) {
                exceptionRaised.set(true);
            }
        });
        thread.start();
        thread.join();
        assertThat(exceptionRaised.get(), is(true));
        user.address = new User.Address();
        user.address.location = "St.Petersburg";
        final Thread thread2 = new Thread(() -> {
            final User u = repo.tryLockAndGet("vasya", 5000L);
            u.firstName = "Vasiliy";
            repo.putAndUnlock("vasya", u);
        });
        thread2.start();
        Thread.sleep(200L);
        user.lastName = "Vasilyev";
        repo.putAndUnlock("vasya", user);
        thread2.join();
        assertThat(repo.get("vasya").lastName, is("Vasilyev"));
        assertThat(repo.get("vasya").firstName, is("Vasiliy"));
        assertThat(repo.keyValueMap().keySet(), hasItems("vasya", "petya"));
    }

    protected MongoPessimisticRepo<User> createRepo() {
        return new MongoPessimisticRepo<>(createLocking(), User.class);
    }
}