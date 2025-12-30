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

    public void updateSeriesMetadata(SoapOpera s) {
        repository.updateSeriesMetadata(s);
    }

    @Transactional
    public void saveEpisodeWithFile(SoapOpera soapOpera, MultipartFile file) throws IOException {
        // 1. DİZİ KONTROLÜ (Parent)
        SoapOpera existingSeries = repository.findSeriesByName(soapOpera.getName());
        UUID seriesId;
        String currentXML;

        if (existingSeries == null) {
            // Dizi yoksa oluştur
            seriesId = UUID.randomUUID();
            soapOpera.setId(seriesId); // Parent ID
            repository.createSeries(soapOpera);
            currentXML = "<Seasons></Seasons>";
        } else {
            // Dizi varsa bilgilerini al
            seriesId = existingSeries.getId();
            currentXML = existingSeries.getXmlData();
            // Formdan gelen metadata ile mevcut diziyi güncellemek istersek:
            // existingSeries.setContent(soapOpera.getContent());
            // repository.updateSeriesMetadata(existingSeries);
        }

        // 2. DOSYA VE BÖLÜM İŞLEMLERİ (Child)
        UUID episodeId = UUID.randomUUID(); // Yeni Bölüm ID'si

        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(soapOperasPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            // Dosya adı: Bölüm UUID'si
            String fileName = episodeId.toString() + ".mp4";
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Süre Hesapla
            int duration = FFmpegUtils.getVideoDurationInMinutes(filePath.toString());
            soapOpera.setTime(duration > 0 ? duration : 1);
        }

        // Bölümü Child Tabloya Kaydet
        SoapOpera episodeData = new SoapOpera();
        episodeData.setId(episodeId);
        episodeData.setTime(soapOpera.getTime());
        episodeData.setYear(soapOpera.getYear()); // Bölüm yılı
        episodeData.setUploadDate(LocalDateTime.now());
        repository.saveEpisode(episodeData);

        // 3. XML GÜNCELLEME
        String updatedXML = injectEpisodeToXML(currentXML, soapOpera.getSeasonNumber(), soapOpera.getEpisodeNumber(), episodeId.toString());
        repository.updateSeriesXML(seriesId, updatedXML);
    }

    // --- XML SİHİRBAZI ---
    private String injectEpisodeToXML(String xml, int seasonNum, int episodeNum, String episodeUUID) {
        // Basit String manipülasyonu ile XML'e düğüm ekliyoruz (DOM parser yerine daha hafif)
        // XML Yapısı: <Seasons><Season number="1"><Episode number="1">UUID</Episode></Season></Seasons>

        String seasonTag = "<Season number=\"" + seasonNum + "\">";
        String episodeTag = "<Episode number=\"" + episodeNum + "\">" + episodeUUID + "</Episode>";

        if (xml.contains(seasonTag)) {
            // Sezon var, içine bölümü ekle (veya varsa güncelle - basitçe sona ekliyoruz)
            // Not: Tam bir XML parser daha güvenli olurdu ama string replace ile:
            // Sezon kapanış etiketinden hemen önceye ekle
            String seasonEndTag = "</Season>";
            int seasonStartIndex = xml.indexOf(seasonTag);
            int seasonEndIndex = xml.indexOf(seasonEndTag, seasonStartIndex);

            // Eğer bu bölüm zaten varsa, eskiyi silmek gerekebilir ama şimdilik 'append' yapıyoruz.
            // XML içinde o aralığa bölüm etiketini sokuyoruz
            String before = xml.substring(0, seasonEndIndex);
            String after = xml.substring(seasonEndIndex);
            return before + episodeTag + after;
        } else {
            // Sezon yok, yeni sezon oluştur ve içine bölümü koy
            String newSeasonBlock = seasonTag + episodeTag + "</Season>";
            // <Seasons> etiketinin içine (sonuna) ekle
            return xml.replace("</Seasons>", newSeasonBlock + "</Seasons>");
        }
    }
}