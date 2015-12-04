package ru.qatools.mongodb;

import ru.qatools.mongodb.util.SerializeUtil;

import java.io.Serializable;

/**
 * @author Ilya Sadykov
 */
public class MongoAbstractStorage<T extends Serializable> implements MongoBasicStorage<T> {
    protected Serializer serializer = SerializeUtil::serializeToBytes;
    protected Deserializer deserializer = SerializeUtil::deserializeFromBytes;

    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    public void setDeserializer(Deserializer deserializer) {
        this.deserializer = deserializer;
    }
}
