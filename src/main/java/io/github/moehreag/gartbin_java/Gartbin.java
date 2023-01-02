package io.github.moehreag.gartbin_java;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * small API to interact with
 * <a href="https://bin.gart.sh">gartbin</a> in Java.
 *
 * <p>If you wish the API to log somewhere else in debug mode, override the {@code
 * private void log(String message){
 *     ...
 * }
 * } method.</p>
 */
public class Gartbin {

    private String instanceUrl = "https://bin.gart.sh";
    private String userAgent = "gartbin-java/1.0.0";
    private String separator = ";";

    private boolean debug = false;
    private int expiration = 168;
    private String password = "";

    private Gartbin(){
    }

    /**
     * Get a new instance of this API
     * @return a new instance of this API
     */
    public static Gartbin create(){
        return new Gartbin();
    }

    /**
     * Set an UserAgent to be used for this API instance.
     * No trailing slashes!
     * @param agent the new UserAgent
     * @return the API instance
     */
    public Gartbin setUserAgent(String agent){
        this.userAgent = agent;
        return this;
    }

    /**
     * Set the gartbin instance to use
     * @param url the new instance's url
     * @return the API instance
     */
    public Gartbin setInstance(String url){
        this.instanceUrl = url;
        return this;
    }

    /**
     * <p>
     *     Set the separator to use for separating the filename from the data. This is required to be the same
     *     for uploading and downloading, otherwise decoding <b>will fail</b>.
     * </p>
     * <p>
     *     <h1>It is highly recommended to leave this at its default value.</h1>
     * </p>
     * @param separator the new Separator
     * @return the API instance
     */
    public Gartbin setSeparator(String separator){
        this.separator = separator;
        return this;
    }

    /**
     * Set whether to output debug information
     * @param debug whether to output debug information
     * @return the API instance
     */
    public Gartbin setDebug(boolean debug){
        this.debug = debug;
        return this;
    }

    /**
     * Set the expiration duration for this API instance.
     * Set to -1 for the link to never expire.
     * @param hours the new expiration duration
     * @return the API instance
     */
    public Gartbin setExpiration(int hours){
        this.expiration = hours;
        return this;
    }

    /**
     * Set a password to encrypt this paste with.
     * It is also needed to retrieve the file again.
     * Leave empty for no password.
     * @param phrase the passphrase
     * @return the API instance
     */
    public Gartbin setPassword(String phrase){
        password = phrase;
        return this;
    }

    /**
     * Upload a file to gartbin
     * @param file the file to upload
     * @return the full URL of the uploaded file
     */
    public String uploadFile(File file) throws IOException {
        String id = upload(instanceUrl+"/api/stream", file);
        if(!id.isEmpty()) {
            return instanceUrl + "/api/" + id;
        }
        // This will never occur
        throw new RuntimeException();
    }

    private String upload(String url, File file) throws IOException {

        try (CloseableHttpClient client = createHttpClient()){

            log("Uploading file "+file.getName());

            JsonElement el = NetworkingWrapper.getRequest(url, client);
            if(el != null) {
                JsonObject initGet = el.getAsJsonObject();
                String tempId = initGet.get("id").getAsString();
                int chunkSize = initGet.get("chunkSize").getAsInt();
                int maxChunks = initGet.get("maxChunks").getAsInt();

                String data = encodeB64(file);

                List<String> dataList = new ArrayList<>();

                for (char c : data.toCharArray()) {
                    dataList.add(String.valueOf(c));
                }

                List<String> chunks = new ArrayList<>();
                Lists.partition(dataList, chunkSize).forEach(list -> chunks.add(String.join("", list)));

                if(chunks.size() > maxChunks){
                    throw new IllegalStateException("Too much Data!");
                }

                long index = 0;
                for (String content : chunks) {
                    NetworkingWrapper.postRequest(url+"/"+tempId,
                            "{\"index\":"+index+",\"content\": \"" + content + "\"}", client);
                    index += content.getBytes(StandardCharsets.UTF_8).length;
                }

                log("Finishing Stream... tempId was: "+tempId);


                String mimetype = Files.probeContentType(file.toPath()).replace("/", ":");

                JsonElement element = NetworkingWrapper.postRequest(url+"/"+tempId+"/end",
                        "{\"language\": \""+mimetype+"/base64\", \"expiration\": "+(expiration == -1 ? "never" : expiration)+", \"password\":\""+password+"\"}",
                        client);
                return element.getAsJsonObject().get("pasteId").getAsString();

            } else {
                log("Server Error!");
            }
        } catch (Exception e){
            throw new IOException(e);
            // upload failed.
        }
        return "";
    }

    /**
     * Download a file.
     *
     * @param id the URL/ID of the file to download
     * @return a GartbinFile containing the data as an InputStream and the original File name
     * @throws IOException if an exception occurs
     */
    public GartbinFile downloadFile(String id) throws IOException {
        if(id.contains(instanceUrl+"/api/")) {
            return download(id);
        } else if(id.contains(instanceUrl) && !id.contains("api")) {
            return downloadFile(id.substring(id.lastIndexOf("/")));
        } else if(id.startsWith("https://") && id.contains("api")) {
            return download(id);
        }
        return download(instanceUrl+"/api/"+id);
    }

    private GartbinFile download(String url) throws IOException {
        if(!url.isEmpty()) {
            JsonElement element = NetworkingWrapper.request(RequestBuilder.get(url + (password.isEmpty() ? "" : "?password="+password)).build(), createHttpClient());
            if(element != null) {
                JsonObject response = element.getAsJsonObject();
                String content = response.get("content").getAsString();

                return decodeB64(content);
            }
        }
        return null;
    }

    private String encodeB64(File file) throws IOException {
        return file.getName() + separator + Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
    }

    private GartbinFile decodeB64(String data) throws IOException {
        String[] info = data.split(separator);
        byte[] bytes = Base64.getDecoder().decode(info[info.length-1]);
        return new GartbinFile(new ByteArrayInputStream(bytes), info[0]);
    }

    private CloseableHttpClient createHttpClient(){
        return HttpClients.custom().setUserAgent(userAgent).build();
    }

    private void log(String message){
        if(debug) {
            System.out.println(message);
        }
    }
}
