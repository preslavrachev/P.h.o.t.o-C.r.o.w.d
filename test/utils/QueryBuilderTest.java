package utils;

import org.junit.Before;
import org.junit.Test;

import play.test.UnitTest;
import utils.Twitter.QueryBuilder;

/**
 * @author uudashr@gmail.com
 *
 */
public class QueryBuilderTest extends UnitTest {
    private QueryBuilder builder;
    
    @Before
    public void prepare() {
        builder = new QueryBuilder("wedding");
    }
    
    @Test
    public void simpleQuery() {
        assertEquals("wedding", builder.toString());
    }
    
    @Test
    public void querySince() {
        builder.since("2011-07-30");
        assertTrue(builder.toString().contains("since:2011-07-30"));
    }
    
    @Test
    public void queryUntil() {
        builder.until("2011-07-30");
        assertTrue(builder.toString().contains("until:2011-07-30"));
    }
    
    @Test
    public void queryLocation() {
        builder.near("san francisco");
        assertTrue(builder.toString().contains("near:\"san francisco\""));
    }
    
    @Test
    public void queryLocationNoQuote() {
        builder.near("NYC");
        assertTrue(builder.toString().contains("near:NYC"));
    }
}
