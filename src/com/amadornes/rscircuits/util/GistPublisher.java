package com.amadornes.rscircuits.util;

import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;

import com.amadornes.rscircuits.util.GistAPI.Gist;
import com.amadornes.rscircuits.util.GistAPI.GistFile;
import com.amadornes.rscircuits.util.GistAPI.GistResponse;

public class GistPublisher {

    public static URL publish(Map<String, String> files) throws Exception {

        GistFile[] gistFiles = files.entrySet().stream()//
                .map(e -> new GistFile(e.getKey(), e.getValue()))//
                .collect(Collectors.toList()).toArray(new GistFile[0]);
        GistResponse gist = GistAPI.createGist("Super Circuit Maker", false, gistFiles);
        return new URL(gist.getURL().replaceFirst("/gists", "").replaceFirst("api", "gist"));
    }

    public static String load(String url, String file) throws Exception {

        Gist gist = GistAPI.getGist(new URL(url.replaceFirst("gist.github.com/", "api.github.com/gists/")));
        return gist.getFiles().get(file).getContent();
    }

}
