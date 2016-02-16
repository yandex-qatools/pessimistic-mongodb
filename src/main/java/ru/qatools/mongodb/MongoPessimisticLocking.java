package ru.qatools.mongodb;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.mongodb.error.InvalidLockOwnerException;
import ru.qatools.mongodb.error.LockWaitTimeoutException;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.mongodb.MongoCredential.createCredential;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.Collections.singletonList;
import static jodd.util.StringUtil.isEmpty;
import static ru.qatools.mongodb.util.ThreadUtil.threadId;

/**
 * @author Ilya Sadykov
 */
public class MongoPessimisticLocking implements PessimisticLocking {
    public static final String COLL_SUFFIX = "_lock";
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoPessimisticLocking.class);
    private static final String HOST_PORT_SPLIT_PATTERN = "(?<!:):(?=[123456789]\\d*$)";
    final String dbName;
    final String keySpace;
    final MongoClient mongo;
    final long waitIntervalMs;

    public MongoPessimisticLocking(MongoClient mongo, String dbName, String keySpace, long pollIntervalMs) {
        this.mongo = mongo;
        this.dbName = dbName;
        this.keySpace = keySpace;
        this.waitIntervalMs = pollIntervalMs;
    }

    public MongoPessimisticLocking(String replicaSet, String dbName, String username, String password,
                                   String keySpace, String writeConcern, long waitIntervalMs) throws UnknownHostException {
        List<ServerAddress> addresses = new ArrayList<>();
        for (String host : replicaSet.split(",")) {
            String[] hostPort = host.split(HOST_PORT_SPLIT_PATTERN);
            addresses.add(new ServerAddress(hostPort[0], Integer.valueOf(hostPort[1])));
        }
        mongo = !isEmpty(username) && !isEmpty(password) ?
                new MongoClient(addresses, singletonList(createCredential(username, dbName, password.toCharArray()))) :
                new MongoClient(addresses);
        mongo.setWriteConcern(WriteConcern.valueOf(writeConcern));
        this.dbName = dbName;
        this.keySpace = keySpace;
        this.waitIntervalMs = waitIntervalMs;
    }

    @Override
    public void tryLock(String key, long timeoutMs) throws LockWaitTimeoutException {
        long waitStartedTime = currentTimeMillis();
        LOGGER.trace("Trying to lock the key '{}' for threadId '{}'...", key, threadId());
        if (isLockedByMe(key)) {
            return;
        }
        while (!tryLockUpsertion(key)) {
            LOGGER.trace("Still waiting for the lock of the key '{}' for threadId '{}' ({} of {})... ",
                    key, threadId(), currentTimeMillis() - waitStartedTime, timeoutMs);
            try {
                sleep(new Random().nextInt((int) waitIntervalMs));
            } catch (InterruptedException e) {
                throw new LockWaitTimeoutException("Timeout loop has been interrupted!", e);
            }
            if (currentTimeMillis() - waitStartedTime > timeoutMs) {
                throw new LockWaitTimeoutException("Lock wait timed out for key '" + key + "'!");
            }
        }
        LOGGER.trace("Locked successfully for key '{}' for threadId '{}'...", key, threadId());
    }

    @Override
    public void unlock(String key) throws InvalidLockOwnerException {
        LOGGER.trace("Unlocking key '{}' for threadId '{}'...", key, threadId());
        if (collection().deleteOne(byIdMine(key)).getDeletedCount() == 0) {
            throw new InvalidLockOwnerException("Current thread '" + threadId() + "' is not the owner of the lock " +
                    "for key '" + key + "'!");
        }
        LOGGER.trace("Unlocked successfully for key '{}' for threadId '{}'...", key, threadId());
    }

    @Override
    public void forceUnlock(String key) {
        LOGGER.trace("Forcing unlock of the key '{}' for threadId '{}'...", key, threadId());
        collection().deleteOne(byId(key));
    }

    private BasicDBObject byId(String key) {
        return new BasicDBObject("_id", key);
    }

    @Override
    public boolean isLocked(String key) {
        return collection().find(byId(key)).limit(1).iterator().hasNext();
    }

    @Override
    public boolean isLockedByMe(String key) {
        return collection().find(byIdMine(key)).limit(1).iterator().hasNext();
    }

    private BasicDBObject byIdMine(String key) {
        return new BasicDBObject()
                .append("_id", key)
                .append("threadId", threadId());
    }

    private boolean tryLockUpsertion(String key) {
        try {
            return collection().updateOne(
                    byId(key),
                    new BasicDBObject("$setOnInsert",
                            new BasicDBObject("threadId", threadId())
                                    .append("_id", key)
                                    .append("lockedSince", currentTimeMillis())),
                    new UpdateOptions().upsert(true)).getUpsertedId() != null;
        } catch (MongoWriteException e) {
            LOGGER.trace("Failed to upsert lock value into {} for {}", keySpace + COLL_SUFFIX, threadId(), e);
            return false;
        }
    }

    MongoCollection collection() {
        return db().getCollection(keySpace + COLL_SUFFIX);
    }

    MongoDatabase db() {
        return mongo.getDatabase(dbName);
    }
}
