package com.ses.whodatidols.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class TmdbService {

    private final String API_KEY = "e6fa9265669faede5de7fd2f5f4a056b";
    private final String BASE_URL = "https://api.themoviedb.org/3";
    private final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";
    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> search(String query, String type) {
        String endpoint = type.equalsIgnoreCase("movie") ? "/search/movie" : "/search/tv";
        URI uri = UriComponentsBuilder.fromUriString(BASE_URL + endpoint)
                .queryParam("api_key", API_KEY)
                .queryParam("query", query)
                .queryParam("language", "en-US")
                .build()
                .toUri();

        try {
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            if (response != null && response.containsKey("results")) {
                return (List<Map<String, Object>>) response.get("results");
            }
        } catch (Exception e) {
            System.err.println("TMDB Search Error: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getDetails(Integer id, String type) {
        String endpoint = type.equalsIgnoreCase("movie") ? "/movie/" : "/tv/";
        URI uri = UriComponentsBuilder.fromUriString(BASE_URL + endpoint + id)
                .queryParam("api_key", API_KEY)
                .queryParam("language", "en-US")
                .build()
                .toUri();

        try {
            return restTemplate.getForObject(uri, Map.class);
        } catch (Exception e) {
            System.err.println("TMDB Details Error: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    public String getPosterUrl(String posterPath) {
        if (posterPath == null || posterPath.isEmpty())
            return null;
        return IMAGE_BASE_URL + posterPath;
    }
}
