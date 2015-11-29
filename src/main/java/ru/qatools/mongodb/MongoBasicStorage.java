package ru.qatools.mongodb;

import org.bson.Document;
import org.bson.types.Binary;
import ru.qatools.mongodb.error.InternalRepositoryException;

import java.io.Serializable;

import static ru.qatools.mongodb.util.SerializeUtil.deserializeFromBytes;

/**
 * @author Ilya Sadykov
 */
public interface MongoBasicStorage<T extends Serializable> {

    @SuppressWarnings("unchecked")
    default T getObject(Document doc) {
        try {
            if (doc == null) {
                return null;
            }
            final Object value = doc.get("object");
            return (T) ((value != null) ? deserializeFromBytes(((Binary) value).getData()) : null);
        } catch (Exception e) {
            throw new InternalRepositoryException("Failed to deserialize object from bson! ", e);
        }
    }
}
