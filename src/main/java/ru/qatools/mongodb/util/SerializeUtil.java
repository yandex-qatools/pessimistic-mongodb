package ru.qatools.mongodb.util;

import org.apache.commons.io.input.ClassLoaderObjectInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public abstract class SerializeUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerializeUtil.class);

    SerializeUtil() {
    }

    /**
     * Serialize the object to bytes
     */
    public static byte[] serializeToBytes(Object object, ClassLoader classLoader) {
        if (object == null || classLoader == null) {
            return null; //NOSONAR
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            new ObjectOutputStream(bos).writeObject(object);
            return bos.toByteArray();
        } catch (Exception e) {
            LOGGER.error("Failed to serialize object to bytes", e);
            return null; //NOSONAR
        }
    }

    /**
     * Deserialize the input bytes into object
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deserializeFromBytes(byte[] input, ClassLoader classLoader)
            throws Exception { //NOSONAR
        ByteArrayInputStream bis = new ByteArrayInputStream(input);
        return (T) new ClassLoaderObjectInputStream(classLoader, bis).readObject();
    }
}