package ru.qatools.mongodb.util;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.junit.Test;
import ru.qatools.mongodb.Serializer;
import ru.qatools.mongodb.User;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

/**
 * @author Ilya Sadykov
 */
public class SerializeUtilTest {
    Serializer serializer = SerializeUtil::objectToBytes;

    @Test
    public void testSerialize() throws Exception {
        assertThat(serializer.toDBObject(null), notNullValue());
        assertThat(serializer.toDBObject(new Object()), notNullValue());
        assertThat(serializer.toDBObject(new Object()).get("object"), nullValue());
    }

    @Test
    public void testSerializeToBson() throws Exception {
        final User user = User.sample();
        BasicDBObject dbObject = new JsonSerializer().toDBObject(user);
        assertThat(dbObject.get("firstName"), is(user.firstName));
        assertThat(((DBObject) dbObject.get("address")).get("name"), is(user.address.name));
    }


}