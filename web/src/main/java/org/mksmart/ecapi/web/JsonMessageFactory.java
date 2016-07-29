package org.mksmart.ecapi.web;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Response.Status;

import org.json.JSONObject;

public class JsonMessageFactory {

    public static JSONObject alive() {
        return alive(new HashSet<String>());
    }

    public static JSONObject alive(Set<String> subresources) {
        JSONObject response = new JSONObject();
        response.put("live", true);
        response.put("subresources", subresources);
        response.put("comment", "Beware: I live!");
        return response;
    }

    public static JSONObject badRequest(String reason) {
        JSONObject response = new JSONObject();
        response.put("code", Status.BAD_REQUEST.getStatusCode());
        response.put("reason", reason);
        return response;
    }

}
