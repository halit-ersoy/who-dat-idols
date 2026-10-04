package com.ses.whodatidols.service;

import com.ses.whodatidols.model.CastMemberDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

@Service
public class TvMazeService {

    private final String BASE_URL = "https://api.tvmaze.com";
    private final RestTemplate restTemplate = new RestTemplate();

    @Cacheable(value = "tvmazeSearchSeries", key = "#query", unless = "#result.isEmpty()")
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchSeries(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                        .path("/search/shows")
                        .queryParam("q", query.trim())
                        .toUriString();

                return restTemplate.getForObject(new URI(url), List.class);
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode().value() == 429 && attempt == 1) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                System.err.println("TVMaze Search Error: " + e.getMessage());
                break;
            } catch (Exception e) {
                System.err.println("TVMaze Search Error: " + e.getMessage());
                break;
            }
        }
        return Collections.emptyList();
    }

    @Cacheable(value = "tvmazeSeriesDetails", key = "#id", unless = "#result == null")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSeriesDetails(Integer id) {
        if (id == null) return Collections.emptyMap();
        try {
            String url = BASE_URL + "/shows/" + id;
            return restTemplate.getForObject(new URI(url), Map.class);
        } catch (Exception e) {
            System.err.println("TVMaze Details Error: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Cacheable(value = "tvmazeCast", key = "#showId", unless = "#result.isEmpty()")
    @SuppressWarnings("unchecked")
    public List<CastMemberDto> getShowCast(int showId) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String url = BASE_URL + "/shows/" + showId + "/cast";
                List<Map<String, Object>> rawCast = restTemplate.getForObject(new URI(url), List.class);
                if (rawCast == null) return Collections.emptyList();

                List<CastMemberDto> castList = new ArrayList<>();
                int order = 0;
                for (Map<String, Object> item : rawCast) {
                    Map<String, Object> person = (Map<String, Object>) item.get("person");
                    Map<String, Object> character = (Map<String, Object>) item.get("character");
                    if (person == null) continue;

                    String name = (String) person.get("name");
                    if (name == null || name.trim().isEmpty()) continue;

                    Integer personId = (Integer) person.get("id");
                    String photoUrl = null;
                    if (person.get("image") instanceof Map) {
                        Map<String, Object> imgMap = (Map<String, Object>) person.get("image");
                        photoUrl = (String) imgMap.get("medium");
                        if (photoUrl == null) photoUrl = (String) imgMap.get("original");
                    }

                    String characterName = character != null ? (String) character.get("name") : null;

                    CastMemberDto dto = new CastMemberDto();
                    dto.setName(name.trim());
                    dto.setCharacterName(characterName != null && !characterName.trim().isEmpty() ? characterName.trim() : null);
                    dto.setPhotoUrl(photoUrl);
                    dto.setTvmazeId(personId);
                    dto.setOrderIndex(order++);
                    castList.add(dto);

                    if (castList.size() >= 25) break;
                }
                return castList;
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode().value() == 429 && attempt == 1) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                System.err.println("TVMaze Cast Error: " + e.getMessage());
                break;
            } catch (Exception e) {
                System.err.println("TVMaze Cast Error: " + e.getMessage());
                break;
            }
        }
        return Collections.emptyList();
    }

    @Cacheable(value = "tvmazePersonImage", key = "#tvmazePersonId", unless = "#result == null")
    public String getPersonImageUrl(int tvmazePersonId) {
        if (tvmazePersonId <= 0) return null;
        try {
            String url = BASE_URL + "/people/" + tvmazePersonId;
            @SuppressWarnings("unchecked")
            Map<String, Object> person = restTemplate.getForObject(new URI(url), Map.class);
            if (person != null && person.get("image") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> imgMap = (Map<String, Object>) person.get("image");
                String photoUrl = (String) imgMap.get("medium");
                if (photoUrl == null) photoUrl = (String) imgMap.get("original");
                return photoUrl;
            }
        } catch (Exception e) {
            System.err.println("TVMaze Person Image Error (" + tvmazePersonId + "): " + e.getMessage());
        }
        return null;
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
