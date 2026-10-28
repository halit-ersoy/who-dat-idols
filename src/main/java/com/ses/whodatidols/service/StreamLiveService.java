package com.ses.whodatidols.service;

import com.ses.whodatidols.model.Episode;
import com.ses.whodatidols.model.Movie;
import com.ses.whodatidols.model.Series;
import com.ses.whodatidols.model.StreamLive;
import com.ses.whodatidols.model.StreamLiveChat;
import com.ses.whodatidols.model.Person;
import com.ses.whodatidols.repository.PersonRepository;
import com.ses.whodatidols.repository.MovieRepository;
import com.ses.whodatidols.repository.SeriesRepository;
import com.ses.whodatidols.repository.StreamLiveRepository;
import com.ses.whodatidols.util.FFmpegUtils;
import com.ses.whodatidols.viewmodel.CreateStreamLiveRequest;
import com.ses.whodatidols.viewmodel.StreamLiveChatMessageDto;
import com.ses.whodatidols.viewmodel.StreamLiveStatusDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class StreamLiveService {

    private static final Logger logger = LoggerFactory.getLogger(StreamLiveService.class);
    private static final long ONE_HOUR_MILLIS = 60 * 60 * 1000L;
    private static final long CHAT_COOLDOWN_MILLIS = 10000L; // 10 saniye yavaş mod (slow mode)
    private static final int MAX_MESSAGE_LENGTH = 200;

    @Value("${media.source.movies.path}")
    private String moviesPath;

    @Value("${media.source.soap_operas.path:}")
    private String soapOperasPath;

    private final StreamLiveRepository streamLiveRepository;
    private final MovieRepository movieRepository;
    private final SeriesRepository seriesRepository;
    private final FFmpegUtils ffmpegUtils;
    private final TvMazeService tvMazeService;
    private final PersonRepository personRepository;
    private final StreamLiveViewerTracker streamLiveViewerTracker;

    // Rate-limiting: User ID -> Last sent message timestamp
    private final Map<UUID, Long> userLastMessageTimes = new ConcurrentHashMap<>();

    public StreamLiveService(StreamLiveRepository streamLiveRepository,
                             MovieRepository movieRepository,
                             SeriesRepository seriesRepository,
                             FFmpegUtils ffmpegUtils,
                             TvMazeService tvMazeService,
                             PersonRepository personRepository,
                             StreamLiveViewerTracker streamLiveViewerTracker) {
        this.streamLiveRepository = streamLiveRepository;
        this.movieRepository = movieRepository;
        this.seriesRepository = seriesRepository;
        this.ffmpegUtils = ffmpegUtils;
        this.tvMazeService = tvMazeService;
        this.personRepository = personRepository;
        this.streamLiveViewerTracker = streamLiveViewerTracker;
    }

    public int getViewerCount(UUID streamId) {
        return streamLiveViewerTracker != null ? streamLiveViewerTracker.getViewerCount(streamId) : 0;
    }

    public StreamLiveStatusDto getCurrentOrUpcomingStreamStatus() {
        StreamLive stream = streamLiveRepository.findActiveOrUpcomingStream();
        if (stream == null) {
            return null;
        }
        return toStatusDto(stream);
    }

    public StreamLiveStatusDto getScheduleOrLatestStreamStatus() {
        StreamLive stream = streamLiveRepository.findScheduleOrLatestStream();
        if (stream == null) {
            return null;
        }
        StreamLiveStatusDto dto = toStatusDto(stream);
        if (dto == null || (!"LIVE".equals(dto.getStatus()) && !"UPCOMING".equals(dto.getStatus()))) {
            return null;
        }
        return dto;
    }

    public StreamLiveStatusDto getStreamStatusById(UUID id) {
        StreamLive stream = streamLiveRepository.findStreamById(id);
        if (stream == null) {
            return null;
        }
        return toStatusDto(stream);
    }

    public StreamLive getStreamById(UUID id) {
        return streamLiveRepository.findStreamById(id);
    }

    public StreamLiveStatusDto toStatusDto(StreamLive stream) {
        long now = System.currentTimeMillis();
        long start = stream.getScheduledStartTime().getTime();
        long end = stream.getEndTime().getTime();
        long totalSeconds = stream.getDurationSeconds() > 0
                ? stream.getDurationSeconds()
                : Math.max(0, (long) stream.getDurationMinutes() * 60L);

        String status;
        long liveEdgeSeconds;
        long remainingStartSeconds = 0;

        if (now < start) {
            status = "UPCOMING";
            liveEdgeSeconds = 0;
            remainingStartSeconds = (start - now) / 1000L;
        } else if (now <= end) {
            status = "LIVE";
            liveEdgeSeconds = Math.min(totalSeconds, (now - start) / 1000L);
        } else if (now <= end + ONE_HOUR_MILLIS) {
            status = "ENDED";
            liveEdgeSeconds = totalSeconds;
        } else {
            status = "CLOSED";
            liveEdgeSeconds = totalSeconds;
        }

        // Sohbet kontrolü: Yayından 10 dakika önce başlar, yayından 1 saat sonraya kadar açıktır
        long tenMinutesBeforeStart = start - (10L * 60L * 1000L);
        boolean chatOpen;
        long chatOpenInSeconds = 0;
        if (now < tenMinutesBeforeStart) {
            chatOpen = false;
            chatOpenInSeconds = (tenMinutesBeforeStart - now) / 1000L;
        } else if (now <= end + ONE_HOUR_MILLIS) {
            chatOpen = true;
        } else {
            chatOpen = false;
        }

        StreamLiveStatusDto dto = new StreamLiveStatusDto(
                stream.getId(),
                stream.getTitle(),
                stream.getDescription(),
                stream.getContentType(),
                stream.getContentId(),
                stream.getContentName(),
                stream.getSlug(),
                stream.getPosterUrl(),
                stream.getVideoUrl(),
                start,
                stream.getDurationMinutes(),
                (int) totalSeconds,
                end,
                now,
                liveEdgeSeconds,
                remainingStartSeconds,
                status,
                stream.isActive(),
                chatOpen,
                chatOpenInSeconds
        );

        if (streamLiveViewerTracker != null) {
            dto.setViewerCount(streamLiveViewerTracker.getViewerCount(stream.getId()));
        }

        return dto;
    }

    /**
     * Resolves the video file path for any contentId (.mp4 or playlist.m3u8).
     */
    public String resolveContentVideoPath(UUID contentId, String contentType) {
        if (contentId == null) return null;

        // 1. Check streamlive folder
        Path streamLiveDir = Paths.get(moviesPath).resolve("streamlive");
        Path streamLiveMp4 = streamLiveDir.resolve(contentId + ".mp4");
        if (Files.exists(streamLiveMp4)) return streamLiveMp4.toString();
        Path streamLiveHls = streamLiveDir.resolve("hls").resolve(contentId.toString()).resolve("playlist.m3u8");
        if (Files.exists(streamLiveHls)) return streamLiveHls.toString();

        // 2. Check movie folder
        if (moviesPath != null && !moviesPath.isBlank()) {
            Path movieMp4 = Paths.get(moviesPath).resolve(contentId + ".mp4");
            if (Files.exists(movieMp4)) return movieMp4.toString();
            Path movieHls = Paths.get(moviesPath).resolve("hls").resolve(contentId.toString()).resolve("playlist.m3u8");
            if (Files.exists(movieHls)) return movieHls.toString();
        }

        // 3. Check soap_operas folder
        if (soapOperasPath != null && !soapOperasPath.isBlank()) {
            Path soapMp4 = Paths.get(soapOperasPath).resolve(contentId + ".mp4");
            if (Files.exists(soapMp4)) return soapMp4.toString();
            Path soapHls = Paths.get(soapOperasPath).resolve("hls").resolve(contentId.toString()).resolve("playlist.m3u8");
            if (Files.exists(soapHls)) return soapHls.toString();
        }

        return null;
    }

    /**
     * Mesaj gönderme işlemi:
     * - Max 200 karakter kontrolü
     * - 10 saniye slow mode kontrolü
     */
    public StreamLiveChatMessageDto processChatMessage(UUID streamLiveId, UUID userId, String nickname, String messageText) {
        if (messageText == null) {
            throw new IllegalArgumentException("Mesaj boş olamaz.");
        }
        String trimmed = messageText.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Mesaj boş olamaz.");
        }
        if (trimmed.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Mesaj en fazla " + MAX_MESSAGE_LENGTH + " karakter olabilir.");
        }

        // 10 saniyelik yavaş mod kontrolü
        long now = System.currentTimeMillis();
        Long lastTime = userLastMessageTimes.get(userId);
        if (lastTime != null && (now - lastTime) < CHAT_COOLDOWN_MILLIS) {
            long remainingSec = ((CHAT_COOLDOWN_MILLIS - (now - lastTime)) / 1000) + 1;
            throw new IllegalStateException("Lütfen yeni mesaj göndermek için " + remainingSec + " saniye bekleyin.");
        }

        // Yayının açık olup olmadığını kontrol et (yayın bittikten sonra 1 saate kadar sohbet edilebilir)
        StreamLive stream = streamLiveRepository.findStreamById(streamLiveId);
        if (stream == null || !stream.isActive()) {
            throw new IllegalStateException("Yayın bulunamadı veya aktif değil.");
        }
        long start = stream.getScheduledStartTime().getTime();
        long tenMinutesBeforeStart = start - (10L * 60L * 1000L);
        if (now < tenMinutesBeforeStart) {
            long waitMinutes = ((tenMinutesBeforeStart - now) / 60000L) + 1;
            throw new IllegalStateException("Canlı sohbet yayının başlamasına 10 dakika kala açılacaktır.");
        }
        long end = stream.getEndTime().getTime();
        if (now > end + ONE_HOUR_MILLIS) {
            throw new IllegalStateException("Yayın ve sohbet süresi sona erdi.");
        }

        boolean isVerified = false;
        if (userId != null) {
            isVerified = personRepository.findById(userId).map(Person::isVerified).orElse(false);
        }

        userLastMessageTimes.put(userId, now);

        StreamLiveChat chat = new StreamLiveChat();
        chat.setId(UUID.randomUUID());
        chat.setStreamLiveId(streamLiveId);
        chat.setUserId(userId);
        chat.setNickname(nickname != null ? nickname : "Kullanıcı");
        chat.setMessage(trimmed);
        chat.setSentAt(new Timestamp(now));
        chat.setVerified(isVerified);

        streamLiveRepository.saveChatMessage(chat);

        return new StreamLiveChatMessageDto(
                chat.getId(),
                chat.getStreamLiveId(),
                chat.getUserId(),
                chat.getNickname(),
                chat.getMessage(),
                now,
                isVerified
        );
    }

    public List<StreamLiveChatMessageDto> getRecentChatHistory(UUID streamLiveId, int limit) {
        return streamLiveRepository.getRecentChatMessages(streamLiveId, Math.min(limit, 100))
                .stream()
                .map(c -> new StreamLiveChatMessageDto(
                        c.getId(),
                        c.getStreamLiveId(),
                        c.getUserId(),
                        c.getNickname(),
                        c.getMessage(),
                        c.getSentAt().getTime(),
                        c.isVerified()
                ))
                .collect(Collectors.toList());
    }

    public StreamLive scheduleStream(CreateStreamLiveRequest request) {
        try {
            return scheduleStream(request, null, null, null);
        } catch (IOException e) {
            throw new RuntimeException("StreamLive planlanırken hata oluştu: " + e.getMessage(), e);
        }
    }

    public StreamLive scheduleStream(CreateStreamLiveRequest request,
                                     MultipartFile file,
                                     MultipartFile image,
                                     String imageUrl) throws IOException {
        StreamLive stream = new StreamLive();
        stream.setId(UUID.randomUUID());
        stream.setTitle(request.getTitle() != null && !request.getTitle().isBlank() ? request.getTitle().trim() : "StreamLive Canlı Sinema");
        stream.setDescription(request.getDescription());
        stream.setContentType(request.getContentType() != null && !request.getContentType().isBlank() ? request.getContentType() : "Movie");

        UUID contentId = request.getContentId();
        int duration = request.getDurationMinutes();
        int durationSeconds = request.getDurationSeconds();
        String contentName = stream.getTitle();
        String slug = com.ses.whodatidols.util.SlugUtil.toSlug(stream.getTitle());
        String posterUrl = "";
        String videoUrl = "";

        Path streamLiveBasePath = Paths.get(moviesPath).resolve("streamlive").toAbsolutePath().normalize();
        if (!Files.exists(streamLiveBasePath)) {
            Files.createDirectories(streamLiveBasePath);
        }

        // 1. Yeni video dosyası yüklendiyse (Film ve Dizi içeriği gibi)
        if (file != null && !file.isEmpty()) {
            if (contentId == null) {
                contentId = UUID.randomUUID();
            }
            stream.setContentId(contentId);

            String fileName = contentId + ".mp4";
            Path filePath = streamLiveBasePath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // FFprobe ile hassas süreyi otomatik hesapla
            double probedSec = ffmpegUtils.getVideoDurationInSeconds(filePath.toString());
            if (probedSec > 0) {
                durationSeconds = (int) Math.round(probedSec);
                duration = (int) Math.ceil(probedSec / 60.0);
            }

            // Otomatik HLS Dönüştürme (Arka planda)
            final String input = filePath.toString();
            final String output = streamLiveBasePath.resolve("hls").resolve(contentId.toString()).toString();
            CompletableFuture.runAsync(() -> {
                try {
                    ffmpegUtils.convertToHls(input, output);
                } catch (Exception e) {
                    logger.error("StreamLive HLS conversion error: {}", e.getMessage());
                }
            });

            // Kapak Resmi Kaydetme
            if (image != null && !image.isEmpty()) {
                Path imagePath = streamLiveBasePath.resolve(contentId + ".jpg");
                Files.copy(image.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
                posterUrl = "/media/image//" + contentId;
            } else if (imageUrl != null && !imageUrl.isBlank()) {
                byte[] imgBytes = tvMazeService.downloadImage(imageUrl.trim());
                if (imgBytes != null && imgBytes.length > 0) {
                    Path imagePath = streamLiveBasePath.resolve(contentId + ".jpg");
                    Files.write(imagePath, imgBytes);
                    posterUrl = "/media/image/" + contentId;
                } else {
                    posterUrl = imageUrl.trim();
                }
            }

            videoUrl = "/media/video/" + contentId + "/playlist.m3u8";

        } else if (contentId != null) {
            // 2. Sunucuda mevcut olan bir içerik seçildiyse
            stream.setContentId(contentId);
            videoUrl = "/media/video/" + contentId + "/playlist.m3u8";

            if ("Movie".equalsIgnoreCase(request.getContentType())) {
                Movie movie = movieRepository.findMovieById(contentId);
                if (movie != null) {
                    contentName = movie.getName();
                    slug = movie.getSlug();
                    posterUrl = "/media/image/" + movie.getId();
                    if (duration <= 0) {
                        duration = movie.getDurationMinutes();
                    }
                }
            } else if ("Episode".equalsIgnoreCase(request.getContentType())) {
                Episode ep = seriesRepository.findEpisodeById(contentId);
                if (ep != null) {
                    Series series = seriesRepository.findSeriesById(ep.getSeriesId());
                    contentName = (series != null ? series.getName() + " - " : "") + "S" + ep.getSeasonNumber() + "E" + ep.getEpisodeNumber();
                    if (ep.getName() != null && !ep.getName().isBlank()) {
                        contentName += " (" + ep.getName() + ")";
                    }
                    slug = ep.getSlug();
                    posterUrl = "/media/image/" + ep.getSeriesId();
                    if (duration <= 0) {
                        duration = ep.getDurationMinutes();
                    }
                }
            }

            // Mevcut video dosyasını çözümle ve kesin süreyi ffprobe ile al
            String contentVideoPath = resolveContentVideoPath(contentId, request.getContentType());
            if (contentVideoPath != null) {
                double probedSec = ffmpegUtils.getVideoDurationInSeconds(contentVideoPath);
                if (probedSec > 0) {
                    durationSeconds = (int) Math.round(probedSec);
                    duration = (int) Math.ceil(probedSec / 60.0);
                }
            }
        } else {
            throw new IllegalArgumentException("Lütfen bir video dosyası yükleyin veya sunucudaki mevcut bir içeriği seçin.");
        }

        if (durationSeconds <= 0) {
            if (duration > 0) {
                durationSeconds = duration * 60;
            } else {
                duration = 90;
                durationSeconds = 90 * 60;
            }
        } else if (duration <= 0) {
            duration = (int) Math.ceil(durationSeconds / 60.0);
        }

        long startMillis = request.getScheduledStartTime();
        long endMillis = startMillis + ((long) durationSeconds * 1000L);

        stream.setContentName(contentName);
        stream.setSlug(slug);
        stream.setPosterUrl(posterUrl);
        stream.setVideoUrl(videoUrl);
        stream.setScheduledStartTime(new Timestamp(startMillis));
        stream.setDurationMinutes(duration);
        stream.setDurationSeconds(durationSeconds);
        stream.setEndTime(new Timestamp(endMillis));
        stream.setActive(true);
        stream.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        streamLiveRepository.createStreamLive(stream);
        return stream;
    }

    public StreamLive updateStream(UUID id,
                                   CreateStreamLiveRequest request,
                                   MultipartFile file,
                                   MultipartFile image,
                                   String imageUrl) throws IOException {
        StreamLive existing = streamLiveRepository.findStreamById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Güncellenecek yayın bulunamadı.");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            existing.setTitle(request.getTitle().trim());
            existing.setSlug(com.ses.whodatidols.util.SlugUtil.toSlug(existing.getTitle()));
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription().trim());
        }
        if (request.getContentType() != null && !request.getContentType().isBlank()) {
            existing.setContentType(request.getContentType());
        }

        int duration = request.getDurationMinutes();
        int durationSeconds = request.getDurationSeconds();

        Path streamLiveBasePath = Paths.get(moviesPath).resolve("streamlive").toAbsolutePath().normalize();
        if (!Files.exists(streamLiveBasePath)) {
            Files.createDirectories(streamLiveBasePath);
        }

        // 1. Yeni video dosyası yüklendiyse
        if (file != null && !file.isEmpty()) {
            UUID contentId = UUID.randomUUID();
            existing.setContentId(contentId);

            String fileName = contentId + ".mp4";
            Path filePath = streamLiveBasePath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            double probedSec = ffmpegUtils.getVideoDurationInSeconds(filePath.toString());
            if (probedSec > 0) {
                durationSeconds = (int) Math.round(probedSec);
                duration = (int) Math.ceil(probedSec / 60.0);
            }

            final String input = filePath.toString();
            final String output = streamLiveBasePath.resolve("hls").resolve(contentId.toString()).toString();
            CompletableFuture.runAsync(() -> {
                try {
                    ffmpegUtils.convertToHls(input, output);
                } catch (Exception e) {
                    logger.error("StreamLive HLS conversion error: {}", e.getMessage());
                }
            });

            existing.setVideoUrl("/media/video/" + contentId + "/playlist.m3u8");
        } else if (request.getContentId() != null && !request.getContentId().equals(existing.getContentId())) {
            // 2. Farklı bir mevcut sunucu içeriği seçildiyse
            UUID contentId = request.getContentId();
            existing.setContentId(contentId);
            existing.setVideoUrl("/media/video/" + contentId + "/playlist.m3u8");

            if ("Movie".equalsIgnoreCase(request.getContentType())) {
                Movie movie = movieRepository.findMovieById(contentId);
                if (movie != null) {
                    existing.setContentName(movie.getName());
                    existing.setSlug(movie.getSlug());
                    if (existing.getPosterUrl() == null || existing.getPosterUrl().isBlank()) {
                        existing.setPosterUrl("/media/image/" + movie.getId());
                    }
                    if (request.getDurationMinutes() <= 0 && movie.getDurationMinutes() > 0) {
                        duration = movie.getDurationMinutes();
                    }
                }
            } else if ("Episode".equalsIgnoreCase(request.getContentType())) {
                Episode ep = seriesRepository.findEpisodeById(contentId);
                if (ep != null) {
                    Series series = seriesRepository.findSeriesById(ep.getSeriesId());
                    String epName = (series != null ? series.getName() + " - " : "") + "S" + ep.getSeasonNumber() + "E" + ep.getEpisodeNumber();
                    if (ep.getName() != null && !ep.getName().isBlank()) {
                        epName += " (" + ep.getName() + ")";
                    }
                    existing.setContentName(epName);
                    existing.setSlug(ep.getSlug());
                    if (existing.getPosterUrl() == null || existing.getPosterUrl().isBlank()) {
                        existing.setPosterUrl("/media/image/" + ep.getSeriesId());
                    }
                    if (request.getDurationMinutes() <= 0 && ep.getDurationMinutes() > 0) {
                        duration = ep.getDurationMinutes();
                    }
                }
            }

            String contentVideoPath = resolveContentVideoPath(contentId, request.getContentType());
            if (contentVideoPath != null) {
                double probedSec = ffmpegUtils.getVideoDurationInSeconds(contentVideoPath);
                if (probedSec > 0) {
                    durationSeconds = (int) Math.round(probedSec);
                    duration = (int) Math.ceil(probedSec / 60.0);
                }
            }
        } else {
            // İçerik ID değişmediğinde ama mevcut videodan kesin saniye kontrolü gerekiyorsa
            String contentVideoPath = resolveContentVideoPath(existing.getContentId(), existing.getContentType());
            if (contentVideoPath != null) {
                double probedSec = ffmpegUtils.getVideoDurationInSeconds(contentVideoPath);
                if (probedSec > 0) {
                    durationSeconds = (int) Math.round(probedSec);
                    duration = (int) Math.ceil(probedSec / 60.0);
                }
            }
        }

        // 3. Kapak resmi güncellendiyse
        if (image != null && !image.isEmpty()) {
            Path imagePath = streamLiveBasePath.resolve(existing.getContentId() + ".jpg");
            Files.copy(image.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
            existing.setPosterUrl("/media/image/" + existing.getContentId());
        } else if (imageUrl != null && !imageUrl.isBlank()) {
            byte[] imgBytes = tvMazeService.downloadImage(imageUrl.trim());
            if (imgBytes != null && imgBytes.length > 0) {
                Path imagePath = streamLiveBasePath.resolve(existing.getContentId() + ".jpg");
                Files.write(imagePath, imgBytes);
                existing.setPosterUrl("/media/image/" + existing.getContentId());
            } else {
                existing.setPosterUrl(imageUrl.trim());
            }
        }

        // 4. Süre ve bitiş zamanı belirleme
        if (durationSeconds <= 0) {
            if (duration > 0) {
                durationSeconds = duration * 60;
            } else if (existing.getDurationSeconds() > 0) {
                durationSeconds = existing.getDurationSeconds();
                duration = existing.getDurationMinutes();
            } else {
                duration = existing.getDurationMinutes() > 0 ? existing.getDurationMinutes() : 90;
                durationSeconds = duration * 60;
            }
        } else if (duration <= 0) {
            duration = (int) Math.ceil(durationSeconds / 60.0);
        }

        // 5. Başlangıç ve bitiş zamanı
        long startMillis = request.getScheduledStartTime();
        if (startMillis <= 0) {
            startMillis = existing.getScheduledStartTime().getTime();
        }
        long endMillis = startMillis + ((long) durationSeconds * 1000L);

        existing.setScheduledStartTime(new Timestamp(startMillis));
        existing.setDurationMinutes(duration);
        existing.setDurationSeconds(durationSeconds);
        existing.setEndTime(new Timestamp(endMillis));

        streamLiveRepository.updateStreamLive(existing);
        return existing;
    }

    public void deleteStream(UUID id) {
        streamLiveRepository.deleteStreamLive(id);
    }

    public List<StreamLive> getAllStreamsForAdmin() {
        return streamLiveRepository.findAllStreamsForAdmin();
    }

    /**
     * Otomatik Temizleme Görevi:
     * Yayının bitişinden (endTime) itibaren 1 saat geçen oturumların sohbet mesajlarını veritabanından kalıcı olarak siler.
     * Her 15 dakikada bir periyodik olarak çalışır.
     */
    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void cleanupExpiredChatMessages() {
        try {
            Timestamp cutoffTime = new Timestamp(System.currentTimeMillis() - ONE_HOUR_MILLIS);
            int deletedCount = streamLiveRepository.deleteChatMessagesForEndedStreams(cutoffTime);
            if (deletedCount > 0) {
                logger.info("StreamLive: Süresi dolan {} adet canlı sohbet mesajı başarıyla temizlendi.", deletedCount);
            }
        } catch (Exception e) {
            logger.error("StreamLive: Canlı sohbet temizleme görevi sırasında hata: {}", e.getMessage(), e);
        }
    }
}
