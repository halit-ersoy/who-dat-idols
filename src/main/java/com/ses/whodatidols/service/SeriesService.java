package com.ses.whodatidols.service;

import com.ses.whodatidols.model.Episode;
import com.ses.whodatidols.model.Series;
import com.ses.whodatidols.repository.SeriesRepository;
import com.ses.whodatidols.controller.MediaController;
import com.ses.whodatidols.util.FFmpegUtils;
import com.ses.whodatidols.util.ImageUtils;
import com.ses.whodatidols.viewmodel.EpisodeViewModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SeriesService {

    private final SeriesRepository repository;
    private final TvMazeService tvMazeService;
    private final NotificationService notificationService;

    @Value("${media.source.soap_operas.path}")
    private String soapOperasPath;

    public SeriesService(SeriesRepository repository, TvMazeService tvMazeService,
            NotificationService notificationService) {
        this.repository = repository;
        this.tvMazeService = tvMazeService;
        this.notificationService = notificationService;
    }

    public List<Series> getAllSeries() {
        return repository.findAllSeries();
    }

    public Series findSeriesByName(String name) {
        return repository.findSeriesByName(name);
    }

    public Series getSeriesById(UUID id) {
        return repository.findSeriesById(id);
    }

    // Refactored to use FK if available, or fallback to XML parsing
    // For now, we are in transition, so we might need to support both or assume
    // migration
    public List<EpisodeViewModel> getEpisodesForSeries(UUID seriesId) {
        // Try to fetch via FK first (The optimized way)
        List<Episode> dbEpisodes = repository.findEpisodesBySeriesId(seriesId);

        if (dbEpisodes != null && !dbEpisodes.isEmpty()) {
            // New way
            List<EpisodeViewModel> viewModels = new ArrayList<>();
            for (Episode ep : dbEpisodes) {
                EpisodeViewModel vm = new EpisodeViewModel();
                vm.setId(ep.getId());
                vm.setName(ep.getName());
                vm.setSeasonNumber(ep.getSeasonNumber());
                vm.setEpisodeNumber(ep.getEpisodeNumber());
                vm.setDuration(ep.getDurationMinutes());
                viewModels.add(vm);
            }
            viewModels.sort(Comparator.comparingInt(EpisodeViewModel::getSeasonNumber)
                    .thenComparingInt(EpisodeViewModel::getEpisodeNumber));
            return viewModels;
        }

        // Fallback: XML Parsing (Legacy)
        Series series = repository.findSeriesById(seriesId);
        if (series == null || series.getEpisodeMetadataXml() == null || series.getEpisodeMetadataXml().isEmpty()) {
            return Collections.emptyList();
        }

        List<EpisodeViewModel> episodes = new ArrayList<>();
        String xml = series.getEpisodeMetadataXml();

        java.util.regex.Pattern seasonPattern = java.util.regex.Pattern
                .compile("<Season number=\"(\\d+)\">(.*?)</Season>", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher seasonMatcher = seasonPattern.matcher(xml);

        while (seasonMatcher.find()) {
            int seasonNum = Integer.parseInt(seasonMatcher.group(1));
            String seasonContent = seasonMatcher.group(2);

            java.util.regex.Pattern epPattern = java.util.regex.Pattern
                    .compile("<Episode number=\"(\\d+)\">\\s*([a-fA-F0-9\\-]+)\\s*</Episode>");
            java.util.regex.Matcher epMatcher = epPattern.matcher(seasonContent);

            while (epMatcher.find()) {
                int epNum = Integer.parseInt(epMatcher.group(1));
                String epIdStr = epMatcher.group(2);
                try {
                    UUID epId = UUID.fromString(epIdStr);
                    Episode epDetails = repository.findEpisodeById(epId);
                    if (epDetails != null) {
                        EpisodeViewModel vm = new EpisodeViewModel();
                        vm.setId(epId);
                        vm.setName(epDetails.getName());
                        vm.setSeasonNumber(seasonNum);
                        vm.setEpisodeNumber(epNum);
                        vm.setDuration(epDetails.getDurationMinutes());
                        episodes.add(vm);
                    }
                } catch (IllegalArgumentException e) {
                    // Skip invalid UUIDs
                }
            }
        }

        episodes.sort(Comparator.comparingInt(EpisodeViewModel::getSeasonNumber)
                .thenComparingInt(EpisodeViewModel::getEpisodeNumber));

        return episodes;
    }

    public void updateSeriesMetadata(Series s, MultipartFile file, MultipartFile image) throws IOException {
        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(soapOperasPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath))
                Files.createDirectories(uploadPath);

            String fileName = s.getId().toString() + ".mp4";
            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        repository.updateSeriesMetadata(s);
        if (image != null && !image.isEmpty()) {
            saveImage(s.getId(), image);
        }
    }

    @Transactional
    public void saveEpisodeWithFile(Series seriesInfo, int seasonNumber, int episodeNumber, MultipartFile file,
            MultipartFile image, UUID existingSeriesId)
            throws IOException {
        // 1. DİZİ KONTROLÜ (Parent)
        UUID seriesId;
        String currentXML;
        String seriesName;

        if (existingSeriesId != null) {
            Series found = repository.findSeriesById(existingSeriesId);
            if (found == null) {
                throw new RuntimeException("Seçilen dizi bulunamadı! ID: " + existingSeriesId);
            }
            seriesId = existingSeriesId;
            currentXML = found.getEpisodeMetadataXml();
            seriesName = found.getName();
        } else {
            Series existingSeries = repository.findSeriesByName(seriesInfo.getName());
            if (existingSeries == null) {
                seriesId = UUID.randomUUID();
                seriesInfo.setId(seriesId);
                // Assume empty XML first
                seriesInfo.setEpisodeMetadataXml("<Seasons></Seasons>");
                seriesInfo.setUploadDate(LocalDateTime.now());
                repository.createSeries(seriesInfo);
                currentXML = "<Seasons></Seasons>";
                seriesName = seriesInfo.getName();
            } else {
                seriesId = existingSeries.getId();
                currentXML = existingSeries.getEpisodeMetadataXml();
                seriesName = existingSeries.getName();
            }
        }

        if (image != null && !image.isEmpty()) {
            saveImage(seriesId, image);
        }

        // 2. DOSYA VE BÖLÜM İŞLEMLERİ (Child)
        UUID episodeId = UUID.randomUUID();
        int duration = 1;

        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(soapOperasPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath))
                Files.createDirectories(uploadPath);

            String fileName = episodeId.toString() + ".mp4";
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            duration = FFmpegUtils.getVideoDurationInMinutes(filePath.toString());
            if (duration <= 0)
                duration = 1;
        }

        Episode episodeData = new Episode();
        episodeData.setId(episodeId);
        episodeData.setName(seriesName); // Usually inherits series name
        episodeData.setDurationMinutes(duration);
        episodeData.setReleaseYear(LocalDateTime.now().getYear());
        episodeData.setUploadDate(LocalDateTime.now());
        episodeData.setSeriesId(seriesId); // OPTIMIZATION: Set FK!
        episodeData.setSeasonNumber(seasonNumber);
        episodeData.setEpisodeNumber(episodeNumber);

        repository.saveEpisode(episodeData);

        // 3. XML GÜNCELLEME
        String updatedXML = injectEpisodeToXML(currentXML, seasonNumber, episodeNumber,
                episodeId.toString());
        repository.updateSeriesXML(seriesId, updatedXML);

        // Bildirim oluştur
        try {
            notificationService.createNotification(
                    "Yeni Bölüm Geldi!",
                    seriesName + " dizisinin " + seasonNumber + ". Sezon " + episodeNumber
                            + ". Bölümü eklendi!",
                    episodeId,
                    "SoapOpera");
        } catch (Exception e) {
            System.err.println("Bildirim oluşturulamadı: " + e.getMessage());
        }
    }

    private void saveImage(UUID id, MultipartFile image) throws IOException {
        Path uploadPath = Paths.get(soapOperasPath).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath))
            Files.createDirectories(uploadPath);
        for (String ext : MediaController.SUPPORTED_IMAGE_EXTENSIONS) {
            Files.deleteIfExists(uploadPath.resolve(id.toString() + ext));
        }
        Path finalPath = uploadPath.resolve(id.toString() + ".jpg");
        try {
            ImageUtils.saveAsJpg(image.getInputStream(), finalPath);
        } catch (Exception e) {
            System.err.println("JPG dönüşüm hatası: " + e.getMessage());
        }
    }

    public void saveImageFromUrl(UUID id, String imageUrl) throws IOException {
        byte[] imageBytes = tvMazeService.downloadImage(imageUrl);
        if (imageBytes == null)
            return;
        Path uploadPath = Paths.get(soapOperasPath).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath))
            Files.createDirectories(uploadPath);
        for (String ext : MediaController.SUPPORTED_IMAGE_EXTENSIONS) {
            Files.deleteIfExists(uploadPath.resolve(id.toString() + ext));
        }
        Path finalPath = uploadPath.resolve(id.toString() + ".jpg");
        try {
            ImageUtils.saveImageFromUrlAsJpg(imageUrl, finalPath);
        } catch (Exception e) {
            System.err.println("JPG dönüşüm hatası (URL): " + e.getMessage());
            Files.write(uploadPath.resolve(id.toString() + ".jpg"), imageBytes);
        }
    }

    @Transactional
    public void deleteEpisodeById(UUID id) {
        Series parentSeries = repository.findSeriesByEpisodeIdInsideXML(id.toString());
        if (parentSeries != null) {
            String currentXml = parentSeries.getEpisodeMetadataXml();
            String cleanXml = removeEpisodeFromXML(currentXml, id.toString());
            repository.updateSeriesXML(parentSeries.getId(), cleanXml);
        }
        repository.deleteEpisodeById(id);
        deletePhysicalFile(id.toString());
    }

    @Transactional
    public void deleteSeriesByName(String name) {
        Series series = repository.findSeriesByName(name);
        if (series != null) {
            deleteSeriesById(series.getId());
        }
    }

    @Transactional
    public void deleteSeriesById(UUID id) {
        Series series = repository.findSeriesById(id);
        if (series == null)
            return;

        List<Episode> episodes = repository.findEpisodesBySeriesId(id);
        for (Episode ep : episodes) {
            repository.deleteEpisodeById(ep.getId());
            deletePhysicalFile(ep.getId().toString());
        }

        String xml = series.getEpisodeMetadataXml();
        if (xml != null && !xml.isEmpty()) {
            java.util.regex.Pattern epPattern = java.util.regex.Pattern
                    .compile("<Episode number=\"\\d+\">([a-fA-F0-9\\-]+)</Episode>");
            java.util.regex.Matcher matcher = epPattern.matcher(xml);
            while (matcher.find()) {
                String epUuidStr = matcher.group(1);
                try {
                    UUID epId = UUID.fromString(epUuidStr);
                    if (repository.findEpisodeById(epId) != null) {
                        repository.deleteEpisodeById(epId);
                        deletePhysicalFile(epUuidStr);
                    }
                } catch (IllegalArgumentException e) {
                }
            }
        }
        repository.deleteSeriesById(id);
    }

    private void deletePhysicalFile(String fileId) {
        try {
            Path filePath = Paths.get(soapOperasPath, fileId + ".mp4").toAbsolutePath().normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Dosya silinemedi: " + e.getMessage());
        }
    }

    private String injectEpisodeToXML(String xml, int seasonNum, int episodeNum, String episodeUUID) {
        String seasonTag = "<Season number=\"" + seasonNum + "\">";
        String episodeTag = "<Episode number=\"" + episodeNum + "\">" + episodeUUID + "</Episode>";

        if (xml.contains(seasonTag)) {
            int seasonIndex = xml.indexOf(seasonTag);
            int closingSeasonIndex = xml.indexOf("</Season>", seasonIndex);

            if (closingSeasonIndex == -1)
                return xml;

            String before = xml.substring(0, closingSeasonIndex);
            String after = xml.substring(closingSeasonIndex);
            return before + episodeTag + after;
        } else {
            String newSeason = seasonTag + episodeTag + "</Season>";
            if (xml.contains("<Seasons>")) {
                return xml.replace("</Seasons>", newSeason + "</Seasons>");
            } else {
                return "<Seasons>" + newSeason + "</Seasons>";
            }
        }
    }

    private String removeEpisodeFromXML(String xml, String episodeUUID) {
        String patternString = "<Episode[^>]*>" + Pattern.quote(episodeUUID) + "<\\/Episode>";
        return xml.replaceAll(patternString, "");
    }

    public List<Episode> getRecentEpisodesWithMetadata(int limit) {
        List<Episode> episodes = repository.findRecentEpisodes(limit);
        return episodes;
    }

    public List<Episode> getTop6EpisodesByCount() {
        return repository.findTop6EpisodesByCount();
    }
}
