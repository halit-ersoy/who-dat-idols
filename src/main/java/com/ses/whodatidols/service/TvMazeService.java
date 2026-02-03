package com.ses.whodatidols.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
public class TvMazeService {

    private final String BASE_URL = "https://api.tvmaze.com";
    private final RestTemplate restTemplate = new RestTemplate();

    public List<Map<String, Object>> searchSeries(String query) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL + "/search/shows")
                .queryParam("q", query)
                .build()
                .toUriString();

        try {
            return restTemplate.getForObject(url, List.class);
        } catch (Exception e) {
            throw e;
        }
    }

    public Map<String, Object> getSeriesDetails(Integer id) {
        String url = BASE_URL + "/shows/" + id;
        return restTemplate.getForObject(url, Map.class);
    }

    public byte[] downloadImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty())
            return null;
        try {
            return restTemplate.getForObject(imageUrl, byte[].class);
        } catch (Exception e) {
            System.err.println("Resim indirilemedi: " + e.getMessage());
            return null;
        }
    }
}
