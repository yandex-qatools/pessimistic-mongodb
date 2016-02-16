package ru.qatools.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.mongodb.error.ConcurrentReadWriteException;
import ru.qatools.mongodb.error.InvalidLockOwnerException;
import ru.qatools.mongodb.error.LockWaitTimeoutException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.mongodb.client.model.Projections.include;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static ru.qatools.mongodb.util.ThreadUtil.threadId;

/**
 * @author Ilya Sadykov
 */
public class MongoPessimisticRepo<T>
        extends MongoAbstractStorage<T> implements PessimisticRepo<T> {
    public static final String COLL_SUFFIX = "_repo";
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoPessimisticRepo.class);
    final MongoPessimisticLocking lock;

    public MongoPessimisticRepo(MongoPessimisticLocking lock, Class<T> entityClass) {
        super(entityClass);
        this.lock = lock;
    }

    @Override
    public T tryLockAndGet(String key, long timeoutMs) throws LockWaitTimeoutException, ConcurrentReadWriteException {
        LOGGER.trace("Trying lock and get key {} by thread {}", key, threadId());
        lock.tryLock(key, timeoutMs);
        return get(key);
    }

    @Override
    public void putAndUnlock(String key, T object) throws ConcurrentReadWriteException {
        LOGGER.trace("Putting new value and unlocking key {} by thread {}", key, threadId());
        ensureLockOwner(key);
        put(key, object);
        lock.unlock(key);
    }


    @Override
    public void removeAndUnlock(String key) throws InvalidLockOwnerException {
        LOGGER.trace("Removing value and unlocking key {} by thread {}", key, threadId());
        ensureLockOwner(key);
        remove(key);
        lock.unlock(key);
    }

    @Override
    public void remove(String key) {
        LOGGER.trace("Removing value without unlocking key {} by thread {}", key, threadId());
        collection().deleteOne(byId(key));
    }

    @Override
    public void put(String key, T object) {
        collection().updateOne(byId(key),
                new Document("$set", serializer.toDBObject(object)),
                new UpdateOptions().upsert(true));
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(String key) {
        final FindIterable res = collection().find(byId(key)).limit(1);
        return getObject((Document) res.iterator().tryNext(), entityClass);
    }

    @Override
    public Set<String> keySet() {
        return stream(collection().find().projection(include("_id")).spliterator(), false).map(
                d -> d.get("_id").toString()
        ).collect(toSet());
    }

    @Override
    public Set<T> valueSet() {
        return stream(collection().find().spliterator(), false)
                .map(d -> getObject(d, entityClass)).collect(toSet());
    }

    @Override
    public Map<String, T> keyValueMap() {
        final Map<String, T> result = new HashMap<>();
        stream(collection().find().spliterator(), false)
                .forEach(d -> result.put(d.get("_id").toString(), getObject(d, entityClass)));
        return result;
    }

    @Override
    public PessimisticLocking getLock() {
        return lock;
    }

    private void ensureLockOwner(String key) {
        if (!lock.isLockedByMe(key)) {
            throw new InvalidLockOwnerException("Key '" + key + "' is not locked by threadId '" + threadId() + "'!");
        }
    }

    private BasicDBObject byId(String key) {
        return new BasicDBObject("_id", key);
    }

    private MongoCollection<Document> collection() {
        return lock.db().getCollection(lock.keySpace + COLL_SUFFIX);
    }
}
