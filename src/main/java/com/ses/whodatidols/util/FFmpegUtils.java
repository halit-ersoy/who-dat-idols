package com.ses.whodatidols.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FFmpegUtils {

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${ffprobe.path:ffprobe}")
    private String ffprobePath;

    private String resolvedFfmpeg;
    private String resolvedFfprobe;

    public synchronized String getFfmpegPath() {
        if (resolvedFfmpeg != null) {
            return resolvedFfmpeg;
        }
        if (testExecutable(ffmpegPath)) {
            resolvedFfmpeg = ffmpegPath;
            return resolvedFfmpeg;
        }
        // Common Windows Winget fallback
        String userHome = System.getProperty("user.home", "");
        Path wingetFfmpeg = Paths.get(userHome, "AppData", "Local", "Microsoft", "WinGet", "Packages",
                "Gyan.FFmpeg.Essentials_Microsoft.Winget.Source_8wekyb3d8bbwe", "ffmpeg-9.0.1-essentials_build", "bin", "ffmpeg.exe");
        if (Files.exists(wingetFfmpeg)) {
            resolvedFfmpeg = wingetFfmpeg.toString();
            return resolvedFfmpeg;
        }
        resolvedFfmpeg = ffmpegPath;
        return resolvedFfmpeg;
    }

    public synchronized String getFfprobePath() {
        if (resolvedFfprobe != null) {
            return resolvedFfprobe;
        }
        if (testExecutable(ffprobePath)) {
            resolvedFfprobe = ffprobePath;
            return resolvedFfprobe;
        }
        // Common Windows Winget fallback
        String userHome = System.getProperty("user.home", "");
        Path wingetFfprobe = Paths.get(userHome, "AppData", "Local", "Microsoft", "WinGet", "Packages",
                "Gyan.FFmpeg.Essentials_Microsoft.Winget.Source_8wekyb3d8bbwe", "ffmpeg-9.0.1-essentials_build", "bin", "ffprobe.exe");
        if (Files.exists(wingetFfprobe)) {
            resolvedFfprobe = wingetFfprobe.toString();
            return resolvedFfprobe;
        }
        resolvedFfprobe = ffprobePath;
        return resolvedFfprobe;
    }

    private boolean testExecutable(String path) {
        if (path == null || path.isBlank()) return false;
        try {
            Process process = new ProcessBuilder(path, "-version").redirectErrorStream(true).start();
            process.getInputStream().close();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public String getVideoCodec(String videoPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    getFfprobePath(),
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=codec_name",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    videoPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.isBlank()) {
                    return line.trim().toLowerCase();
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Codec detection failed: " + e.getMessage());
        }
        return null;
    }

    public int[] getVideoWidthHeight(String videoPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    getFfprobePath(),
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=width,height",
                    "-of", "csv=s=x:p=0",
                    videoPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && line.contains("x")) {
                    String[] parts = line.split("x");
                    int width = Integer.parseInt(parts[0]);
                    int height = Integer.parseInt(parts[1]);
                    return new int[] { width, height };
                }
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public double getVideoDurationInSeconds(String videoPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    getFfprobePath(),
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    videoPath);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.isBlank()) {
                    return Double.parseDouble(line.trim());
                }
            }
            process.waitFor();

        } catch (Exception e) {
            System.err.println("Süre hesaplanamadı (saniye): " + e.getMessage());
        }
        return 0.0;
    }

    public int getVideoDurationInMinutes(String videoPath) {
        double seconds = getVideoDurationInSeconds(videoPath);
        if (seconds > 0) {
            return (int) Math.ceil(seconds / 60.0);
        }
        return 0;
    }

    public void transcodeVideo(String inputPath, String outputPath, int width, int height)
            throws IOException, InterruptedException {
        Files.deleteIfExists(Paths.get(outputPath));

        ProcessBuilder pb = new ProcessBuilder(
                getFfmpegPath(),
                "-i", inputPath,
                "-vf", "scale=" + width + ":" + height,
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-movflags", "+faststart",
                "-c:a", "copy",
                "-y",
                outputPath);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("FFmpeg: " + line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg transcode failed with exit code: " + exitCode);
        }
    }

    public void convertToHls(String inputPath, String outputFolder) throws IOException, InterruptedException {
        convertToHls(inputPath, outputFolder, 10);
    }

    public void convertToHls(String inputPath, String outputFolder, int segmentTimeSeconds) throws IOException, InterruptedException {
        Files.createDirectories(Paths.get(outputFolder));

        String playlistFile = Paths.get(outputFolder, "playlist.m3u8").toString();
        String segmentPattern = Paths.get(outputFolder, "playlist%d.ts").toString();

        String videoCodec = getVideoCodec(inputPath);
        boolean isH264 = "h264".equalsIgnoreCase(videoCodec);

        ProcessBuilder pb;
        if (isH264) {
            pb = new ProcessBuilder(
                    getFfmpegPath(),
                    "-i", inputPath,
                    "-c", "copy",
                    "-start_number", "0",
                    "-hls_time", String.valueOf(segmentTimeSeconds),
                    "-hls_list_size", "0",
                    "-hls_segment_filename", segmentPattern,
                    "-f", "hls",
                    "-y",
                    playlistFile);
        } else {
            pb = new ProcessBuilder(
                    getFfmpegPath(),
                    "-i", inputPath,
                    "-c:v", "libx264",
                    "-preset", "veryfast",
                    "-crf", "22",
                    "-c:a", "aac",
                    "-start_number", "0",
                    "-hls_time", String.valueOf(segmentTimeSeconds),
                    "-hls_list_size", "0",
                    "-hls_segment_filename", segmentPattern,
                    "-f", "hls",
                    "-y",
                    playlistFile);
        }

        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            if (isH264) {
                System.err.println("Fast copy HLS failed (exit " + exitCode + "), falling back to libx264 transcode...");
                ProcessBuilder fallbackPb = new ProcessBuilder(
                        getFfmpegPath(),
                        "-i", inputPath,
                        "-c:v", "libx264",
                        "-preset", "veryfast",
                        "-crf", "22",
                        "-c:a", "aac",
                        "-start_number", "0",
                        "-hls_time", String.valueOf(segmentTimeSeconds),
                        "-hls_list_size", "0",
                        "-hls_segment_filename", segmentPattern,
                        "-f", "hls",
                        "-y",
                        playlistFile);
                fallbackPb.redirectErrorStream(true);
                Process fallbackProcess = fallbackPb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(fallbackProcess.getInputStream()))) {
                    while (reader.readLine() != null) {
                    }
                }
                int fallbackExit = fallbackProcess.waitFor();
                if (fallbackExit != 0) {
                    throw new RuntimeException("HLS conversion failed with exit code: " + fallbackExit);
                }
            } else {
                throw new RuntimeException("HLS conversion failed with exit code: " + exitCode);
            }
        }
    }

    public void convertImageToWebP(String inputPath, String outputPath)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                getFfmpegPath(),
                "-i", inputPath,
                "-q:v", "75",
                "-y",
                outputPath);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Image conversion to WebP failed with exit code: " + exitCode);
        }
    }
}
