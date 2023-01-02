package io.github.moehreag.gartbin_java;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public record GartbinFile(InputStream data, String originalName) {

    public void save(Path path) throws IOException {
        save(path, originalName);
    }

    public void save(Path path, String filename) throws IOException {
        Files.write(path.resolve(filename), data.readAllBytes());
    }
}
