package io.github.moehreag.gartbin_java;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

class NetworkingWrapper {

    static JsonElement getRequest(String url, CloseableHttpClient client) throws IOException {
        return request(new HttpGet(url), client);
    }

    static JsonElement request(HttpUriRequest request, CloseableHttpClient client) throws IOException {
        HttpResponse response = client.execute(request);

        int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
            throw new IOException("API request failed, status code " + status + "\nBody: " + EntityUtils.toString(response.getEntity()));
        }

        String responseBody = EntityUtils.toString(response.getEntity());

        return JsonParser.parseString(responseBody);
    }

    static JsonElement postRequest(String url, String body, CloseableHttpClient client) throws IOException {
        RequestBuilder requestBuilder = RequestBuilder.post().setUri(url);
        requestBuilder.setHeader("Content-Type", "application/json");
        requestBuilder.setEntity(new StringEntity(body));
        return request(requestBuilder.build(), client);
    }
}
