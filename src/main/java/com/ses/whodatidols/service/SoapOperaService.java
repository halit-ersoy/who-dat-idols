package com.ses.whodatidols.service;

import com.ses.whodatidols.model.SoapOpera;
import com.ses.whodatidols.repository.SoapOperaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SoapOperaService {

    private final SoapOperaRepository soapOperaRepository;

    // Properties dosyasındaki "Soap Operas" yolunu buraya çekiyoruz
    @Value("${media.source.soap_operas.path}")
    private String soapOperasPath;

    public SoapOperaService(SoapOperaRepository soapOperaRepository) {
        this.soapOperaRepository = soapOperaRepository;
    }

    public void saveSoapOperaWithFile(SoapOpera soapOpera, MultipartFile file) throws IOException {
        UUID uuid = UUID.randomUUID();
        soapOpera.setId(uuid);
        soapOpera.setUploadDate(LocalDateTime.now());

        if (file != null && !file.isEmpty()) {
            Path uploadPath = Paths.get(soapOperasPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = uuid.toString() + ".mp4";
            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Dinamik Erişim Adresi
            soapOpera.setContent("/media/video/" + uuid.toString());
        }

        soapOperaRepository.save(soapOpera);
    }
}