package ru.qatools.mongodb.util;

import com.mongodb.BasicDBObject;
import org.apache.commons.io.input.ClassLoaderObjectInputStream;
import org.bson.Document;
import org.bson.types.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

public abstract class SerializeUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerializeUtil.class);
    public static final String OBJECT_FIELD = "object";

    SerializeUtil() {
    }

    /**
     * Serialize the object to bytes
     */
    public static BasicDBObject objectToBytes(Object object) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            new ObjectOutputStream(bos).writeObject(object);
            return new BasicDBObject(OBJECT_FIELD, bos.toByteArray());
        } catch (Exception e) {
            LOGGER.error("Failed to serialize object to bytes", e);
            return new BasicDBObject("object", null); //NOSONAR
        }
    }

    /**
     * Deserialize the input bytes into object
     */
    @SuppressWarnings("unchecked")
    public static <T> T objectFromBytes(Document input, Class<T> expected)
            throws Exception { //NOSONAR
        final Object value = input.get(OBJECT_FIELD);

        return (T) ((value != null) ? new ClassLoaderObjectInputStream(expected.getClassLoader(),
                new ByteArrayInputStream(((Binary) value).getData())).readObject() : null);
    }
}