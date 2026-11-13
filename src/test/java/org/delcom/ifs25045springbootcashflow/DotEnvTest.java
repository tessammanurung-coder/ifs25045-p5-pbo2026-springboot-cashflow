package org.delcom.ifs25045springbootcashflow;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DotEnvTest {
    @TempDir
    Path dir;

    @Test
    void parsesCommentsQuotesAndInvalidLines() {
        Map<String, String> v = DotEnv.parse(List.of(
                "# komentar", "", "A=1", " B = dua ", "C=\"kutip ganda\"", "D='kutip tunggal'",
                "E=", "=tanpa-kunci", "tanpa-sama", "F=a=b", "G=\""));
        assertEquals("1", v.get("A"));
        assertEquals("dua", v.get("B"));
        assertEquals("kutip ganda", v.get("C"));
        assertEquals("kutip tunggal", v.get("D"));
        assertEquals("", v.get("E"));
        assertEquals("a=b", v.get("F"));
        assertEquals("\"", v.get("G"));
        assertEquals(7, v.size());
    }

    @Test
    void loadsIntoSystemPropertiesWithoutOverridingExisting() throws IOException {
        Path file = Files.writeString(dir.resolve(".env"),
                "DOTENV_TEST_NEW=nilai-baru\nDOTENV_TEST_KEEP=dari-file\nPATH=dari-file\n");
        System.setProperty("DOTENV_TEST_KEEP", "dari-jvm");
        try {
            DotEnv.load(file);
            assertEquals("nilai-baru", System.getProperty("DOTENV_TEST_NEW"));
            assertEquals("dari-jvm", System.getProperty("DOTENV_TEST_KEEP"));
            assertNull(System.getProperty("PATH"), "variabel environment OS tidak ditimpa");
        } finally {
            System.clearProperty("DOTENV_TEST_NEW");
            System.clearProperty("DOTENV_TEST_KEEP");
        }
    }

    @Test
    void missingFileIsIgnored() {
        assertDoesNotThrow(() -> DotEnv.load(dir.resolve("tidak-ada.env")));
        assertDoesNotThrow(() -> DotEnv.load());
    }

    @Test
    void unreadableFileIsReported() throws IOException {
        Path file = dir.resolve("binary.env");
        Files.write(file, new byte[] {(byte) 0xC3, (byte) 0x28, '=', 1});
        assertThrows(IllegalStateException.class, () -> DotEnv.load(file));
    }
}
