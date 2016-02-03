package org.mksmart.ecapi;

import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.junit.Test;

public class HomepageTest extends TestBase {

    @Test
    public void testSignOfLife() throws Exception {
        RequestExecutor req = executor
                .execute(builder.buildGetRequest("/").withHeader("Accept", "text/html"));
        req.assertStatus(200).assertContentType("text/html")
                .assertContentContains("jit/compile", "jit/entity")
                .assertContentRegexp("[Ee]ntity\\-[Cc]entric\\s+API", "<title.*M[Kk]:[Ss]mart");
    }

}
