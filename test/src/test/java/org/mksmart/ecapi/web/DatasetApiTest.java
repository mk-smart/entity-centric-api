package org.mksmart.ecapi.web;

import static org.mksmart.ecapi.Constants.API_ROOT;

import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.junit.Test;
import org.mksmart.ecapi.TestBase;

public class DatasetApiTest extends TestBase {

    @Test
    public void testApiEntityRoot() throws Exception {
        RequestExecutor req = executor.execute(builder.buildGetRequest(API_ROOT + "/dataset").withHeader(
            "Accept", "application/json"));
        req.assertStatus(200).assertContentType("application/json").assertContentContains("I live");
        ;
    }

}
