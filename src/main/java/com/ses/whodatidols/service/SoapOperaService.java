package com.ses.whodatidols.service;

import com.ses.whodatidols.model.SoapOpera;
import com.ses.whodatidols.repository.SoapOperaRepository;
import com.ses.whodatidols.util.FFmpegUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SoapOperaService {

    private final SoapOperaRepository repository;

    @Value("${media.source.soap_operas.path}")
    private String soapOperasPath;

    public SoapOperaService(SoapOperaRepository repository) {
        this.repository = repository;
    }

    public List<SoapOpera> getAllSeries() {
        return repository.findAllSeries();
    }

    public void updateSeriesMetadata(SoapOpera s, MultipartFile file, MultipartFile image) throws IOException {
        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(soapOperasPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String fileName = s.getId().toString() + ".mp4";
            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            int duration = FFmpegUtils.getVideoDurationInMinutes(filePath.toString());
            s.setTime(duration > 0 ? duration : (s.getTime() > 0 ? s.getTime() : 1));
            
            // Eğer bu bir bölüm ID'si ise (Child tablo), Child tablodaki süreyi de güncelle
            repository.updateEpisode(s);
        }

        repository.updateSeriesMetadata(s);
        if (image != null && !image.isEmpty()) {
            saveImage(s.getId(), image);
        }
    }

    @Transactional
    public void saveEpisodeWithFile(SoapOpera soapOpera, MultipartFile file, MultipartFile image) throws IOException {
        // 1. DİZİ KONTROLÜ (Parent)
        SoapOpera existingSeries = repository.findSeriesByName(soapOpera.getName());
        UUID seriesId;
        String currentXML;

        if (existingSeries == null) {
            seriesId = UUID.randomUUID();
            soapOpera.setId(seriesId);
            repository.createSeries(soapOpera);
            currentXML = "<Seasons></Seasons>";
        } else {
            seriesId = existingSeries.getId();
            currentXML = existingSeries.getXmlData();
        }

        if (image != null && !image.isEmpty()) {
            saveImage(seriesId, image);
        }

        // 2. DOSYA VE BÖLÜM İŞLEMLERİ (Child)
        UUID episodeId = UUID.randomUUID();

        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(soapOperasPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String fileName = episodeId.toString() + ".mp4";
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            int duration = FFmpegUtils.getVideoDurationInMinutes(filePath.toString());
            soapOpera.setTime(duration > 0 ? duration : 1);
        }

        SoapOpera episodeData = new SoapOpera();
        episodeData.setId(episodeId);
        episodeData.setTime(soapOpera.getTime());
        episodeData.setYear(soapOpera.getYear());
        episodeData.setUploadDate(LocalDateTime.now());
        repository.saveEpisode(episodeData);

        // 3. XML GÜNCELLEME
        String updatedXML = injectEpisodeToXML(currentXML, soapOpera.getSeasonNumber(), soapOpera.getEpisodeNumber(), episodeId.toString());
        repository.updateSeriesXML(seriesId, updatedXML);
    }

    private void saveImage(UUID id, MultipartFile image) throws IOException {
        Path uploadPath = Paths.get(soapOperasPath).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        String extension = getExtension(image.getOriginalFilename());
        String[] supportedExtensions = {".webp", ".jpg", ".jpeg", ".png"};
        for (String ext : supportedExtensions) {
            Files.deleteIfExists(uploadPath.resolve(id.toString() + ext));
        }

        Path imagePath = uploadPath.resolve(id.toString() + extension);
        Files.copy(image.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return ".jpg";
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }

    // ----------------------------------------------------------
    //       SİLME MANTIĞI (DELETE LOGIC) - EKLENDİ
    // ----------------------------------------------------------

    @Transactional
    public void deleteEpisodeById(UUID id) {
        // 1. Bu bölüm hangi dizinin XML'inde geçiyor bul?
        SoapOpera parentSeries = repository.findSeriesByEpisodeIdInsideXML(id.toString());

        if (parentSeries != null) {
            // 2. XML'den o bölüm satırını temizle
            String currentXml = parentSeries.getXmlData();
            String cleanXml = removeEpisodeFromXML(currentXml, id.toString());
            repository.updateSeriesXML(parentSeries.getId(), cleanXml);
        }

        // 3. Veritabanından (Child tablosu) sil
        repository.deleteEpisodeById(id);

        // 4. Dosyayı fiziksel olarak sil (Opsiyonel ama önerilir)
        deletePhysicalFile(id.toString());
    }

    @Transactional
    public void deleteSeriesByName(String name) {
        // NOT: Diziyi silince altındaki bölümler veritabanında "öksüz" (orphan) kalabilir.
        // İdeal olan önce XML'i parse edip tüm child ID'leri bulup silmektir.
        // Ancak şimdilik basitçe Parent'ı siliyoruz.
        // Eğer tüm dosyaları da silmek isterseniz burada XML parse edip loop kurmak gerekir.
        repository.deleteSeriesByName(name);
    }

    private void deletePhysicalFile(String fileId) {
        try {
            Path filePath = Paths.get(soapOperasPath, fileId + ".mp4").toAbsolutePath().normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Dosya silinemedi (Disk hatası olabilir): " + e.getMessage());
        }
    }

    // --- XML SİHİRBAZI (HELPER METHODS) ---

    private String injectEpisodeToXML(String xml, int seasonNum, int episodeNum, String episodeUUID) {
        String seasonTag = "<Season number=\"" + seasonNum + "\">";
        String episodeTag = "<Episode number=\"" + episodeNum + "\">" + episodeUUID + "</Episode>";

        if (xml.contains(seasonTag)) {
            String seasonEndTag = "</Season>";
            int seasonStartIndex = xml.indexOf(seasonTag);
            int seasonEndIndex = xml.indexOf(seasonEndTag, seasonStartIndex);
            String before = xml.substring(0, seasonEndIndex);
            String after = xml.substring(seasonEndIndex);
            return before + episodeTag + after;
        } else {
            String newSeasonBlock = seasonTag + episodeTag + "</Season>";
            return xml.replace("</Seasons>", newSeasonBlock + "</Seasons>");
        }
    }

    private String removeEpisodeFromXML(String xml, String episodeUUID) {
        // Regex ile <Episode ...>UUID</Episode> bloğunu bul ve sil
        // Bu pattern şunları kapsar: <Episode number="X">UUID</Episode>
        String patternString = "<Episode[^>]*>" + Pattern.quote(episodeUUID) + "<\\/Episode>";
        return xml.replaceAll(patternString, "");
    }
}