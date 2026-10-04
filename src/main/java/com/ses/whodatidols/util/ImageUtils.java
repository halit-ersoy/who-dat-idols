package com.ses.whodatidols.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImageUtils {

    /**
     * Converts an image input stream to JPG format and saves it to the target path.
     * If the input stream is already JPEG (magic bytes 0xFF 0xD8), writes bytes directly.
     * The input stream is read completely.
     *
     * @param inputStream The image data stream
     * @param targetPath  The full path where the .jpg file should be saved
     * @throws IOException If reading or writing fails
     */
    public static void saveAsJpg(InputStream inputStream, Path targetPath) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        if (bytes == null || bytes.length == 0) {
            throw new IOException("Empty image data received.");
        }

        // Ensure parent directory exists
        if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
            Files.createDirectories(targetPath.getParent());
        }

        // If the downloaded data is already a valid JPEG (magic bytes 0xFF 0xD8),
        // save the raw bytes directly to disk. This is much faster and avoids AWT decoding/encoding errors.
        if (bytes.length > 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            Files.write(targetPath, bytes);
            return;
        }

        // Otherwise (PNG, WebP, GIF), decode with ImageIO and convert to standard RGB JPG
        BufferedImage originalImage = null;
        try {
            originalImage = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            // Fallback: write raw bytes directly so file exists
            Files.write(targetPath, bytes);
            return;
        }

        if (originalImage == null) {
            // Fallback: write bytes anyway
            Files.write(targetPath, bytes);
            return;
        }

        BufferedImage rgbImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = rgbImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, Color.WHITE, null);
        g2d.dispose();

        boolean success = ImageIO.write(rgbImage, "jpg", targetPath.toFile());
        if (!success) {
            Files.write(targetPath, bytes);
        }
    }

    /**
     * Downloads an image from a URL, converts it to JPG (or saves directly if JPEG), and saves it.
     */
    public static void saveImageFromUrlAsJpg(String imageUrl, Path targetPath) throws IOException {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IOException("Image URL is empty");
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(imageUrl.trim()).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "image/jpeg,image/png,image/webp,image/*;q=0.8");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(true);

            int responseCode = conn.getResponseCode();
            if (responseCode >= 300 && responseCode < 400) {
                String redirectUrl = conn.getHeaderField("Location");
                if (redirectUrl != null) {
                    saveImageFromUrlAsJpg(redirectUrl, targetPath);
                    return;
                }
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error " + responseCode + " while downloading image from " + imageUrl);
            }

            try (InputStream in = conn.getInputStream()) {
                saveAsJpg(in, targetPath);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
