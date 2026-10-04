package com.ses.whodatidols.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TranslationService {

    private static final Logger logger = LoggerFactory.getLogger(TranslationService.class);
    private final String API_URL = "https://api.mymemory.translated.net/get";
    private final RestTemplate restTemplate = new RestTemplate();

    @Cacheable(value = "translatedTexts", key = "#text", unless = "#result == null || #result.isEmpty()")
    public String translateToTurkish(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        // If text is already predominantly Turkish, return as is
        if (isAlreadyTurkish(text)) {
            return text.trim();
        }

        // MyMemory API has a 500 character limit for free requests.
        // We'll split the text into chunks of roughly 400 characters around sentence boundaries.
        if (text.length() <= 400) {
            String translated = performSingleTranslation(text);
            return (translated != null && !translated.trim().isEmpty()) ? translated.trim() : text.trim();
        }

        StringBuilder translatedFull = new StringBuilder();
        String[] chunks = splitIntoChunks(text, 400);

        for (String chunk : chunks) {
            if (chunk.trim().isEmpty()) continue;
            String translatedChunk = performSingleTranslation(chunk);
            if (translatedChunk != null && !translatedChunk.trim().isEmpty()) {
                translatedFull.append(translatedChunk.trim()).append(" ");
            } else {
                translatedFull.append(chunk.trim()).append(" ");
            }
        }

        String result = translatedFull.toString().trim();
        return !result.isEmpty() ? result : text.trim();
    }

    private String performSingleTranslation(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        try {
            // MyMemory API: langpair=autodetect|tr (supports English, Korean, Japanese, Chinese, etc.)
            URI uri = UriComponentsBuilder.fromUriString(API_URL)
                    .queryParam("q", text.trim())
                    .queryParam("langpair", "autodetect|tr")
                    .build()
                    .encode()
                    .toUri();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);

            if (response != null && response.containsKey("responseData")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseData = (Map<String, Object>) response.get("responseData");
                if (responseData != null && responseData.containsKey("translatedText")) {
                    String translated = (String) responseData.get("translatedText");
                    if (translated != null && !translated.trim().isEmpty()) {
                        // Check for MyMemory quota warning
                        if (translated.toUpperCase().contains("MYMEMORY WARNING:") ||
                            translated.toUpperCase().contains("QUERY LENGTH LIMIT EXCEEDED")) {
                            logger.warn("MyMemory quota or length warning: {}", translated);
                            return text;
                        }
                        // Unescape HTML entities (e.g. &#39; -> ')
                        return HtmlUtils.htmlUnescape(translated.trim());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("MyMemory Translation error: {}", e.getMessage());
        }
        return text;
    }

    private String[] splitIntoChunks(String text, int limit) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        int start = 0;

        while (start < length) {
            int end = Math.min(start + limit, length);

            if (end < length) {
                // Try finding sentence end first (. ! ? or newline)
                int sentenceBreak = -1;
                for (int i = end; i > start + (limit / 2); i--) {
                    char c = text.charAt(i);
                    if (c == '\n' || ((c == '.' || c == '!' || c == '?') && (i + 1 == length || Character.isWhitespace(text.charAt(i + 1))))) {
                        sentenceBreak = i + 1;
                        break;
                    }
                }

                if (sentenceBreak != -1) {
                    end = sentenceBreak;
                } else {
                    // Fall back to last space
                    int lastSpace = text.lastIndexOf(' ', end);
                    if (lastSpace > start + 50) {
                        end = lastSpace;
                    }
                }
            }

            chunks.add(text.substring(start, end).trim());
            start = end;
            while (start < length && (text.charAt(start) == ' ' || text.charAt(start) == '\n')) {
                start++;
            }
        }
        return chunks.toArray(new String[0]);
    }

    public boolean isAlreadyTurkish(String text) {
        if (text == null || text.trim().isEmpty()) return true;

        // Check for Asian or Cyrillic characters (Hangul, Hanzi, Kana, Cyrillic)
        if (text.matches(".*[\\p{IsHangul}\\p{IsHiragana}\\p{IsKatakana}\\p{IsHan}\\p{IsCyrillic}].*")) {
            return false;
        }

        String lower = text.toLowerCase();
        // Common English bio markers
        String[] englishMarkers = {
            " was born", " is an actor", " is a south korean", " is a japanese",
            " is a chinese", " known for", " best known for", " graduated from",
            " her career", " his career", " film debut", " appeared in",
            " began his", " began her", " she has", " he has", " starred in"
        };

        int englishScore = 0;
        for (String marker : englishMarkers) {
            if (lower.contains(marker)) {
                englishScore++;
            }
        }

        // Distinct Turkish characters: ç, ğ, ı, ö, ş, ü
        long turkishCharCount = text.chars()
                .filter(ch -> ch == 'ç' || ch == 'ğ' || ch == 'ı' || ch == 'ö' || ch == 'ş' || ch == 'ü' ||
                              ch == 'Ç' || ch == 'Ğ' || ch == 'İ' || ch == 'Ö' || ch == 'Ş' || ch == 'Ü')
                .count();

        // If English markers found and very few Turkish characters, it's not Turkish
        if (englishScore >= 2 && turkishCharCount < 3) {
            return false;
        }
        if (englishScore >= 1 && turkishCharCount == 0) {
            return false;
        }

        return turkishCharCount >= 3;
    }
}
