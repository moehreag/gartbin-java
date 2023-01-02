package io.github.moehreag.gartbin_java;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GartbinTest {

    public static final File TEST_FILE = new File("testFile.png");
    public static final File DOWNLOADED = new File("testFile_downloaded");

    public static final String PASSPHRASE = "123supersecretpassphrase123";

    @Test
    void testStream(){
        try {
            Gartbin gartbin = Gartbin.create().setDebug(true).setExpiration(1).setUserAgent("gartbin-java-test/1.0.0").setPassword(PASSPHRASE);

            String fileUrl = gartbin.uploadFile(TEST_FILE);
            System.out.println(fileUrl);
            assertFalse(fileUrl.isEmpty());

            GartbinFile file = gartbin.downloadFile(fileUrl);
            if(DOWNLOADED.exists()){
                assertTrue(DOWNLOADED.delete());
            }
            file.save(new File("").toPath(), DOWNLOADED.getName());
            assertTrue(DOWNLOADED.exists());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testPaste(){
        try {
            String content = Files.readString(Path.of("README.md"));
            assertFalse(content.isEmpty());
            Gartbin gartbin = Gartbin.create().setDebug(true).setExpiration(1).setPassword(PASSPHRASE).setUserAgent("gartbin-java-test/1.0.0");
            String fileUrl = gartbin.createPaste(content);
            System.out.println(fileUrl);
            assertFalse(fileUrl.isEmpty());

            String contentDownload = gartbin.downloadPaste(fileUrl);
            assertEquals(content, contentDownload);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
