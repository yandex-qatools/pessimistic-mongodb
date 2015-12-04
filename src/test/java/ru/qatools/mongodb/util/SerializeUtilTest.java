package ru.qatools.mongodb.util;

import org.junit.Test;
import ru.qatools.mongodb.Serializer;

import static java.lang.Thread.currentThread;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Ilya Sadykov
 */
public class SerializeUtilTest {
    Serializer serializer = SerializeUtil::serializeToBytes;

    @Test
    public void testSerialize() throws Exception {
        assertThat(serializer.toBytes(null), nullValue());
        assertThat(serializer.toBytes(null, currentThread().getContextClassLoader()), nullValue());
        assertThat(serializer.toBytes(new Object()), nullValue());
    }
}