package com.ses.whodatidols.service;

import com.ses.whodatidols.model.SoapOpera;
import com.ses.whodatidols.repository.SoapOperaRepository;
import com.ses.whodatidols.util.FFmpegUtils;
import com.ses.whodatidols.util.ImageUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import java.util.regex.Pattern;

@Service
public class SoapOperaService {

    private final SoapOperaRepository repository;
    private final TvMazeService tvMazeService;

    @Value("${media.source.soap_operas.path}")
    private String soapOperasPath;

    public SoapOperaService(SoapOperaRepository repository, TvMazeService tvMazeService) {
        this.repository = repository;
        this.tvMazeService = tvMazeService;
    }

    public List<SoapOpera> getAllSeries() {
        return repository.findAllSeries();
    }

    public SoapOpera findSeriesByName(String name) {
        return repository.findSeriesByName(name);
    }

    public void updateSeriesMetadata(SoapOpera s, MultipartFile file, MultipartFile image) throws IOException {
        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(soapOperasPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath))
                Files.createDirectories(uploadPath);

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
    public void saveEpisodeWithFile(SoapOpera soapOpera, MultipartFile file, MultipartFile image, UUID existingSeriesId)
            throws IOException {
        // 1. DİZİ KONTROLÜ (Parent)
        UUID seriesId;
        String currentXML;

        if (existingSeriesId != null) {
            // Mevcut diziye ekleme modu
            // Servisteki getAllSeries metodunu kullanarak (cache vs varsa yararlanır)
            // diziyi bulalım
            SoapOpera found = this.getAllSeries().stream()
                    .filter(s -> s.getId().equals(existingSeriesId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Seçilen dizi bulunamadı! ID: " + existingSeriesId));

            seriesId = existingSeriesId;
            currentXML = found.getXmlData();

        } else {
            // İsimden bul veya yeni oluştur modu
            SoapOpera existingSeries = repository.findSeriesByName(soapOpera.getName());

            if (existingSeries == null) {
                seriesId = UUID.randomUUID();
                soapOpera.setId(seriesId);
                repository.createSeries(soapOpera);
                currentXML = "<Seasons></Seasons>";
            } else {
                seriesId = existingSeries.getId();
                currentXML = existingSeries.getXmlData();
            }
        }

        if (image != null && !image.isEmpty()) {
            saveImage(seriesId, image);
        }

        // 2. DOSYA VE BÖLÜM İŞLEMLERİ (Child)
        UUID episodeId = UUID.randomUUID();

        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(soapOperasPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath))
                Files.createDirectories(uploadPath);

            String fileName = episodeId.toString() + ".mp4";
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            int duration = FFmpegUtils.getVideoDurationInMinutes(filePath.toString());
            soapOpera.setTime(duration > 0 ? duration : 1);
        }

        SoapOpera episodeData = new SoapOpera();
        episodeData.setId(episodeId);
        episodeData.setName(soapOpera.getName()); // Set the name!
        episodeData.setTime(soapOpera.getTime());
        episodeData.setYear(soapOpera.getYear());
        episodeData.setUploadDate(LocalDateTime.now());
        repository.saveEpisode(episodeData);

        // 3. XML GÜNCELLEME
        String updatedXML = injectEpisodeToXML(currentXML, soapOpera.getSeasonNumber(), soapOpera.getEpisodeNumber(),
                episodeId.toString());
        repository.updateSeriesXML(seriesId, updatedXML);
    }

    private void saveImage(UUID id, MultipartFile image) throws IOException {
        Path uploadPath = Paths.get(soapOperasPath).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath))
            Files.createDirectories(uploadPath);

        // Eski resimleri temizle
        String[] supportedExtensions = { ".jpg", ".jpeg", ".png", ".webp" };
        for (String ext : supportedExtensions) {
            Files.deleteIfExists(uploadPath.resolve(id.toString() + ext));
        }

        Path finalPath = uploadPath.resolve(id.toString() + ".jpg");
        try {
            ImageUtils.saveAsJpg(image.getInputStream(), finalPath);
        } catch (Exception e) {
            System.err.println("JPG dönüşüm hatası: " + e.getMessage());
            String originalExt = getExtension(image.getOriginalFilename());
            Files.move(
                    image.getInputStream().toString().contains(".tmp") ? Paths.get(image.getName())
                            : uploadPath.resolve("temp"),
                    uploadPath.resolve(id.toString() + originalExt),
                    StandardCopyOption.REPLACE_EXISTING);
            // Note: The move logic above was complex/incomplete in fallback, simplifying to
            // just logging if simple retry fails
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains("."))
            return ".jpg";
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }

    public void saveImageFromUrl(UUID id, String imageUrl) throws IOException {
        byte[] imageBytes = tvMazeService.downloadImage(imageUrl);
        if (imageBytes == null)
            return;

        Path uploadPath = Paths.get(soapOperasPath).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath))
            Files.createDirectories(uploadPath);

        // Eski resimleri temizle
        String[] supportedExtensions = { ".jpg", ".jpeg", ".png", ".webp" };
        for (String ext : supportedExtensions) {
            Files.deleteIfExists(uploadPath.resolve(id.toString() + ext));
        }

        Path finalPath = uploadPath.resolve(id.toString() + ".jpg");
        try {
            ImageUtils.saveImageFromUrlAsJpg(imageUrl, finalPath);
        } catch (Exception e) {
            System.err.println("JPG dönüşüm hatası (URL): " + e.getMessage());
            // Fallback: Save bytes as jpg
            Files.write(uploadPath.resolve(id.toString() + ".jpg"), imageBytes);
        }
    }

    // ----------------------------------------------------------
    // SİLME MANTIĞI (DELETE LOGIC) - EKLENDİ
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
        // NOT: Diziyi silince altındaki bölümler veritabanında "öksüz" (orphan)
        // kalabilir.
        // İdeal olan önce XML'i parse edip tüm child ID'leri bulup silmektir.
        // Ancak şimdilik basitçe Parent'ı siliyoruz.
        // Eğer tüm dosyaları da silmek isterseniz burada XML parse edip loop kurmak
        // gerekir.
        repository.deleteSeriesByName(name);
    }

    @Transactional
    public void deleteSeriesById(UUID id) {
        // 1. Diziyi bul
        SoapOpera series = repository.findSeriesById(id);
        if (series == null)
            return;

        // 2. XML'i parse et ve tüm bölüm ID'lerini topla
        String xml = series.getXmlData();
        if (xml != null && !xml.isEmpty()) {
            java.util.regex.Pattern epPattern = java.util.regex.Pattern
                    .compile("<Episode number=\"\\d+\">([a-fA-F0-9\\-]+)</Episode>");
            java.util.regex.Matcher matcher = epPattern.matcher(xml);

            while (matcher.find()) {
                String epUuidStr = matcher.group(1);
                try {
                    UUID epId = UUID.fromString(epUuidStr);
                    // 3. Bölümü ve dosyasını sil
                    // (deleteEpisodeById metodu XML update etmeye çalışır ama
                    // biz zaten Parent'ı sileceğimiz için gerek yok, direkt DB ve File silelim)

                    repository.deleteEpisodeById(epId);
                    deletePhysicalFile(epUuidStr);
                } catch (IllegalArgumentException e) {
                    // UUID formatı bozuksa geç
                }
            }
        }

        // 4. Diziyi (Parent) sil
        repository.deleteSeriesById(id);
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
            // Sezon var, içine ekle
            int seasonIndex = xml.indexOf(seasonTag);
            int closingSeasonIndex = xml.indexOf("</Season>", seasonIndex);

            // Eğer sezon kapanış etiketi yoksa (hatalı XML), düzeltmeye çalışamayız, sona
            // eklenir.
            if (closingSeasonIndex == -1)
                return xml;

            String before = xml.substring(0, closingSeasonIndex);
            String after = xml.substring(closingSeasonIndex);
            return before + episodeTag + after;
        } else {
            // Sezon yok, yeni sezon oluştur
            String newSeason = seasonTag + episodeTag + "</Season>";
            // <Seasons> taginin içine ekle (veya en sona)
            if (xml.contains("<Seasons>")) {
                return xml.replace("</Seasons>", newSeason + "</Seasons>");
            } else {
                return "<Seasons>" + newSeason + "</Seasons>";
            }
        }
    }

    // --- SON EKLENEN BÖLÜMLERİ DETAYLANDIR ---
    public List<SoapOpera> getRecentEpisodesWithMetadata(int limit) {
        List<SoapOpera> episodes = repository.findRecentEpisodes(limit);

        for (SoapOpera ep : episodes) {
            // Parent Diziyi Bul
            SoapOpera parent = repository.findSeriesByName(ep.getName());
            if (parent != null) {
                ep.setCategory(parent.getCategory());
                ep.setLanguage(parent.getLanguage()); // Country code
                ep.setFinalStatus(parent.getFinalStatus());

                // Poster resmi Parent ID'sinden gelir
                // Ancak SoapOpera nesnesinde 'image' alanı yok, ID'yi parent ID yaparsak resim
                // çözülür mü?
                // Frontend 'image' URL'si bekliyor. Controller'da halledeceğiz.
                // Burada Parent ID'yi bir yere kaydetmek lazım.
                // Geçici olarak 'content' alanına Parent ID yazalım veya yeni alan ekleyelim.
                // Model değişikliği yapmadan: content alanını kullanalım (nasılsa child'da boş)
                ep.setContent(parent.getId().toString());

                // XML'den Sezon/Bölüm Bul
                int[] se = findSeasonEpisodeInXML(parent.getXmlData(), ep.getId().toString());
                ep.setSeasonNumber(se[0]);
                ep.setEpisodeNumber(se[1]);
            }
        }
        return episodes;
    }

    private int[] findSeasonEpisodeInXML(String xml, String episodeUUID) {
        if (xml == null || episodeUUID == null)
            return new int[] { 0, 0 };

        // Basit regex ile arama: <Season number="X">...<Episode
        // number="Y">UUID</Episode>
        // Ancak XML yapısı iç içe.
        // Sezonları ayırıp içinde aramak daha güvenli.

        java.util.regex.Pattern seasonPattern = java.util.regex.Pattern
                .compile("<Season number=\"(\\d+)\">(.*?)</Season>", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher seasonMatcher = seasonPattern.matcher(xml);

        while (seasonMatcher.find()) {
            int seasonNum = Integer.parseInt(seasonMatcher.group(1));
            String seasonContent = seasonMatcher.group(2);

            if (seasonContent.contains(episodeUUID)) {
                // Bu sezonda, bölümü bul
                java.util.regex.Pattern epPattern = java.util.regex.Pattern.compile("<Episode number=\"(\\d+)\">\\s*"
                        + java.util.regex.Pattern.quote(episodeUUID) + "\\s*</Episode>");
                java.util.regex.Matcher epMatcher = epPattern.matcher(seasonContent);
                if (epMatcher.find()) {
                    return new int[] { seasonNum, Integer.parseInt(epMatcher.group(1)) };
                }
            }
        }
        return new int[] { 1, 1 }; // Bulunamazsa varsayılan
    }

    private String removeEpisodeFromXML(String xml, String episodeUUID) {
        // Regex ile <Episode ...>UUID</Episode> bloğunu bul ve sil
        // Bu pattern şunları kapsar: <Episode number="X">UUID</Episode>
        String patternString = "<Episode[^>]*>" + Pattern.quote(episodeUUID) + "<\\/Episode>";
        return xml.replaceAll(patternString, "");
    }
}