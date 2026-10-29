package com.ses.whodatidols.util;

import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * A MultipartFile implementation backed by a temporary File on disk.
 * Used to transparently bridge chunked uploaded files to existing service methods.
 */
public class FileSystemMultipartFile implements MultipartFile {

    private final File file;
    private final String originalFilename;

    public FileSystemMultipartFile(File file) {
        this.file = file;
        this.originalFilename = file != null ? file.getName() : "";
    }

    public FileSystemMultipartFile(File file, String originalFilename) {
        this.file = file;
        this.originalFilename = originalFilename != null ? originalFilename : (file != null ? file.getName() : "");
    }

    @Override
    @NonNull
    public String getName() {
        return "file";
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        try {
            if (file != null && file.exists()) {
                String ct = Files.probeContentType(file.toPath());
                if (ct != null) return ct;
            }
        } catch (Exception ignored) {}
        return "application/octet-stream";
    }

    @Override
    public boolean isEmpty() {
        return file == null || !file.exists() || file.length() == 0;
    }

    @Override
    public long getSize() {
        return file != null && file.exists() ? file.length() : 0;
    }

    @Override
    @NonNull
    public byte[] getBytes() throws IOException {
        if (file == null || !file.exists()) return new byte[0];
        return Files.readAllBytes(file.toPath());
    }

    @Override
    @NonNull
    public InputStream getInputStream() throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("File does not exist: " + file);
        }
        return new FileInputStream(file);
    }

    @Override
    public void transferTo(@NonNull File dest) throws IOException, IllegalStateException {
        if (file == null || !file.exists()) {
            throw new IOException("File does not exist: " + file);
        }
        Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public File getFile() {
        return file;
    }
}
