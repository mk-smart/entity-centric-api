package org.mksmart.ecapi.commons.couchdb.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.json.JSONObject;

/**
 * 
 * @author alessandro <alexdma@apache.org>
 *
 */
public class LocalDocumentProvider implements DocumentProvider<JSONObject> {

    private String base, db;

    public LocalDocumentProvider(String base, String db) {
        this.base = base;
        this.db = db;
    }

    @Override
    public JSONObject getDocument(String id) {
        String newid = id.replace(":", "-").replace("/", "__").replace("#", "--");
        String didEnc;
        try {
            didEnc = new URLCodec().encode(newid);
        } catch (EncoderException e) {
            throw new RuntimeException(e);
        }
        String u = this.base + '/' + this.db + '/' + didEnc;
        return getResource(u);
    }

    @Override
    public JSONObject getDocuments(String... keys) {
        String u = this.base + '/' + this.db + '/' + "_all_docs?include_docs=true";
        return getResource(u, keys);
    }

    @Override
    public JSONObject getReducedView(String designDocId, String viewId, boolean group, String... keys) {
        return getView(designDocId, viewId, keys);
    }

    @Override
    public JSONObject getResource(String url, String... keys) {
        InputStream in;
        try {
            in = new URL(url).openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        BufferedReader streamReader;
        StringBuilder responseStrBuilder;
        try {
            streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new JSONObject(responseStrBuilder.toString());
    }

    @Override
    public JSONObject getView(String designDocId, String viewId, String... keys) {
        if (keys != null && keys.length > 0) throw new UnsupportedOperationException(
                "Local document retrieval does not support lookups. Please call method without supplying any keys.");
        String didEnc, widEnc;
        try {
            didEnc = new URLCodec().encode(sanitize(designDocId));
        } catch (EncoderException e) {
            throw new RuntimeException(e);
        }
        try {
            widEnc = new URLCodec().encode(sanitize(viewId));
        } catch (EncoderException e) {
            throw new RuntimeException(e);
        }
        String u = this.base + '/' + this.db + '/' + "_design" + '/' + didEnc + '/' + "_view" + '/' + widEnc;
        return getResource(u);
    }

    private String sanitize(String s) {
        return s.replace(":", "-").replace("/", "__").replace("#", "--");
    }

}
