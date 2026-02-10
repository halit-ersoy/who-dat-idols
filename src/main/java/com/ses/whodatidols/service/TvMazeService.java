package com.ses.whodatidols.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class TvMazeService {

    private final String BASE_URL = "https://api.tvmaze.com";
    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("null")
    public List<Map<String, Object>> searchSeries(String query) {
        URI url = UriComponentsBuilder.fromUriString(BASE_URL + "/search/shows")
                .queryParam("q", query)
                .build()
                .toUri();

        try {
            return restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, null,
                    new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {
                    }).getBody();
        } catch (Exception e) {
            throw e;
        }
    }

    @SuppressWarnings("null")
    public Map<String, Object> getSeriesDetails(Integer id) {
        String url = BASE_URL + "/shows/" + id;
        return restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, null,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                }).getBody();
    }

    public byte[] downloadImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty())
            return null;
        try {
            return restTemplate.getForObject(new URI(imageUrl), byte[].class);
        } catch (Exception e) {
            System.err.println("Resim indirilemedi: " + e.getMessage());
            return null;
        }
    }
}
