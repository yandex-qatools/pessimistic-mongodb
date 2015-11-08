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
import ru.qatools.mongodb.util.ThreadUtil;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static ru.qatools.mongodb.util.SerializeUtil.deserializeFromBytes;
import static ru.qatools.mongodb.util.SerializeUtil.serializeToBytes;

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
        try {
            return (T) SerializeUtil.deserializeFromBytes(((Binary) ((Document) res.iterator().next()).get("object")).getData());
        } catch (ClassNotFoundException | IOException e) {
            throw new InternalRepositoryException("Failed to deserialize object from bson! ", e);
        }
    }

    @Override
    public Set<String> keySet() {
        return stream(collection().find().spliterator(), false).map(
                d -> d.get("_id").toString()
        ).collect(toSet());
    }

    @Override
    public PessimisticLocking getLock() {
        return lock;
    }

    private void ensureLockOwner(String key) {
        if (!lock.isLockedByMe(key)) {
            throw new InvalidLockOwnerException("Key '" + key + "' is not locked by threadId '" + ThreadUtil.threadId() + "'!");
        }
    }

    private BasicDBObject byId(String key) {
        return new BasicDBObject("_id", key);
    }

    private MongoCollection<Document> collection() {
        return lock.db().getCollection(lock.keySpace + COLL_SUFFIX);
    }
}
