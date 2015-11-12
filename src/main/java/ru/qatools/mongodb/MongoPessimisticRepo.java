package ru.qatools.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bson.types.Binary;
import ru.qatools.mongodb.error.ConcurrentReadWriteException;
import ru.qatools.mongodb.error.InternalRepositoryException;
import ru.qatools.mongodb.error.InvalidLockOwnerException;
import ru.qatools.mongodb.error.LockWaitTimeoutException;
import ru.qatools.mongodb.util.SerializeUtil;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.mongodb.client.model.Projections.include;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static ru.qatools.mongodb.util.SerializeUtil.deserializeFromBytes;
import static ru.qatools.mongodb.util.ThreadUtil.threadId;

/**
 * @author Ilya Sadykov
 */
public class MongoPessimisticRepo<T extends Serializable> implements PessimisticRepo<T> {

    public static final String COLL_SUFFIX = "_repo";
    final MongoPessimisticLocking lock;

    public MongoPessimisticRepo(MongoPessimisticLocking lock) {
        this.lock = lock;
    }

    @Override
    public T tryLockAndGet(String key, long timeoutMs) throws LockWaitTimeoutException, ConcurrentReadWriteException {
        lock.tryLock(key, timeoutMs);
        return get(key);
    }

    @Override
    public void putAndUnlock(String key, T object) throws ConcurrentReadWriteException {
        ensureLockOwner(key);
        put(key, object);
        lock.unlock(key);
    }


    @Override
    public void removeAndUnlock(String key) throws InvalidLockOwnerException {
        ensureLockOwner(key);
        remove(key);
        lock.unlock(key);
    }

    @Override
    public void remove(String key) {
        collection().deleteOne(byId(key));
    }

    @Override
    public void put(String key, T object) {
        collection().updateOne(byId(key), new BasicDBObject("$set",
                new BasicDBObject("object", SerializeUtil.serializeToBytes(object))), new UpdateOptions().upsert(true));
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(String key) {
        final FindIterable res = collection().find(byId(key)).limit(1);
        if (!res.iterator().hasNext()) {
            return null;
        }
        return getObject((Document) res.iterator().next());
    }

    @Override
    public Set<String> keySet() {
        return stream(collection().find().projection(include("_id")).spliterator(), false).map(
                d -> d.get("_id").toString()
        ).collect(toSet());
    }

    @Override
    public Set<T> valueSet() {
        return stream(collection().find()
                .projection(include("object")).spliterator(), false)
                .map(this::getObject).collect(toSet());
    }

    @Override
    public Map<String, T> keyValueMap() {
        final Map<String, T> result = new HashMap<>();
        stream(collection().find().spliterator(), false)
                .forEach(d -> result.put(d.get("_id").toString(), getObject(d)));
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

    @SuppressWarnings("unchecked")
    private T getObject(Document doc) {
        try {
            final Object value = doc.get("object");
            return (T) ((value != null) ? deserializeFromBytes(((Binary) value).getData()) : null);
        } catch (Exception e) {
            throw new InternalRepositoryException("Failed to deserialize object from bson! ", e);
        }
    }
}
