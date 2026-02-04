package com.ses.whodatidols.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImageUtils {

    /**
     * Converts an image input stream to JPG format and saves it to the target path.
     * The input stream is closed after reading.
     *
     * @param inputStream The image data stream
     * @param targetPath  The full path where the .jpg file should be saved
     * @throws IOException If reading or writing fails
     */
    public static void saveAsJpg(InputStream inputStream, Path targetPath) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputStream);
        if (originalImage == null) {
            throw new IOException("Failed to read image data. The file might be corrupted or format unsupported.");
        }

        // Ensure parent directory exists
        if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
            Files.createDirectories(targetPath.getParent());
        }

        // Convert to RGB (standard for JPG)
        BufferedImage rgbImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = rgbImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, Color.WHITE, null); // Draw on white background for transparency
        g2d.dispose();

        // Write as JPG
        boolean success = ImageIO.write(rgbImage, "jpg", targetPath.toFile());
        if (!success) {
            throw new IOException("JPG writer not found. This should not happen in standard Java.");
        }
    }

    /**
     * Downloads an image from a URL, converts it to JPG, and saves it.
     */
    public static void saveImageFromUrlAsJpg(String imageUrl, Path targetPath) throws IOException {
        try (InputStream in = URI.create(imageUrl).toURL().openStream()) {
            saveAsJpg(in, targetPath);
        }
    }
}
