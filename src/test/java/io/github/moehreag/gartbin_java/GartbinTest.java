package io.github.moehreag.gartbin_java;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class GartbinTest {

    private static final Gartbin gartbin = Gartbin.create();
    public static final File TEST_FILE = new File("testFile.png");
    public static final File DOWNLOADED = new File("testFile_downloaded");

    public static final String PASSPHRASE = "123supersecretpassphrase123";

    @Test
    void test(){
        try {
            gartbin.setDebug(true).setExpiration(1).setUserAgent("gartbin-java-test/1.0.0").setPassword(PASSPHRASE);

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
}
