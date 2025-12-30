package com.ses.whodatidols.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FFmpegUtils {

    public static int[] getVideoWidthHeight(String videoPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=width,height",
                    "-of", "csv=s=x:p=0",
                    videoPath
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && line.contains("x")) {
                    String[] parts = line.split("x");
                    int width = Integer.parseInt(parts[0]);
                    int height = Integer.parseInt(parts[1]);
                    return new int[]{width, height};
                }
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- YENİ EKLENEN: SÜRE HESAPLAMA (JARVIS PRECISION) ---
    public static int getVideoDurationInMinutes(String videoPath) {
        try {
            // Komut: ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 dosya.mp4
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    videoPath
            );

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    // Saniye cinsinden gelir (örn: 125.456)
                    double seconds = Double.parseDouble(line);
                    // Dakikaya çevir ve yukarı yuvarla (örn: 2.1 dk -> 3 dk)
                    return (int) Math.ceil(seconds / 60.0);
                }
            }
            process.waitFor();

        } catch (Exception e) {
            System.err.println("Süre hesaplanamadı: " + e.getMessage());
        }
        return 0; // Hata durumunda 0 döner
    }

    public static void transcodeVideo(String inputPath, String outputPath, int width, int height)
            throws IOException, InterruptedException {
        Files.deleteIfExists(Paths.get(outputPath));

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", inputPath,
                "-vf", "scale=" + width + ":" + height,
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-c:a", "copy",
                "-y",
                outputPath
        );
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
}