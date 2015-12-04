package ru.qatools.mongodb;

import static java.lang.Thread.currentThread;

/**
 * @author Ilya Sadykov
 */
public interface Serializer {
    /**
     * Serialize the object to bytes
     */
    byte[] toBytes(Object object, ClassLoader classLoader);

    /**
     * Serialize the object to bytes
     */
    default byte[] toBytes(Object object) {
        if (object == null) {
            return null; //NOSONAR
        }
        return toBytes(object, (object.getClass().getClassLoader() != null) ?
                object.getClass().getClassLoader() : currentThread().getContextClassLoader());
    }

}
