package com.ses.whodatidols.controller;

import com.ses.whodatidols.model.Actor;
import com.ses.whodatidols.repository.ActorRepository;
import com.ses.whodatidols.service.TmdbService;
import com.ses.whodatidols.service.TranslationService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/actor")
public class ActorController {

    private final ActorRepository actorRepository;
    private final TmdbService tmdbService;
    private final TranslationService translationService;

    public ActorController(ActorRepository actorRepository, TmdbService tmdbService, TranslationService translationService) {
        this.actorRepository = actorRepository;
        this.tmdbService = tmdbService;
        this.translationService = translationService;
    }

    @GetMapping("/{id}")
    @Cacheable(value = "actorDetails", key = "#id")
    public ResponseEntity<Map<String, Object>> getActorDetails(@PathVariable("id") UUID id) {
        Actor actor = actorRepository.findById(id);
        if (actor == null) {
            return ResponseEntity.notFound().build();
        }

        // 1. Ensure TMDB ID exists
        Integer tmdbId = actor.getTmdbId();
        if ((tmdbId == null || tmdbId <= 0) && actor.getName() != null) {
            tmdbId = tmdbService.searchPersonId(actor.getName());
            if (tmdbId != null && tmdbId > 0) {
                actor.setTmdbId(tmdbId);
                actorRepository.updateActorTmdbId(id, tmdbId);
            }
        }

        // 2. Fetch TMDB Person Details if not already cached in DB
        String biography = actor.getBiography();
        String birthday = actor.getBirthday();
        String deathday = actor.getDeathday();
        String placeOfBirth = actor.getPlaceOfBirth();
        String knownFor = actor.getKnownFor();
        Integer gender = actor.getGender();

        boolean needsDbUpdate = false;

        if ((biography == null || birthday == null || placeOfBirth == null) && tmdbId != null && tmdbId > 0) {
            try {
                Map<String, Object> tmdbDetails = tmdbService.getPersonDetails(tmdbId);
                if (tmdbDetails != null && !tmdbDetails.isEmpty()) {
                    String fetchedBio = (String) tmdbDetails.get("biography");
                    if (fetchedBio != null && !fetchedBio.trim().isEmpty()) {
                        // Translate biography to Turkish before saving
                        String translatedBio = translationService.translateToTurkish(fetchedBio.trim());
                        biography = (translatedBio != null && !translatedBio.trim().isEmpty()) ? translatedBio.trim() : fetchedBio.trim();
                        needsDbUpdate = true;
                    }

                    String fetchedBday = (String) tmdbDetails.get("birthday");
                    if (fetchedBday != null && !fetchedBday.trim().isEmpty()) {
                        birthday = fetchedBday.trim();
                        needsDbUpdate = true;
                    }

                    String fetchedDday = (String) tmdbDetails.get("deathday");
                    if (fetchedDday != null && !fetchedDday.trim().isEmpty()) {
                        deathday = fetchedDday.trim();
                        needsDbUpdate = true;
                    }

                    String fetchedPlace = (String) tmdbDetails.get("place_of_birth");
                    if (fetchedPlace != null && !fetchedPlace.trim().isEmpty()) {
                        placeOfBirth = fetchedPlace.trim();
                        needsDbUpdate = true;
                    }

                    String fetchedDept = (String) tmdbDetails.get("known_for_department");
                    if (fetchedDept != null && !fetchedDept.trim().isEmpty()) {
                        switch (fetchedDept.toLowerCase()) {
                            case "acting" -> knownFor = "Oyunculuk";
                            case "directing" -> knownFor = "Yönetmenlik";
                            case "writing" -> knownFor = "Senaristlik";
                            case "production" -> knownFor = "Yapımcılık";
                            default -> knownFor = fetchedDept.trim();
                        }
                        needsDbUpdate = true;
                    }

                    Object fetchedGender = tmdbDetails.get("gender");
                    if (fetchedGender instanceof Number) {
                        gender = ((Number) fetchedGender).intValue();
                        needsDbUpdate = true;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error fetching TMDB details for actor " + id + ": " + e.getMessage());
            }
        }

        // 3. If biography was already in DB but is in a foreign language, translate and update DB
        if (biography != null && !biography.trim().isEmpty() && !translationService.isAlreadyTurkish(biography)) {
            try {
                String translatedBio = translationService.translateToTurkish(biography.trim());
                if (translatedBio != null && !translatedBio.trim().isEmpty() && !translatedBio.equalsIgnoreCase(biography.trim())) {
                    biography = translatedBio.trim();
                    needsDbUpdate = true;
                }
            } catch (Exception e) {
                System.err.println("Error translating existing DB biography for actor " + id + ": " + e.getMessage());
            }
        }

        // Save fetched/translated metadata to Database
        if (needsDbUpdate) {
            actorRepository.updateActorDetails(id, biography, birthday, deathday, placeOfBirth, knownFor, gender);
        }

        if (knownFor == null || knownFor.trim().isEmpty()) {
            knownFor = "Oyuncu";
        }

        // 4. Fetch Platform Productions
        List<Map<String, Object>> movies = actorRepository.getMoviesForActor(id);
        List<Map<String, Object>> series = actorRepository.getSeriesForActor(id);

        List<Map<String, Object>> allProductions = new ArrayList<>();
        allProductions.addAll(movies);
        allProductions.addAll(series);

        // Sort productions: highest release year first
        allProductions.sort((a, b) -> {
            Integer yearA = a.get("releaseYear") instanceof Number ? ((Number) a.get("releaseYear")).intValue() : 0;
            Integer yearB = b.get("releaseYear") instanceof Number ? ((Number) b.get("releaseYear")).intValue() : 0;
            return yearB.compareTo(yearA);
        });

        // 5. Construct Response
        Map<String, Object> response = new HashMap<>();
        response.put("id", actor.getId().toString());
        response.put("name", actor.getName());

        String photoUrl = actor.getPhotoUrl();
        if (photoUrl == null || photoUrl.isEmpty()) {
            photoUrl = actor.getRemotePhotoUrl();
        }
        response.put("photoUrl", photoUrl != null ? photoUrl : "/media/actor/" + actor.getId());

        response.put("biography", biography != null && !biography.trim().isEmpty() ? biography.trim() : null);
        response.put("birthday", birthday);
        response.put("deathday", deathday);
        response.put("placeOfBirth", placeOfBirth);
        response.put("knownFor", knownFor);
        response.put("gender", gender);

        response.put("movies", movies);
        response.put("series", series);
        response.put("productions", allProductions);
        response.put("totalProductions", allProductions.size());

        return ResponseEntity.ok(response);
    }
}
