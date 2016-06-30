package org.mksmart.ecapi.commons.couchdb.client;

import java.net.URL;

import org.apache.http.auth.Credentials;
import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.RestClient;
import org.apache.wink.client.handlers.BasicAuthSecurityHandler;

public abstract class RemoteDocumentHandlerBase {

    /**
     * TODO try to have one for the whole framework.
     */
    protected RestClient client;

    protected URL serviceUrl;

    protected String dbName;

    public RemoteDocumentHandlerBase(URL serviceUrl, String dbName) {
        this(serviceUrl, dbName, null);
    }

    public RemoteDocumentHandlerBase(URL serviceUrl, String dbName, Credentials credentials) {
        this.serviceUrl = serviceUrl;
        this.dbName = dbName;
        ClientConfig config = new ClientConfig();
        if (credentials != null) {
            BasicAuthSecurityHandler auth = new BasicAuthSecurityHandler();
            // Only set credentials if there is a username, no spaces around
            if (credentials.getUserPrincipal() != null) {
                String un = new String(credentials.getUserPrincipal().getName()).trim();
                if (!un.isEmpty()) {
                    auth.setUserName(un);
                    if (credentials.getPassword() != null) {
                        String pw = new String(credentials.getPassword()).trim();
                        if (!pw.isEmpty()) {
                            auth.setPassword(pw);
                        }
                    }

                }
            }
            config.handlers(auth);
        }
        client = new RestClient(config);
    }

}
