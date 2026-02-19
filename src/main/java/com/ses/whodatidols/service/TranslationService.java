package com.ses.whodatidols.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Service
public class TranslationService {

    private final String API_URL = "https://api.mymemory.translated.net/get";
    private final RestTemplate restTemplate = new RestTemplate();

    public String translateToTurkish(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        // MyMemory API has a 500 character limit for free requests.
        // We'll split the text into chunks of roughly 450 characters.
        if (text.length() <= 450) {
            return performSingleTranslation(text);
        }

        StringBuilder translatedFull = new StringBuilder();
        String[] chunks = splitIntoChunks(text, 450);

        for (String chunk : chunks) {
            String translatedChunk = performSingleTranslation(chunk);
            if (translatedChunk.startsWith("Çeviri hatası:")) {
                return translatedChunk; // Return error immediately if one chunk fails
            }
            translatedFull.append(translatedChunk).append(" ");
        }

        return translatedFull.toString().trim();
    }

    private String performSingleTranslation(String text) {
        try {
            // MyMemory API: langpair=en|tr
            URI uri = UriComponentsBuilder.fromUriString(API_URL)
                    .queryParam("q", text)
                    .queryParam("langpair", "en|tr")
                    .build()
                    .toUri();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);

            if (response != null && response.containsKey("responseData")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseData = (Map<String, Object>) response.get("responseData");
                if (responseData != null && responseData.containsKey("translatedText")) {
                    return (String) responseData.get("translatedText");
                }
            }

            return "Çeviri hatası: Yanıt alınamadı.";
        } catch (Exception e) {
            return "Çeviri hatası: " + e.getMessage();
        }
    }

    private String[] splitIntoChunks(String text, int limit) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        int start = 0;

        while (start < length) {
            int end = Math.min(start + limit, length);

            // Try to find a space near the limit to avoid cutting words
            if (end < length) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }

            chunks.add(text.substring(start, end).trim());
            start = end;
            // Skip spaces at the beginning of the next chunk
            while (start < length && text.charAt(start) == ' ') {
                start++;
            }
        }
        return chunks.toArray(new String[0]);
    }
}
