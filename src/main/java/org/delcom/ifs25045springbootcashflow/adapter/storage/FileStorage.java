package org.delcom.ifs25045springbootcashflow.adapter.storage;

import org.delcom.ifs25045springbootcashflow.domain.exception.RepositoryException;
import org.delcom.ifs25045springbootcashflow.domain.exception.ValidationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Menyimpan gambar (bukti transaksi + foto profil) ke folder upload dan menghasilkan path publik
 * "/uploads/<nama>". Nama file selalu dibuat server (UUID) sehingga nama dari klien tidak pernah
 * dipakai sebagai path, dan isi file diperiksa terhadap signature gambar.
 */
public class FileStorage {
    public static final String URL_PREFIX = "/uploads/";
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final Path root;

    public FileStorage(Path uploadDir) {
        this.root = uploadDir.toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RepositoryException("Folder upload tidak dapat dibuat: " + root, e);
        }
    }

    public Path getRoot() {
        return root;
    }

    /** @param prefix label jenis file, mis. "proof" atau "avatar" (huruf/angka saja). */
    public String store(String prefix, String originalName, byte[] data) {
        if (data == null || data.length == 0) {
            throw new ValidationException("File kosong");
        }
        if (data.length > MAX_BYTES) {
            throw new ValidationException("Ukuran file maksimal 5 MB");
        }
        String extension = extensionOf(originalName);
        if (!EXTENSIONS.contains(extension)) {
            throw new ValidationException("Format file harus JPG, PNG, GIF, atau WEBP");
        }
        if (!looksLikeImage(data, extension)) {
            throw new ValidationException("Isi file bukan gambar yang valid");
        }
        String safePrefix = (prefix == null ? "" : prefix.replaceAll("[^A-Za-z0-9]", ""));
        String fileName = (safePrefix.isEmpty() ? "" : safePrefix + "-") + UUID.randomUUID() + "." + extension;
        try {
            Files.write(root.resolve(fileName), data, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RepositoryException("Gagal menyimpan file", e);
        }
        return URL_PREFIX + fileName;
    }

    /** Menghapus file berdasarkan path publik; path di luar folder upload diabaikan. */
    public boolean delete(String publicPath) {
        Path target = resolve(publicPath);
        if (target == null) {
            return false;
        }
        try {
            return Files.deleteIfExists(target);
        } catch (IOException e) {
            return false;
        }
    }

    public boolean exists(String publicPath) {
        Path target = resolve(publicPath);
        return target != null && Files.isRegularFile(target);
    }

    private Path resolve(String publicPath) {
        if (publicPath == null || !publicPath.startsWith(URL_PREFIX)) {
            return null;
        }
        String name = publicPath.substring(URL_PREFIX.length());
        Path resolved = root.resolve(name).normalize();
        if (!resolved.startsWith(root) || name.isEmpty() || name.equals(".") || name.contains("/")
                || name.contains("\\") || name.contains("..")) {
            return null;
        }
        return resolved;
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean looksLikeImage(byte[] d, String extension) {
        return switch (extension) {
            case "png" -> d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G';
            case "jpg", "jpeg" -> d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8
                    && (d[2] & 0xFF) == 0xFF;
            case "gif" -> d.length >= 6 && d[0] == 'G' && d[1] == 'I' && d[2] == 'F' && d[3] == '8';
            default -> d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                    && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P';
        };
    }
}
