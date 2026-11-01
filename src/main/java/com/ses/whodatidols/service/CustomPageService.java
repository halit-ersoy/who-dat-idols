package com.ses.whodatidols.service;

import com.ses.whodatidols.model.CustomPage;
import com.ses.whodatidols.repository.CustomPageRepository;
import com.ses.whodatidols.viewmodel.CustomPageDto;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class CustomPageService {

    private static final Logger logger = LoggerFactory.getLogger(CustomPageService.class);

    private final CustomPageRepository customPageRepository;

    @Value("${custom.pages.upload.dir:uploads/custom-pages}")
    private String uploadDirProperty;

    private Path baseUploadPath;

    private static final Set<String> RESERVED_SLUGS = Set.of(
            "admin", "api", "login", "login-admin", "logout", "logout-admin",
            "register", "profile", "settings", "history", "favorites", "sss",
            "about", "privacy_policy", "terms_of_use", "watch", "bl-dizileri",
            "coming-soon", "programlar", "diziler", "filmler", "search",
            "panel", "static", "elements", "homepage", "uploads", "error",
            "actuator", "streamlive", "manifest.json", "sw.js", "robots.txt",
            "sitemap.xml", "favicon.ico", "series", "movies", "episodes",
            "actors", "feedbacks", "contact"
    );

    private static final Set<String> EDITABLE_EXTENSIONS = Set.of(
            "html", "htm", "css", "js", "json", "txt", "svg", "xml", "md"
    );

    public CustomPageService(CustomPageRepository customPageRepository) {
        this.customPageRepository = customPageRepository;
    }

    /**
     * Filtre üzerinden doğrudan HTTP cevabı yazar
     */
    public boolean servePageDirectly(String slug, String subPath, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<CustomPage> pageOpt = findActiveBySlug(slug);
        if (pageOpt.isEmpty()) {
            return false;
        }

        CustomPage page = pageOpt.get();

        // Domain ve Hedef Site Kontrolü
        String targetSite = page.getTargetSite() != null ? page.getTargetSite() : "whodatidols";
        String serverName = request != null ? request.getServerName() : "";
        boolean isRequestToBlDizi = serverName != null && serverName.toLowerCase(Locale.ROOT).contains("bldizi");
        boolean isLocalhost = serverName != null && (serverName.equalsIgnoreCase("localhost") || serverName.equals("127.0.0.1"));

        if ("bldizi".equalsIgnoreCase(targetSite) && !isRequestToBlDizi) {
            String target = isLocalhost ? "http://localhost:8082/" + slug + (subPath != null ? subPath : "")
                    : "https://bldizi.com/" + slug + (subPath != null ? subPath : "");
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.setHeader("Location", target);
            return true;
        } else if ("whodatidols".equalsIgnoreCase(targetSite) && isRequestToBlDizi) {
            String target = isLocalhost ? "http://localhost:8080/" + slug + (subPath != null ? subPath : "")
                    : "https://whodatidols.com/" + slug + (subPath != null ? subPath : "");
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.setHeader("Location", target);
            return true;
        }

        Path targetDir = baseUploadPath.resolve(page.getSlug()).normalize();
        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            return false;
        }

        Path requestedFile;
        if (subPath == null || subPath.isBlank() || subPath.equals("/")) {
            requestedFile = targetDir.resolve(page.getEntryFile()).normalize();
        } else {
            String cleanSubPath = subPath.startsWith("/") ? subPath.substring(1) : subPath;
            requestedFile = targetDir.resolve(cleanSubPath).normalize();
        }

        if (!requestedFile.startsWith(targetDir)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return true;
        }

        if (Files.isDirectory(requestedFile)) {
            requestedFile = requestedFile.resolve("index.html").normalize();
        }

        if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return true;
        }

        MediaType mediaType = determineMediaType(requestedFile.getFileName().toString());
        response.setContentType(mediaType.toString());
        response.setContentLengthLong(Files.size(requestedFile));
        response.setHeader("Cache-Control", mediaType.equals(MediaType.TEXT_HTML) ? "no-cache" : "public, max-age=86400");

        try (InputStream is = Files.newInputStream(requestedFile); OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
            os.flush();
        }
        return true;
    }

    @PostConstruct
    public void init() {
        try {
            baseUploadPath = Paths.get(uploadDirProperty).toAbsolutePath().normalize();
            if (!Files.exists(baseUploadPath)) {
                Files.createDirectories(baseUploadPath);
            }
            logger.info("CustomPage upload directory initialized at: {}", baseUploadPath);
        } catch (IOException e) {
            logger.error("Could not create CustomPage upload directory: {}", e.getMessage(), e);
        }
    }

    public List<CustomPageDto> getAllPages() {
        return customPageRepository.findAll().stream()
                .map(CustomPageDto::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<CustomPage> findActiveBySlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        return customPageRepository.findActiveBySlug(slug.trim());
    }

    public void validateSlug(String slug, UUID excludeId) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Uzantı (URL slug) boş olamaz.");
        }
        String clean = slug.trim().toLowerCase(Locale.ROOT);
        if (!clean.matches("^[a-zA-Z0-9-_]+$")) {
            throw new IllegalArgumentException("Uzantı sadece harf, rakam, tire (-) ve alt çizgi (_) içerebilir.");
        }
        if (clean.length() < 2 || clean.length() > 64) {
            throw new IllegalArgumentException("Uzantı en az 2, en fazla 64 karakter olmalıdır.");
        }
        if (RESERVED_SLUGS.contains(clean)) {
            throw new IllegalArgumentException("'" + clean + "' sistem için ayrılmış özel bir rotadır, kullanılamaz.");
        }

        Optional<CustomPage> existing = customPageRepository.findBySlug(clean);
        if (existing.isPresent()) {
            if (excludeId == null || !existing.get().getId().equals(excludeId)) {
                throw new IllegalArgumentException("'" + clean + "' uzantısı zaten başka bir sayfa tarafından kullanılıyor.");
            }
        }
    }

    /**
     * ZIP veya tekil HTML dosyasını işler, açar ve CustomPage olarak kaydeder.
     */
    public CustomPage processUpload(MultipartFile file, String slug, String title, String targetSite) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Lütfen yüklenecek bir dosya seçiniz.");
        }

        String cleanSlug = slug.trim().toLowerCase(Locale.ROOT);
        validateSlug(cleanSlug, null);

        String cleanSite = (targetSite != null && !targetSite.isBlank()) ? targetSite.toLowerCase(Locale.ROOT).trim() : "whodatidols";
        if (!Set.of("whodatidols", "bldizi", "all").contains(cleanSite)) {
            cleanSite = "whodatidols";
        }

        if (title == null || title.isBlank()) {
            title = cleanSlug;
        }

        Path targetDir = baseUploadPath.resolve(cleanSlug).normalize();
        if (!targetDir.startsWith(baseUploadPath)) {
            throw new SecurityException("Geçersiz hedef klasör yolu.");
        }

        // Eğer mevcutsa önceki klasörü temizle
        if (Files.exists(targetDir)) {
            FileSystemUtils.deleteRecursively(targetDir);
        }
        Files.createDirectories(targetDir);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        boolean isZip = originalFilename.toLowerCase().endsWith(".zip");

        String entryFile = "index.html";

        if (isZip) {
            extractZipWithSafety(file.getInputStream(), targetDir);
            flattenSingleRootFolder(targetDir);

            // Giriş dosyasını tespit et
            if (Files.exists(targetDir.resolve("index.html"))) {
                entryFile = "index.html";
            } else if (Files.exists(targetDir.resolve("index.htm"))) {
                entryFile = "index.htm";
            } else {
                // Herhangi bir .html dosyası var mı bul
                try (Stream<Path> stream = Files.walk(targetDir, 2)) {
                    Optional<Path> htmlFile = stream
                            .filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".html"))
                            .findFirst();
                    if (htmlFile.isPresent()) {
                        entryFile = targetDir.relativize(htmlFile.get()).toString().replace("\\", "/");
                    } else {
                        // Temizle ve hata fırlat
                        FileSystemUtils.deleteRecursively(targetDir);
                        throw new IllegalArgumentException("Yüklenen ZIP içerisinde ana sayfa (index.html) bulunamadı!");
                    }
                }
            }
        } else if (originalFilename.toLowerCase().endsWith(".html") || originalFilename.toLowerCase().endsWith(".htm")) {
            Path targetFile = targetDir.resolve("index.html");
            file.transferTo(targetFile.toFile());
            entryFile = "index.html";
        } else {
            FileSystemUtils.deleteRecursively(targetDir);
            throw new IllegalArgumentException("Yalnızca .zip veya .html uzantılı dosya yükleyebilirsiniz.");
        }

        // Klasör istatistiklerini hesapla
        long totalSize = calculateDirectorySize(targetDir);
        int fileCount = countDirectoryFiles(targetDir);

        Optional<CustomPage> existingOpt = customPageRepository.findBySlug(cleanSlug);
        CustomPage page;
        if (existingOpt.isPresent()) {
            page = existingOpt.get();
            page.setTitle(title.trim());
            page.setEntryFile(entryFile);
            page.setStoragePath(targetDir.toString());
            page.setTotalSize(totalSize);
            page.setFileCount(fileCount);
            page.setActive(true);
            page.setTargetSite(cleanSite);
            page.setUpdatedAt(Instant.now());
            customPageRepository.update(page);
        } else {
            page = new CustomPage();
            page.setTitle(title.trim());
            page.setSlug(cleanSlug);
            page.setTargetSite(cleanSite);
            page.setEntryFile(entryFile);
            page.setStoragePath(targetDir.toString());
            page.setTotalSize(totalSize);
            page.setFileCount(fileCount);
            page.setActive(true);
            customPageRepository.save(page);
        }

        logger.info("CustomPage successfully deployed for slug: /{}. Files: {}, Size: {} bytes", cleanSlug, fileCount, totalSize);
        return page;
    }

    public boolean updateTargetSite(UUID id, String targetSite) {
        Optional<CustomPage> pageOpt = customPageRepository.findById(id);
        if (pageOpt.isEmpty()) return false;
        CustomPage page = pageOpt.get();
        String cleanSite = (targetSite != null && !targetSite.isBlank()) ? targetSite.toLowerCase(Locale.ROOT).trim() : "whodatidols";
        if (!Set.of("whodatidols", "bldizi", "all").contains(cleanSite)) {
            cleanSite = "whodatidols";
        }
        page.setTargetSite(cleanSite);
        customPageRepository.update(page);
        return true;
    }

    /**
     * Zip Slip zafiyetine karşı güvenli ZIP çıkarma işlemi
     */
    private void extractZipWithSafety(InputStream inputStream, Path targetDir) throws IOException {
        byte[] buffer = new byte[8192];
        try (ZipInputStream zis = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path resolvedPath = targetDir.resolve(entry.getName()).normalize();
                if (!resolvedPath.startsWith(targetDir)) {
                    throw new SecurityException("Güvenlik ihlali: Zip Slip tespiti (" + entry.getName() + ")");
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Path parent = resolvedPath.getParent();
                    if (parent != null && !Files.exists(parent)) {
                        Files.createDirectories(parent);
                    }
                    try (OutputStream os = Files.newOutputStream(resolvedPath)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * ZIP yalnızca tek bir ana klasör içeriyorsa (örn. my-site/index.html),
     * dosyaları bir üst klasöre taşıyarak kök dizine çeker.
     */
    private void flattenSingleRootFolder(Path targetDir) {
        try (Stream<Path> entries = Files.list(targetDir)) {
            List<Path> list = entries.collect(Collectors.toList());
            if (list.size() == 1 && Files.isDirectory(list.get(0))) {
                Path singleInnerDir = list.get(0);
                Path tempDir = targetDir.resolveSibling(targetDir.getFileName() + "_flatten_temp");
                Files.move(singleInnerDir, tempDir, StandardCopyOption.ATOMIC_MOVE);
                FileSystemUtils.deleteRecursively(targetDir);
                Files.move(tempDir, targetDir, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (Exception e) {
            logger.warn("Could not flatten root folder: {}", e.getMessage());
        }
    }

    /**
     * Sayfayı ve diskteki klasörünü tamamen siler
     */
    public boolean deletePage(UUID id) {
        Optional<CustomPage> pageOpt = customPageRepository.findById(id);
        if (pageOpt.isEmpty()) return false;

        CustomPage page = pageOpt.get();
        Path targetDir = baseUploadPath.resolve(page.getSlug()).normalize();

        try {
            if (Files.exists(targetDir) && targetDir.startsWith(baseUploadPath)) {
                FileSystemUtils.deleteRecursively(targetDir);
                logger.info("Deleted custom page folder: {}", targetDir);
            }
        } catch (Exception e) {
            logger.error("Error deleting folder for page: {}", page.getSlug(), e);
        }

        return customPageRepository.deleteById(id);
    }

    public boolean toggleActive(UUID id) {
        return customPageRepository.toggleActive(id);
    }

    /**
     * Ziyaretçiye sayfanın HTML veya alt dosyasını (css, js, resim vb.) servis eder
     */
    public ResponseEntity<Resource> servePageFile(String slug, String subPath, HttpServletRequest request) {
        Optional<CustomPage> pageOpt = findActiveBySlug(slug);
        if (pageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CustomPage page = pageOpt.get();
        Path targetDir = baseUploadPath.resolve(page.getSlug()).normalize();
        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            return ResponseEntity.notFound().build();
        }

        Path requestedFile;
        if (subPath == null || subPath.isBlank() || subPath.equals("/")) {
            requestedFile = targetDir.resolve(page.getEntryFile()).normalize();
        } else {
            String cleanSubPath = subPath.startsWith("/") ? subPath.substring(1) : subPath;
            requestedFile = targetDir.resolve(cleanSubPath).normalize();
        }

        // Dizin atlama kontrolü
        if (!requestedFile.startsWith(targetDir)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (Files.isDirectory(requestedFile)) {
            requestedFile = requestedFile.resolve("index.html").normalize();
        }

        if (!Files.exists(requestedFile) || !Files.isRegularFile(requestedFile)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(requestedFile.toFile());
        MediaType mediaType = determineMediaType(requestedFile.getFileName().toString());

        boolean isHtml = mediaType.equals(MediaType.TEXT_HTML);
        CacheControl cacheControl = isHtml
                ? CacheControl.noCache()
                : CacheControl.maxAge(1, java.util.concurrent.TimeUnit.DAYS).cachePublic();

        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .contentType(mediaType)
                .body(resource);
    }

    /**
     * Kod editörü için düzenlenebilir dosyaları listeler
     */
    public List<String> listEditableFiles(UUID id) {
        Optional<CustomPage> pageOpt = customPageRepository.findById(id);
        if (pageOpt.isEmpty()) return Collections.emptyList();

        CustomPage page = pageOpt.get();
        Path targetDir = baseUploadPath.resolve(page.getSlug()).normalize();
        if (!Files.exists(targetDir)) return Collections.emptyList();

        List<String> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(targetDir, 5)) {
            stream.filter(Files::isRegularFile)
                    .forEach(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        int dotIdx = name.lastIndexOf('.');
                        if (dotIdx != -1) {
                            String ext = name.substring(dotIdx + 1);
                            if (EDITABLE_EXTENSIONS.contains(ext)) {
                                String rel = targetDir.relativize(p).toString().replace("\\", "/");
                                result.add(rel);
                            }
                        }
                    });
        } catch (IOException e) {
            logger.error("Error listing editable files: ", e);
        }

        // index.html her zaman en başta görünsün
        result.sort((a, b) -> {
            if (a.equalsIgnoreCase("index.html")) return -1;
            if (b.equalsIgnoreCase("index.html")) return 1;
            return a.compareToIgnoreCase(b);
        });

        return result;
    }

    /**
     * Dosya içeriğini metin olarak okur
     */
    public String readFileContent(UUID id, String relativePath) throws IOException {
        Optional<CustomPage> pageOpt = customPageRepository.findById(id);
        if (pageOpt.isEmpty()) throw new FileNotFoundException("Sayfa bulunamadı.");

        CustomPage page = pageOpt.get();
        Path targetDir = baseUploadPath.resolve(page.getSlug()).normalize();
        Path targetFile = targetDir.resolve(relativePath).normalize();

        if (!targetFile.startsWith(targetDir) || !Files.exists(targetFile) || !Files.isRegularFile(targetFile)) {
            throw new FileNotFoundException("Dosya bulunamadı veya erişim engellendi.");
        }

        return Files.readString(targetFile, StandardCharsets.UTF_8);
    }

    /**
     * Dosya içeriğini günceller ve sayfa istatistiklerini yeniler
     */
    public void saveFileContent(UUID id, String relativePath, String content) throws IOException {
        Optional<CustomPage> pageOpt = customPageRepository.findById(id);
        if (pageOpt.isEmpty()) throw new FileNotFoundException("Sayfa bulunamadı.");

        CustomPage page = pageOpt.get();
        Path targetDir = baseUploadPath.resolve(page.getSlug()).normalize();
        Path targetFile = targetDir.resolve(relativePath).normalize();

        if (!targetFile.startsWith(targetDir) || !Files.exists(targetFile) || !Files.isRegularFile(targetFile)) {
            throw new FileNotFoundException("Dosya bulunamadı veya erişim engellendi.");
        }

        Files.writeString(targetFile, content != null ? content : "", StandardCharsets.UTF_8);

        // İstatistikleri güncelle
        page.setTotalSize(calculateDirectorySize(targetDir));
        page.setFileCount(countDirectoryFiles(targetDir));
        page.setUpdatedAt(Instant.now());
        customPageRepository.update(page);
    }

    private MediaType determineMediaType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return MediaType.TEXT_HTML;
        if (lower.endsWith(".css")) return MediaType.valueOf("text/css; charset=UTF-8");
        if (lower.endsWith(".js") || lower.endsWith(".mjs")) return MediaType.valueOf("application/javascript; charset=UTF-8");
        if (lower.endsWith(".json")) return MediaType.APPLICATION_JSON;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (lower.endsWith(".svg")) return MediaType.valueOf("image/svg+xml");
        if (lower.endsWith(".webp")) return MediaType.valueOf("image/webp");
        if (lower.endsWith(".ico")) return MediaType.valueOf("image/x-icon");
        if (lower.endsWith(".woff2")) return MediaType.valueOf("font/woff2");
        if (lower.endsWith(".woff")) return MediaType.valueOf("font/woff");
        if (lower.endsWith(".ttf")) return MediaType.valueOf("font/ttf");
        if (lower.endsWith(".mp4")) return MediaType.valueOf("video/mp4");
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        if (lower.endsWith(".txt")) return MediaType.TEXT_PLAIN;

        MediaType factoryType = MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return factoryType;
    }

    private long calculateDirectorySize(Path path) {
        try (Stream<Path> stream = Files.walk(path)) {
            return stream.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0L;
                        }
                    }).sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    private int countDirectoryFiles(Path path) {
        try (Stream<Path> stream = Files.walk(path)) {
            return (int) stream.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            return 0;
        }
    }
}
