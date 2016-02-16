package ru.qatools.mongodb.util;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import org.bson.Document;
import ru.qatools.mongodb.Deserializer;
import ru.qatools.mongodb.Serializer;

/**
 * @author Ilya Sadykov
 */
public class JsonSerializer implements Serializer, Deserializer {
    @Override
    public <T> T fromDBObject(Document input, Class<T> expectedClass) throws Exception {
        return new Gson().fromJson(input.toJson(), expectedClass);
    }

    @Override
    public BasicDBObject toDBObject(Object object) {
        return (BasicDBObject) JSON.parse(new Gson().toJson(object));
    }
}
