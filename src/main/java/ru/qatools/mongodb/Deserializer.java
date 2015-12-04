package ru.qatools.mongodb;

import java.io.Serializable;

import static java.lang.Thread.currentThread;

/**
 * @author Ilya Sadykov
 */
public interface Deserializer {
    /**
     * Deserialize the input bytes into object
     */
    default <T extends Serializable> T fromBytes(byte[] input) throws Exception {  //NOSONAR
        return fromBytes(input, currentThread().getContextClassLoader());
    }

    /**
     * Deserialize the input bytes into object
     */
    <T extends Serializable> T fromBytes(byte[] input, ClassLoader classLoader) throws Exception; //NOSONAR
}
