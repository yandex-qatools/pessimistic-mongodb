package ru.qatools.mongodb.util;

import org.junit.Test;

import static java.lang.Thread.currentThread;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static ru.qatools.mongodb.util.SerializeUtil.serializeToBytes;

/**
 * @author Ilya Sadykov
 */
public class SerializeUtilTest {

    @Test
    public void testSerialize() throws Exception {
        assertThat(serializeToBytes(null), nullValue());
        assertThat(serializeToBytes(null, currentThread().getContextClassLoader()), nullValue());
        assertThat(serializeToBytes(new Object()), nullValue());
    }
}