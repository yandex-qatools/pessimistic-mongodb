package ru.qatools.mongodb;

import ru.qatools.mongodb.util.SerializeUtil;

/**
 * @author Ilya Sadykov
 */
public class MongoAbstractStorage<T> implements MongoBasicStorage<T> {
    protected final Class<T> entityClass;
    protected Serializer serializer = SerializeUtil::objectToBytes;
    protected Deserializer deserializer = SerializeUtil::objectFromBytes;

    public MongoAbstractStorage(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public Serializer getSerializer() {
        return serializer;
    }

    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public Deserializer getDeserializer() {
        return deserializer;
    }

    public void setDeserializer(Deserializer deserializer) {
        this.deserializer = deserializer;
    }
}
