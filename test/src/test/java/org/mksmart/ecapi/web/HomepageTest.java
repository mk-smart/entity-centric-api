package org.mksmart.ecapi.web;

import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.junit.Test;
import org.mksmart.ecapi.TestBase;

public class HomepageTest extends TestBase {

    @Test
    public void testSignOfLife() throws Exception {
        RequestExecutor req = executor
                .execute(builder.buildGetRequest("/").withHeader("Accept", "text/html"));
        req.assertStatus(200).assertContentType("text/html")
                .assertContentRegexp("[Ee]ntity\\-[Cc]entric\\s+API").assertContentContains("<nav", "</nav>");
    }

}
