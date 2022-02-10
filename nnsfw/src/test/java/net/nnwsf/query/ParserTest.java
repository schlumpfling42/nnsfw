package net.nnwsf.query;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ParserTest {

    @BeforeAll
    public static void init() {
        QueryParser.init();
    }

    @Test
    public void test() {
        String searchTerm = "key1:value1 OR (key2:'value : 2' AND key3:value3) OR key4:value4";
        SearchTerm result = QueryParser.parseString(searchTerm);
        Assertions.assertEquals("{key1:value1 OR {key2:'value : 2' AND key3:value3} OR key4:value4}", result.toString());
    }

    @Test
    public void testInvalidOperation() {
        String searchTerm = "key1:value1 OR (key2:'value : 2' AND key3:value3) AND key4:value4";
        RuntimeException exception = Assertions.assertThrowsExactly(RuntimeException.class, () -> QueryParser.parseString(searchTerm), "test");
        Assertions.assertEquals("Invalid search term 'AND' at position: 53", exception.getMessage());
    }

}
