package com.ses.whodatidols.service;

import com.ses.whodatidols.model.CastMemberDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

@Service
public class TmdbService {

    @Value("${tmdb.api.key:e6fa9265669faede5de7fd2f5f4a056b}")
    private String apiKey = "e6fa9265669faede5de7fd2f5f4a056b";

    private final String BASE_URL = "https://api.themoviedb.org/3";
    private final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";
    private final String PROFILE_BASE_URL = "https://image.tmdb.org/t/p/w185";
    private final RestTemplate restTemplate = new RestTemplate();

    @Cacheable(value = "tmdbSearch", key = "#query + '-' + #type", unless = "#result.isEmpty()")
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> search(String query, String type) {
        if (apiKey == null || apiKey.trim().isEmpty() || query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String path = "movie".equalsIgnoreCase(type) ? "search/movie" : "search/tv";
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .pathSegment(path.split("/"))
                    .queryParam("api_key", apiKey.trim())
                    .queryParam("query", query.trim())
                    .queryParam("language", "tr-TR")
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(new URI(url), Map.class);
            if (response != null && response.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                if (results != null && !results.isEmpty()) {
                    return results;
                }
            }

            // Fallback to en-US if tr-TR returns empty
            String fallbackUrl = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .pathSegment(path.split("/"))
                    .queryParam("api_key", apiKey.trim())
                    .queryParam("query", query.trim())
                    .queryParam("language", "en-US")
                    .toUriString();

            Map<String, Object> fallbackResp = restTemplate.getForObject(new URI(fallbackUrl), Map.class);
            if (fallbackResp != null && fallbackResp.containsKey("results")) {
                return (List<Map<String, Object>>) fallbackResp.get("results");
            }
        } catch (Exception e) {
            System.err.println("TMDB Search Error: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    @Cacheable(value = "tmdbDetails", key = "#id + '-' + #type", unless = "#result.isEmpty()")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDetails(Integer id, String type) {
        if (apiKey == null || apiKey.trim().isEmpty() || id == null) {
            return Collections.emptyMap();
        }
        String endpoint = type.equalsIgnoreCase("movie") ? "/movie/" : "/tv/";
        URI uri = UriComponentsBuilder.fromUriString(BASE_URL + endpoint + id)
                .queryParam("api_key", apiKey.trim())
                .queryParam("language", "tr-TR")
                .build()
                .toUri();

        try {
            Map<String, Object> resp = restTemplate.getForObject(uri, Map.class);
            if (resp != null) return resp;
        } catch (Exception e) {
            System.err.println("TMDB Details Error: " + e.getMessage());
        }
        return Collections.emptyMap();
    }

    @Cacheable(value = "tmdbCast", key = "#id + '-' + #type", unless = "#result.isEmpty()")
    @SuppressWarnings("unchecked")
    public List<CastMemberDto> getCast(int id, String type) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String path = "movie".equalsIgnoreCase(type) ? "movie/" + id + "/credits" : "tv/" + id + "/credits";
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .pathSegment(path.split("/"))
                    .queryParam("api_key", apiKey.trim())
                    .queryParam("language", "tr-TR")
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(new URI(url), Map.class);
            if (response != null && response.containsKey("cast")) {
                List<Map<String, Object>> rawCast = (List<Map<String, Object>>) response.get("cast");
                if (rawCast == null) return Collections.emptyList();

                List<CastMemberDto> castList = new ArrayList<>();
                for (Map<String, Object> c : rawCast) {
                    String name = (String) c.get("name");
                    if (name == null || name.trim().isEmpty()) continue;

                    String character = (String) c.get("character");
                    String profilePath = (String) c.get("profile_path");
                    Integer order = (Integer) c.get("order");
                    int orderIndex = order != null ? order : castList.size();

                    String photoUrl = getProfileUrl(profilePath);
                    Integer actorTmdbId = (Integer) c.get("id");

                    CastMemberDto dto = new CastMemberDto();
                    dto.setName(name.trim());
                    dto.setCharacterName(character != null && !character.trim().isEmpty() ? character.trim() : null);
                    dto.setPhotoUrl(photoUrl);
                    dto.setTmdbId(actorTmdbId);
                    dto.setOrderIndex(orderIndex);
                    castList.add(dto);

                    if (castList.size() >= 25) break;
                }
                return castList;
            }
        } catch (Exception e) {
            System.err.println("TMDB Cast Error: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    @Cacheable(value = "tmdbPersonProfile", key = "#personId", unless = "#result == null")
    public String getPersonProfileUrl(int personId) {
        if (apiKey == null || apiKey.trim().isEmpty() || personId <= 0) return null;
        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .pathSegment("person", String.valueOf(personId))
                    .queryParam("api_key", apiKey.trim())
                    .toUriString();
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && resp.containsKey("profile_path")) {
                String profilePath = (String) resp.get("profile_path");
                return getProfileUrl(profilePath);
            }
        } catch (Exception e) {
            System.err.println("TMDB Person Profile Error (" + personId + "): " + e.getMessage());
        }
        return null;
    }

    @Cacheable(value = "tmdbPersonDetails", key = "#personId", unless = "#result == null || #result.isEmpty()")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPersonDetails(int personId) {
        if (apiKey == null || apiKey.trim().isEmpty() || personId <= 0) return Collections.emptyMap();
        try {
            String urlTr = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .pathSegment("person", String.valueOf(personId))
                    .queryParam("api_key", apiKey.trim())
                    .queryParam("language", "tr-TR")
                    .toUriString();

            Map<String, Object> resp = restTemplate.getForObject(new URI(urlTr), Map.class);
            if (resp != null) {
                Map<String, Object> copy = new HashMap<>(resp);
                String bio = (String) copy.get("biography");
                if (bio == null || bio.trim().isEmpty()) {
                    try {
                        String urlEn = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                                .pathSegment("person", String.valueOf(personId))
                                .queryParam("api_key", apiKey.trim())
                                .queryParam("language", "en-US")
                                .toUriString();
                        Map<String, Object> respEn = restTemplate.getForObject(new URI(urlEn), Map.class);
                        if (respEn != null && respEn.get("biography") != null) {
                            String enBio = (String) respEn.get("biography");
                            if (enBio != null && !enBio.trim().isEmpty()) {
                                copy.put("biography", enBio);
                            }
                        }
                    } catch (Exception ignored) {}
                }
                String profilePath = (String) copy.get("profile_path");
                if (profilePath != null && !profilePath.isEmpty()) {
                    copy.put("profileUrl", getProfileUrl(profilePath));
                }
                return copy;
            }
        } catch (Exception e) {
            System.err.println("TMDB Person Details Error (" + personId + "): " + e.getMessage());
        }
        return Collections.emptyMap();
    }

    @Cacheable(value = "tmdbSearchPerson", key = "#name", unless = "#result == null")
    @SuppressWarnings("unchecked")
    public Integer searchPersonId(String name) {
        if (apiKey == null || apiKey.trim().isEmpty() || name == null || name.trim().isEmpty()) return null;
        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .pathSegment("search", "person")
                    .queryParam("api_key", apiKey.trim())
                    .queryParam("query", name.trim())
                    .toUriString();

            Map<String, Object> resp = restTemplate.getForObject(new URI(url), Map.class);
            if (resp != null && resp.containsKey("results")) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) resp.get("results");
                if (list != null && !list.isEmpty()) {
                    Object idObj = list.get(0).get("id");
                    if (idObj instanceof Number) {
                        return ((Number) idObj).intValue();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("TMDB Search Person Error (" + name + "): " + e.getMessage());
        }
        return null;
    }

    public String getPosterUrl(String posterPath) {
        if (posterPath == null || posterPath.isEmpty())
            return null;
        return IMAGE_BASE_URL + posterPath;
    }

    public String getProfileUrl(String profilePath) {
        if (profilePath == null || profilePath.isEmpty())
            return null;
        return PROFILE_BASE_URL + profilePath;
    }
}
