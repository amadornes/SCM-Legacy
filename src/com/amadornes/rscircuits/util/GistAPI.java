package com.amadornes.rscircuits.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class GistAPI {

    private static final Gson gson = new Gson();

    private static String sendHttpRequest(URL url, String method, String content) throws IOException {

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        if (content != null && !content.isEmpty()) {
            byte[] postDataBytes = content.getBytes("UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setDoOutput(true);
            conn.getOutputStream().write(postDataBytes);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

        String line;
        StringBuilder builder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

    public static GistResponse createGist(String desc, boolean _public, GistFile... content) throws IOException {

        URL url = new URL("https://api.github.com/gists");
        String response = sendHttpRequest(url, "POST", gson.toJson(new Gist(desc, _public, content)));
        return gson.fromJson(response, GistResponse.class);
    }

    public static Gist getGist(URL url) throws IOException {

        String response = sendHttpRequest(url, "GET", null);
        return gson.fromJson(response, Gist.class);
    }

    public static class GistFile {

        private String content;
        private String name;

        public GistFile() {
        }

        public GistFile(String name, String content) {
            this.content = content;
            this.name = name;
        }

        @Override
        public String toString() {

            return content;
        }

        public String getContent() {

            return content;
        }

        public String getName() {

            return name;
        }
    }

    public static class GistResponse {

        private String html_url;
        private String id;

        public String getURL() {

            return html_url;
        }

        public String getId() {

            return id;
        }
    }

    public static class Gist {

        private String description;
        @SerializedName("public")
        private boolean _public;
        private Map<String, GistFile> files = new HashMap<>();

        private Gist(String description, boolean _public, GistFile... files) {

            this.description = description;
            this._public = _public;
            Arrays.stream(files).forEach(f -> this.files.put(f.name, f));
        }

        public String getDescription() {

            return description;
        }

        public Map<String, GistFile> getFiles() {

            return files;
        }
    }

}
