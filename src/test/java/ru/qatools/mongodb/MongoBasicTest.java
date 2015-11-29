package ru.qatools.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import org.junit.After;
import org.junit.Before;
import ru.yandex.qatools.embed.service.MongoEmbeddedService;

import java.io.Serializable;
import java.net.UnknownHostException;

import static com.mongodb.MongoCredential.createCredential;
import static de.flapdoodle.embed.mongo.distribution.Version.Main.PRODUCTION;
import static java.util.Collections.singletonList;
import static ru.qatools.mongodb.util.SocketUtil.findFreePort;

/**
 * @author Ilya Sadykov
 */
public class MongoBasicTest {
    public static final String RS_NAME = "local";
    public static final String DB = "dbname";
    public static final String USER = "user";
    public static final String PASS = "password";
    public static final int INIT_TIMEOUT = 10000;
    protected MongoEmbeddedService mongo;
    protected MongoClient mongoClient;

    @Before
    public void setUp() throws Exception {
        mongo = new MongoEmbeddedService("localhost:" + findFreePort(),
                DB, USER, PASS, RS_NAME, null, true, INIT_TIMEOUT)
                .useVersion(PRODUCTION).useWiredTiger();
        mongo.start();
        mongoClient = new MongoClient(singletonList(new ServerAddress(mongo.getHost(), mongo.getPort())),
                singletonList(createCredential(USER, DB, PASS.toCharArray())));
    }

    @After
    public void tearDown() throws Exception {
        mongo.stop();
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

    protected User user(String firstName) {
        final User res = new User();
        res.firstName = firstName;
        return res;
    }

    static class User implements Serializable {
        String firstName = "Ivan";
        String lastName = "Ivanov";
    }
}
