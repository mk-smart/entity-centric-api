package org.mksmart.ecapi.web;

import static org.mksmart.ecapi.Constants.API_ROOT;

import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.junit.Test;
import org.mksmart.ecapi.TestBase;

public class ApiTest extends TestBase {

    @Test
    public void testApiRoot() throws Exception {
        RequestExecutor req = executor.execute(builder.buildGetRequest(API_ROOT).withHeader("Accept",
            "application/atomsvc+xml"));
        req.assertStatus(200).assertContentType("application/atomsvc+xml")
                .assertContentContains("xmlns=\"http://www.w3.org/2005/Atom\"");
    }

}
