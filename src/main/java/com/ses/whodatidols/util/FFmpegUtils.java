package com.ses.whodatidols.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class FFmpegUtils {

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${ffprobe.path:ffprobe}")
    private String ffprobePath;

    public int[] getVideoWidthHeight(String videoPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffprobePath,
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

    public int getVideoDurationInMinutes(String videoPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffprobePath,
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    videoPath);

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    double seconds = Double.parseDouble(line);
                    return (int) Math.ceil(seconds / 60.0);
                }
            }
            process.waitFor();

        } catch (Exception e) {
            System.err.println("Süre hesaplanamadı: " + e.getMessage());
        }
        return 0;
    }

    public void transcodeVideo(String inputPath, String outputPath, int width, int height)
            throws IOException, InterruptedException {
        Files.deleteIfExists(Paths.get(outputPath));

        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
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
        java.nio.file.Files.createDirectories(java.nio.file.Paths.get(outputFolder));

        String playlistFile = java.nio.file.Paths.get(outputFolder, "playlist.m3u8").toString();

        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-i", inputPath,
                "-codec:", "copy",
                "-start_number", "0",
                "-hls_time", "10",
                "-hls_list_size", "0",
                "-f", "hls",
                playlistFile);

        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("HLS conversion failed with exit code: " + exitCode);
        }
    }

    public void convertImageToWebP(String inputPath, String outputPath)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
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
