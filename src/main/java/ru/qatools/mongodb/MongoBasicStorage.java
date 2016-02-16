package ru.qatools.mongodb;

import org.bson.Document;
import ru.qatools.mongodb.error.InternalRepositoryException;

/**
 * @author Ilya Sadykov
 */
public interface MongoBasicStorage<T> {

    @SuppressWarnings("unchecked")
    default T getObject(Document doc, Class<T> expectedClass) {
        try {
            if (doc == null) {
                return null;
            }
            return getDeserializer().fromDBObject(doc, expectedClass);
        } catch (Exception e) {
            throw new InternalRepositoryException("Failed to deserialize object from bson! ", e);
        }
    }

    Serializer getSerializer();

    Deserializer getDeserializer();
}
