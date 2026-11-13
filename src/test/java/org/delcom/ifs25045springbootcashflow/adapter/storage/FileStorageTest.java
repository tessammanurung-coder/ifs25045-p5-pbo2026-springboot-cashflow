package org.delcom.ifs25045springbootcashflow.adapter.storage;

import static org.junit.jupiter.api.Assertions.*;

import org.delcom.ifs25045springbootcashflow.domain.exception.RepositoryException;
import org.delcom.ifs25045springbootcashflow.domain.exception.ValidationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileStorageTest {
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    private static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a', 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 1, 0, 0, 0, 'W', 'E', 'B', 'P'};

    @TempDir
    Path dir;
    private FileStorage storage;

    @BeforeEach
    void setUp() {
        storage = new FileStorage(dir.resolve("nested/uploads"));
    }

    @Test
    void createsRootFolder() {
        assertTrue(Files.isDirectory(storage.getRoot()));
    }

    @Test
    void storesAllSupportedImageTypes() {
        for (Object[] c : new Object[][] {{"a.PNG", PNG}, {"a.jpg", JPG}, {"a.jpeg", JPG}, {"a.gif", GIF}, {"a.webp", WEBP}}) {
            String path = storage.store("proof", (String) c[0], (byte[]) c[1]);
            assertTrue(path.startsWith("/uploads/proof-"), path);
            assertTrue(storage.exists(path));
        }
    }

    @Test
    void clientFileNameNeverBecomesPath() throws IOException {
        String path = storage.store("../../evil", "../../x.png", PNG);
        assertTrue(path.matches("/uploads/evil-[0-9a-f-]{36}\\.png"), path);
        String bare = storage.store(null, "a.png", PNG);
        assertTrue(bare.matches("/uploads/[0-9a-f-]{36}\\.png"), bare);
        String blank = storage.store("!!!", "a.png", PNG);
        assertTrue(blank.matches("/uploads/[0-9a-f-]{36}\\.png"), blank);
        try (var files = Files.list(storage.getRoot())) {
            assertEquals(3, files.count());
        }
    }

    @Test
    void rejectsInvalidUploads() {
        assertThrows(ValidationException.class, () -> storage.store("p", "a.png", null));
        assertThrows(ValidationException.class, () -> storage.store("p", "a.png", new byte[0]));
        assertThrows(ValidationException.class, () -> storage.store("p", "a.png", new byte[FileStorage.MAX_BYTES + 1]));
        assertThrows(ValidationException.class, () -> storage.store("p", "a.exe", PNG));
        assertThrows(ValidationException.class, () -> storage.store("p", "noext", PNG));
        assertThrows(ValidationException.class, () -> storage.store("p", null, PNG));
        assertThrows(ValidationException.class, () -> storage.store("p", "a.png", JPG));
        assertThrows(ValidationException.class, () -> storage.store("p", "a.jpg", PNG));
        assertThrows(ValidationException.class, () -> storage.store("p", "a.gif", PNG));
        assertThrows(ValidationException.class, () -> storage.store("p", "a.webp", PNG));
        assertThrows(ValidationException.class, () -> storage.store("p", "a.png", new byte[] {1, 2, 3}));
        assertThrows(ValidationException.class, () -> storage.store("p", "a.webp", new byte[] {'R', 'I'}));
    }

    @Test
    void deleteOnlyTouchesFilesInsideRoot() throws IOException {
        String path = storage.store("proof", "a.png", PNG);
        assertTrue(storage.delete(path));
        assertFalse(storage.delete(path));
        assertFalse(storage.exists(path));
        Path outside = Files.writeString(dir.resolve("secret.txt"), "x");
        assertFalse(storage.delete("/uploads/../../secret.txt"));
        assertFalse(storage.delete("/uploads/a/b.png"));
        assertFalse(storage.delete("/uploads/a\\b.png"));
        assertFalse(storage.delete("/uploads/"));
        assertFalse(storage.delete("/etc/passwd"));
        assertFalse(storage.delete(null));
        assertFalse(storage.exists(null));
        assertTrue(Files.exists(outside));
    }

    @Test
    void deleteReturnsFalseWhenTargetCannotBeRemoved() throws IOException {
        Files.createDirectories(storage.getRoot().resolve("folder"));
        Files.writeString(storage.getRoot().resolve("folder/child.txt"), "x");
        assertFalse(storage.delete("/uploads/folder"), "folder tidak kosong tidak bisa dihapus");
        assertFalse(storage.exists("/uploads/folder"), "folder bukan file biasa");
    }

    @Test
    void failsClearlyWhenRootIsAFile() throws IOException {
        Path file = Files.writeString(dir.resolve("afile"), "x");
        assertThrows(RepositoryException.class, () -> new FileStorage(file));
    }

    @Test
    void failsClearlyWhenRootBecomesUnwritable() throws IOException {
        Path root = storage.getRoot();
        Files.delete(root);
        Files.writeString(root, "now a file");
        assertThrows(RepositoryException.class, () -> storage.store("p", "a.png", PNG));
    }
}
