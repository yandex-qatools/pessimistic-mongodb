package ru.qatools.mongodb;

import org.bson.Document;

/**
 * @author Ilya Sadykov
 */
public interface Deserializer {

    /**
     * Deserialize the input document into object
     */
    <T> T fromDBObject(Document input, Class<T> expectedClass) throws Exception; //NOSONAR
}
