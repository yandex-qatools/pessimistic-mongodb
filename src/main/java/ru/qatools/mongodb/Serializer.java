package ru.qatools.mongodb;

import com.mongodb.BasicDBObject;

/**
 * @author Ilya Sadykov
 */
public interface Serializer {
    /**
     * Serialize the object to bytes
     */
    BasicDBObject toDBObject(Object object);
}
